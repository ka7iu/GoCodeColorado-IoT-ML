package com.paultrebilcoxruiz.gocodeiot.database;

import android.provider.BaseColumns;

public final class WeatherContract {

    private WeatherContract() {}

    /* Inner class that defines the table contents */
    public static class WeatherEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_TEMPERATURE = "temperature";
        public static final String COLUMN_NAME_DEWPOINT = "dewpoint";
        public static final String COLUMN_NAME_RELHUMIDITY = "relhumidity";
        public static final String COLUMN_NAME_PRESSURE = "pressure";
        public static final String COLUMN_NAME_UV = "uv";
        public static final String COLUMN_NAME_AIRQUALITY = "airquality";
    }
}
