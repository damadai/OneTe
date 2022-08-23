package com.cloud.duolib.model.util;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OsTimeUtils {

    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat dfGl = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @SuppressLint({"ConstantLocale"})
    private static final SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * @return 当前时间戳（毫秒）
     */
    public static Long getCurrentMillisecond() {
        return System.currentTimeMillis();
    }

    public static String getCurrentTime2() {
        return df2.format(getCurrentMillisecond());
    }

    public static String getCurrentTime1() {
        return df1.format(getCurrentMillisecond());
    }

    /**
     * @return 当前时间戳（秒）
     */
    public static Long getCurrentSecond() {
        return getCurrentMillisecond() / 1000;
    }

    //获取格林威治时间
    public static String getCurrentGeLin() {
        dfGl.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dfGl.format(Calendar.getInstance().getTime());
    }

    //Mon, 11 Apr 2022 08:51:16 GMT
    private static String getGeLinFormat(String utcTime) {
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        dfGl.setTimeZone(utcZone);
        String dateTime = "";
        try {
            Date date = dfGl.parse(utcTime);
            if (date != null) {
                dateTime = df1.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateTime;
    }

    //分钟差
    public static Long getTimeMix(String from, String to) {
        Date fromDate3;
        Date toDate3;
        try {
            fromDate3 = sdf.parse(from);
            toDate3 = sdf.parse(getGeLinFormat(to));
            long from3 = 0;
            long to3 = 0;
            if ((fromDate3 != null) & (toDate3 != null)) {
                from3 = fromDate3.getTime();
                to3 = toDate3.getTime();
            }
            return (to3 - from3) / (1000 * 60);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final ThreadLocal<SimpleDateFormat> SDF_THREAD_LOCAL = new ThreadLocal<>();

    private static SimpleDateFormat getDefaultFormat() {
        SimpleDateFormat simpleDateFormat = SDF_THREAD_LOCAL.get();
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SDF_THREAD_LOCAL.set(simpleDateFormat);
        }
        return simpleDateFormat;
    }

    public static long string2Millis(final String time) {
        return string2Millis(time, getDefaultFormat());
    }

    public static long string2Millis(final String time, @NonNull final DateFormat format) {
        try {
            return format.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static long getNowMills() {
        return System.currentTimeMillis();
    }
}