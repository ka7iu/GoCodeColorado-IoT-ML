package com.paultrebilcoxruiz.gocodeiot.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherSqlDBHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + WeatherContract.WeatherEntry.TABLE_NAME + " (" +
                    WeatherContract.WeatherEntry._ID + " INTEGER PRIMARY KEY," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_TIME + " INTEGER," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_TEMPERATURE + " REAL," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_DEWPOINT + " REAL," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_RELHUMIDITY + " INTEGER," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_PRESSURE + " REAL," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_UV + " INTEGER," +
                    WeatherContract.WeatherEntry.COLUMN_NAME_AIRQUALITY + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WeatherContract.WeatherEntry.TABLE_NAME;

    public WeatherSqlDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


}
