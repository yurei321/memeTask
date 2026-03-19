package com.example.memetask.db;

import androidx.room.TypeConverter;

import java.util.Date;

public class DateConverters {

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date toDate(Long value) {
        return value == null ? null : new Date(value);
    }
}
