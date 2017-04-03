package com.paultrebilcoxruiz.gocodeiot.utils;

public class BoardDefaults {

    public static String getFlameDetectorPin() {
        return "BCM24";
    }

    public static String getMotionDetectorPin() {
        return "BCM26";
    }

    public static String getGpsUartBus() {
        return "UART0";
    }

    public static String getAdcCsPin() {
        return "BCM12";
    }

    public static String getAdcClockPin() {
        return "BCM21";
    }

    public static String getAdcMosiPin() {
        return "BCM16";
    }

    public static String getAdcMisoPin() {
        return "BCM20";
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
