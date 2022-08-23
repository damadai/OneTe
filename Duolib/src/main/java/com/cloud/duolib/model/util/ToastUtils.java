package com.cloud.duolib.model.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public static void showMsgLong(Context cxt,String msg) {
        try {
            Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void showMsgShort(Context cxt,String msg) {
        try {
            Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void showMsgShort(Context cxt,int msg) {
        try {
            Toast.makeText(cxt, cxt.getString(msg), Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void showMsgLong(Context cxt,int msg) {
        try {
            Toast.makeText(cxt, cxt.getString(msg), Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}