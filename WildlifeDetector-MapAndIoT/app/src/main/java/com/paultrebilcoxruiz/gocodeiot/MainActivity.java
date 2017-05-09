package com.paultrebilcoxruiz.gocodeiot;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
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
import com.paultrebilcoxruiz.gocodeiot.database.Weather;
import com.paultrebilcoxruiz.gocodeiot.database.Detection;
import com.paultrebilcoxruiz.gocodeiot.hardware.adc.MCP3008;
import com.paultrebilcoxruiz.gocodeiot.hardware.camera.CameraHandler;
import com.paultrebilcoxruiz.gocodeiot.hardware.camera.ImagePreprocessor;
import com.paultrebilcoxruiz.gocodeiot.hardware.sensors.Bmx280;
import com.paultrebilcoxruiz.gocodeiot.hardware.sensors.flamedetector.FlameDetector;
import com.paultrebilcoxruiz.gocodeiot.hardware.sensors.motiondetector.HCSR501;
import com.paultrebilcoxruiz.gocodeiot.machinelearning.Classifier;
import com.paultrebilcoxruiz.gocodeiot.machinelearning.TensorFlowImageClassifier;
import com.paultrebilcoxruiz.gocodeiot.utils.BoardDefaults;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements HCSR501.OnMotionDetectedEventListener, LocationListener, ImageReader.OnImageAvailableListener, FlameDetector.OnFlameDetectedListener {

    private final String FIREBASE_DATABASE_URL = "https://go-code-co-wildl-1490055276288.firebaseio.com/";
    private final String FIREBASE_STORAGE_URL = "gs://go-code-co-wildl-1490055276288.appspot.com";

    private final float acceptableRecognitionConfidence = 0.85f;

    public static final int mGpsBuadRate = 9600;
    public static final float mGpsAccuracy = 2.5f;

    private HCSR501 mMotionDetector;
    private FlameDetector mFlameDetector;

    private Bmx280 mTemperaturePressureSensor;
    private MCP3008 mMCP3008;
    private NmeaGpsDriver mGpsDriver;

    private Handler mAnalogInputHandler;

    private LocationManager mLocationManager;
    private I2cDevice mHumidity;

    private ImagePreprocessor mImagePreprocessor;
    private CameraHandler mCameraHandler;
    private TensorFlowImageClassifier mTensorFlowClassifier;

    private HandlerThread mCameraBackgroundThread;
    private Handler mCameraBackgroundHandler;

    private Weather mWeatherData;

    private Runnable mWeatherSensorRunnable = new Runnable() {

        private static final long DELAY_MS = 1000L * 60; //1 minute

        @Override
        public void run() {
            Log.e("Test", "check weather");
            try {
                mWeatherData = new Weather();
                mWeatherData.setTime(System.currentTimeMillis());

                readHumidity();
                readPressure();
                readTemperature();
                readAirQuality();
                readUv();
                populateDewPoint();

                uploadWeatherData();

            } catch( IOException e ) {
                Log.e("Test", "handler io exception: " + e.getMessage());
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
            mFlameDetector = new FlameDetector(BoardDefaults.getFlameDetectorPin());
            mFlameDetector.setOnFlameDetectedListener(this);
        } catch( IOException e ) {
            Log.e("Test", "flame detector exception: " + e.getMessage() );
        }



        initCamera();
        mAnalogInputHandler = new Handler();
        mAnalogInputHandler.post(mWeatherSensorRunnable);

        try {
            mMotionDetector = new HCSR501(BoardDefaults.getMotionDetectorPin());
            mMotionDetector.setOnMotionDetectedEventListener(this);
            Log.e("Test", "added motion detector");
        } catch( IOException e ) {
            Log.e("Test", "motion detector exception: " + e.getMessage());
        }
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
        mMCP3008 = new MCP3008(BoardDefaults.getAdcCsPin(), BoardDefaults.getAdcClockPin(), BoardDefaults.getAdcMosiPin(), BoardDefaults.getAdcMisoPin());
        mMCP3008.register();
    }

    private void initGps() throws IOException {
        mGpsDriver = new NmeaGpsDriver(this, BoardDefaults.getGpsUartBus(),
                mGpsBuadRate, mGpsAccuracy);
        mGpsDriver.register();
        // Register for location updates
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }


    //int
    private void readHumidity() throws IOException {
        mWeatherData.setRelhumidity(mHumidity.readRegByte(0));

    }

    //int
    private void readAirQuality() throws IOException {
        mWeatherData.setAirquality(mMCP3008.readAdc( BoardDefaults.getAirQualityChannel() ));
    }

    //int
    private void readUv() throws IOException {
        mWeatherData.setUv(mMCP3008.readAdc( BoardDefaults.getUvChannel() ));
    }

    //float
    private void readPressure() throws IOException {
        mWeatherData.setPressure(mTemperaturePressureSensor.readPressure() * 100 ); //Pa
    }

    //float
    private void readTemperature() throws IOException {
        mWeatherData.setTemperature(mTemperaturePressureSensor.readTemperature()); //C
    }

    //http://en.wikipedia.org/wiki/Dew_point
    private void populateDewPoint() {
        double a = 17.271;
        double b = 237.7;
        double temp = (a * mWeatherData.getTemperature()) / (b + mWeatherData.getTemperature()) + Math.log(mWeatherData.getTemperature()*0.01);
        double Td = (b * temp) / (a - temp);

        mWeatherData.setDewpoint(Td);
    }

    private void takePicture() {
        Log.e("Test", "take picture");
        mCameraBackgroundHandler.post(mTakePictureBackgroundRunnable);
    }

    private void uploadWeatherData() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl(FIREBASE_DATABASE_URL);

        databaseRef.child("weather").child(String.valueOf(System.currentTimeMillis())).setValue(mWeatherData);
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
        if( state == HCSR501.State.STATE_HIGH ) {
            Log.e("Test", "onmotiondetected");
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

        Detection detectedAnimal = getDetectedAnimal(results);
        if( detectedAnimal == null ) {
            Log.e("Test", "no detected animal");
            return;
        }

        uploadAnimal( bitmap, detectedAnimal );
    }

    private void uploadAnimal(Bitmap bitmap, final Detection detectedAnimal) {
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

    private void handleNotificationForImage(Uri downloadUri, Detection detection) {

        detection.setImageUrl(downloadUri.toString());
        if( mLocationManager != null && mLocationManager.getAllProviders() != null && !mLocationManager.getAllProviders().isEmpty() ) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.NO_REQUIREMENT);
            Location location = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(criteria, true));
            if( location != null ) {
                detection.setLatitude(location.getLatitude());
                detection.setLongitude(location.getLongitude());
            } else {
                detection.setLatitude(0);
                detection.setLongitude(0);
            }
        }
        detection.setTimeMillis(System.currentTimeMillis());

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl(FIREBASE_DATABASE_URL);

        databaseRef.child("detection").setValue(detection);
    }

    public Detection getDetectedAnimal(List<Classifier.Recognition> results) {
        for( Classifier.Recognition tmp : results ) {
            if( tmp.getConfidence() >= acceptableRecognitionConfidence ) {
                Detection detection = new Detection();
                detection.setAnimalType(tmp.getTitle());
                detection.setConfidence(tmp.getConfidence());
                return detection;
            }
        }

        return null;
    }

    @Override
    public void onFlameDetected(FlameDetector.State flame) {
        if( flame == FlameDetector.State.FLAME ) {
            Log.e("Test", "on flame detected");
        }
    }
}
