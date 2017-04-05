package com.paultrebilcoxruiz.gocodeiot.hardware.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;

public class CameraHandler {
    private static final String TAG = CameraHandler.class.getSimpleName();

    public static final int IMAGE_WIDTH = 3456;
    public static final int IMAGE_HEIGHT = 2304;

    private static final int MAX_IMAGES = 1;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private ImageReader mImageReader;

    private CameraHandler() {
    }

    private static class InstanceHolder {
        private static CameraHandler mCamera = new CameraHandler();
    }

    public static CameraHandler getInstance() {
        return InstanceHolder.mCamera;
    }

    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageAvailableListener) {

        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};

        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {}

        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }

        String id = camIds[0];

        //init image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.YUV_420_888, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(
                imageAvailableListener, backgroundHandler);

        //open camera resource
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler);
        } catch (CameraAccessException cae) {}

    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            closeCaptureSession();
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            closeCaptureSession();
            cameraDevice.close();
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = null;
        }

    };

    public void takePicture() {
        if (mCameraDevice == null) {
            return;
        }

        createCameraCaptureSession();
    }

    private void createCameraCaptureSession() {
        try {
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch( CameraAccessException e ) {}
    }

    private CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }

                    mCaptureSession = cameraCaptureSession;
                    triggerImageCapture();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //no-op
                }
            };

    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);

        } catch (CameraAccessException cae) {}

    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {}

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    session.close();
                    mCaptureSession = null;
                }
            };

    private void closeCaptureSession() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.close();
            } catch (Exception ex) {}
            mCaptureSession = null;
        }
    }

    public void shutDown() {
        closeCaptureSession();
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }
}