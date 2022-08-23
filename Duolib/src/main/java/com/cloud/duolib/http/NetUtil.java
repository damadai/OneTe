package com.cloud.duolib.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.cloud.duolib.CloudBuilder;

public class NetUtil {
    public static boolean hasInternet() {
        boolean flag;
        flag = ((ConnectivityManager) CloudBuilder.INSTANCE.getApp().getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
        return flag;
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (null != info && info.isConnected() && info.isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
