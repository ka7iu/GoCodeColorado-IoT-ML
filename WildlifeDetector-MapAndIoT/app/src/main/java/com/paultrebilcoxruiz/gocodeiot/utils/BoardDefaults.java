package com.paultrebilcoxruiz.gocodeiot.utils;

public class BoardDefaults {

    public static String getFlameDetectorPin() {
        return "BCM24";
    }

    public static String getMotionDetectorPin() {
        return "BCM4";
    }

    public static String getAdcCsPin() {
        return "BCM17";
    }

    public static String getAdcClockPin() {
        return "BCM18";
    }

    public static String getAdcMosiPin() {
        return "BCM27";
    }

    public static String getAdcMisoPin() {
        return "BCM22";
    }

    public static String getBmx280I2cBus() {
        return "I2C1";
    }

    public static String getHumidityI2cBus() {
        return "I2C1";
    }

    public static int getAirQualityChannel() {
        return 0x0;
    }

    public static int getUvChannel() {
        return 0x1;
    }

}
