package com.paultrebilcoxruiz.gocodeiot;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.gps.NmeaGpsDriver;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.paultrebilcoxruiz.gocodeiot.firebase.Detection;
import com.paultrebilcoxruiz.gocodeiot.hardware.adc.MCP3008;
import com.paultrebilcoxruiz.gocodeiot.hardware.camera.CameraHandler;
import com.paultrebilcoxruiz.gocodeiot.hardware.camera.ImagePreprocessor;
import com.paultrebilcoxruiz.gocodeiot.hardware.sensors.Bmx280;
import com.paultrebilcoxruiz.gocodeiot.hardware.sensors.motiondetector.HCSR501;
import com.paultrebilcoxruiz.gocodeiot.machinelearning.Classifier;
import com.paultrebilcoxruiz.gocodeiot.machinelearning.TensorFlowImageClassifier;
import com.paultrebilcoxruiz.gocodeiot.utils.BoardDefaults;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements HCSR501.OnMotionDetectedEventListener, LocationListener, ImageReader.OnImageAvailableListener {

    private final String FIREBASE_DATABASE_URL = "https://go-code-co-wildl-1490055276288.firebaseio.com/";
    private final String FIREBASE_STORAGE_URL = "gs://go-code-co-wildl-1490055276288.appspot.com";

    private final float acceptableRecognitionConfidence = 80.0f;

    public static final int mGpsBuadRate = 9600;
    public static final float mGpsAccuracy = 2.5f;

    private final long DELAY_TIME_MILLIS = 5 * 1000 * 60; //5 minutes

    private HCSR501 mMotionDetector;

    private Bmx280 mTemperaturePressureSensor;
    private MCP3008 mMCP3008;
    private NmeaGpsDriver mGpsDriver;

    private Handler mAnalogInputHandler;

    private LocationManager mLocationManager;
    private I2cDevice mHumidity;

    private long lastDetectionTime = 0;

    private ImagePreprocessor mImagePreprocessor;
    private CameraHandler mCameraHandler;
    private TensorFlowImageClassifier mTensorFlowClassifier;

    private HandlerThread mCameraBackgroundThread;
    private Handler mCameraBackgroundHandler;

    private Runnable mWeatherSensorRunnable = new Runnable() {

        private static final long DELAY_MS = 3000L;//

        @Override
        public void run() {
            Log.e("Test", "check weather");
            try {
                readHumidity();
                readPressure();
                readTemperature();
                readAirQuality();
                readUv();
            } catch( IOException e ) {
                Log.e("Test", "handler io exception: " + e.getMessage() );
            }

            mAnalogInputHandler.postDelayed(this, DELAY_MS);
        }
    };

    private Runnable mTakePictureBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e("Test", "takepicture runnable");
            if( mCameraHandler == null ) {
                Log.e("Test", "camera handler is null?");
                return;
            }

            mCameraHandler.takePicture();
        }
    };

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            Log.e("Test", "initialize camera on background");

            mImagePreprocessor = new ImagePreprocessor(CameraHandler.IMAGE_WIDTH,
                    CameraHandler.IMAGE_HEIGHT, TensorFlowImageClassifier.INPUT_SIZE);


            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    MainActivity.this, mCameraBackgroundHandler,
                    MainActivity.this);

            mTensorFlowClassifier = new TensorFlowImageClassifier(MainActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Test", "oncreate");
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        PeripheralManagerService service = new PeripheralManagerService();
        try {
            mHumidity = service.openI2cDevice(BoardDefaults.getHumidityI2cBus(), 0x08);
            initMCP3008();
            mTemperaturePressureSensor = new Bmx280( BoardDefaults.getBmx280I2cBus() );
            if( hasLocationPermission() ) {
                initGps();
            }
        } catch( IOException e ) {
            Log.e("Test", "Initialization exception occurred: " + e.getMessage());
        }

        try {
            mMotionDetector = new HCSR501(BoardDefaults.getMotionDetectorPin());
            mMotionDetector.setOnMotionDetectedEventListener(this);
        } catch( IOException e ) {

        }

        initCamera();
        mAnalogInputHandler = new Handler();
        mAnalogInputHandler.post(mWeatherSensorRunnable);
    }

    private void initCamera() {
        mCameraBackgroundThread = new HandlerThread("BackgroundThread");
        mCameraBackgroundThread.start();
        mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());
        mCameraBackgroundHandler.post(mInitializeOnBackground);
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void initMCP3008() throws IOException {
        Log.e("Test", "initmcp3008");
        mMCP3008 = new MCP3008(BoardDefaults.getAdcCsPin(), BoardDefaults.getAdcClockPin(), BoardDefaults.getAdcMosiPin(), BoardDefaults.getAdcMisoPin());
        mMCP3008.register();
    }

    private void initGps() throws IOException {
        Log.e("Test", "initgps");
        mGpsDriver = new NmeaGpsDriver(this, BoardDefaults.getGpsUartBus(),
                mGpsBuadRate, mGpsAccuracy);
        mGpsDriver.register();
        // Register for location updates
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }


    //Change to 5v digital
    private void readHumidity() throws IOException {
        Log.e("Test", "humidity: " + mHumidity.readRegByte(0));
    }

    private void readAirQuality() throws IOException {
        Log.e("Test", "air quality: " + mMCP3008.readAdc( BoardDefaults.getAirQualityChannel() ) );
    }

    private void readUv() throws IOException {
        Log.e("Test", "uv: " + mMCP3008.readAdc( BoardDefaults.getUvChannel() ) );
    }

    private void readPressure() throws IOException {
        Log.e("Test", "pressure: " + mTemperaturePressureSensor.readPressure() * 100 ); //Pa
    }

    private void readTemperature() throws IOException {
        Log.e("Test", "temperature: " + mTemperaturePressureSensor.readTemperature() ); //C
    }

    private void takePicture() {
        mCameraBackgroundHandler.post(mTakePictureBackgroundRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mMCP3008 != null ) {
            mMCP3008.unregister();
        }

        if( mAnalogInputHandler != null ) {
            mAnalogInputHandler.removeCallbacks(mWeatherSensorRunnable);
        }

        if( mTemperaturePressureSensor != null ) {
            mTemperaturePressureSensor.close();
        }

        if (mGpsDriver != null) {
            // Unregister components
            mGpsDriver.unregister();
            mLocationManager.removeUpdates(this);

            try {
                mGpsDriver.close();
            } catch (IOException e) {
                //no op
            }
        }

        if( mHumidity != null ) {
            try {
                mHumidity.close();
                mHumidity = null;
            } catch( IOException e ) {

            }
        }

        try {
            if (mCameraBackgroundThread != null) mCameraBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mCameraBackgroundThread = null;
        mCameraBackgroundHandler = null;

        try {
            if (mCameraHandler != null) mCameraHandler.shutDown();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.close();
        } catch (Throwable t) {
            // close quietly
        }
    }

    @Override
    public void onMotionDetectedEvent(HCSR501.State state) {
        if( shouldTakeImage() ) {
            takePicture();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Bitmap bitmap;
        try (Image image = reader.acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }

        final List<Classifier.Recognition> results = mTensorFlowClassifier.recognizeImage(bitmap);

        Log.e("Test", "Got the following results from Tensorflow: " + results);

        String detectedAnimal = getAnimalType(results);
        if( TextUtils.isEmpty(detectedAnimal) ) {
            Log.e("Test", "no detected animal");
            return;
        }

        lastDetectionTime = System.currentTimeMillis();

        uploadAnimal( bitmap, detectedAnimal );
    }

    private void uploadAnimal(Bitmap bitmap, final String detectedAnimal) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] data = outputStream.toByteArray();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(FIREBASE_STORAGE_URL).child(System.currentTimeMillis() + ".jpg");


        UploadTask uploadTask = storageReference.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                handleNotificationForImage(taskSnapshot.getDownloadUrl(), detectedAnimal);
            }
        });
    }

    private void handleNotificationForImage(Uri downloadUri, String detectedAnimal) {
        Detection detection = new Detection();
        detection.setAnimalType(detectedAnimal);
        detection.setImageUrl(downloadUri.toString());
        if( mLocationManager != null && mLocationManager.getAllProviders() != null && !mLocationManager.getAllProviders().isEmpty() ) {
            Location location = mLocationManager.getLastKnownLocation(mLocationManager.getAllProviders().get(0));
            detection.setLatitude(location.getLatitude());
            detection.setLongitude(location.getLongitude());
        }
        detection.setTimeMillis(System.currentTimeMillis());

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl(FIREBASE_DATABASE_URL);

        databaseRef.child("detection").setValue(detection);
    }

    private boolean shouldTakeImage() {
        return System.currentTimeMillis() - lastDetectionTime > DELAY_TIME_MILLIS;
    }

    public String getAnimalType(List<Classifier.Recognition> results) {
        for( Classifier.Recognition tmp : results ) {
            if( tmp.getConfidence() >= acceptableRecognitionConfidence ) {
                return tmp.getTitle();
            }
        }

        return null;
    }
}
