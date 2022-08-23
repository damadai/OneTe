/*
 * Copyright © 2020 XiaMen BaFenYi Network Technology Co., Ltd. All rights reserved.
 * 版权：厦门八分仪网络科技有限公司版权所有（C）2020
 * 作者：Administrator
 * 创建日期：2020年4月6日
 */

package com.cloud.duolib.model.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cloud.duolib.CloudBuilder;

/**
 * Created by T on 2018/6/28
 * Preferenced工具类
 */

public class PreferenceUtil {
    public static void putLong(String key, long value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void putString(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static long getLong(String key, long defValue) {
        return PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp())
                .getLong(key, defValue);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp())
                .getBoolean(key, defValue);
    }

    public static String getString(String key, String defValue) {
        return PreferenceManager.getDefaultSharedPreferences(CloudBuilder.INSTANCE.getApp())
                .getString(key, defValue);
    }
}
