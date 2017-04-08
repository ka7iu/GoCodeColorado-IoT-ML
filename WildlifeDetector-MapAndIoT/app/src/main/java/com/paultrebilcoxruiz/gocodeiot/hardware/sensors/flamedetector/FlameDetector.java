package com.paultrebilcoxruiz.gocodeiot.hardware.sensors.flamedetector;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class FlameDetector implements AutoCloseable {

    public enum State {
        FLAME,
        NO_FLAME;
    }

    public interface OnFlameDetectedListener {
        void onFlameDetected(State flame);
    }

    private Gpio mFlameDetectorGpio;
    private OnFlameDetectedListener mOnFlameDetectedListener;

    public boolean mLastState;

    private GpioCallback mInterruptCallback = new GpioCallback() {

        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {

                if( gpio.getValue() != mLastState ) {
                    mLastState = gpio.getValue();
                    if( mOnFlameDetectedListener != null ) {
                        mOnFlameDetectedListener.onFlameDetected(mLastState ? State.FLAME : State.NO_FLAME);
                    }
                }


            } catch( IOException e ) {

            }

            return true;
        }
    };

    public FlameDetector(String pin) throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();

        Gpio gpio = service.openGpio(pin);

        try {
            connect(gpio);
        } catch( IOException e ) {
            close();
            throw e;
        }
    }

    private void connect(Gpio gpio) throws IOException {
        mFlameDetectorGpio = gpio;
        mFlameDetectorGpio.setDirection(Gpio.DIRECTION_IN);
        mFlameDetectorGpio.setEdgeTriggerType(Gpio.EDGE_RISING);

        mLastState = false;

        mFlameDetectorGpio.setActiveType(Gpio.ACTIVE_LOW);
        mFlameDetectorGpio.registerGpioCallback(mInterruptCallback);
    }

    public void setOnFlameDetectedListener(OnFlameDetectedListener listener) {
        mOnFlameDetectedListener = listener;
    }

    @Override
    public void close() throws IOException {
        mOnFlameDetectedListener = null;

        if( mFlameDetectorGpio != null ) {
            mFlameDetectorGpio.unregisterGpioCallback(mInterruptCallback);

            try {
                mFlameDetectorGpio.close();
            } finally {
                mFlameDetectorGpio = null;
            }
        }
    }
}
