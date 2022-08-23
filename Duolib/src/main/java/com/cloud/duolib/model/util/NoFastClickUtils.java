package com.cloud.duolib.model.util;

/**
 * Created  on 18-6-24.
 */

public class NoFastClickUtils {
    private static long lastClickTime = 0;//上次点击的时间

    public static boolean isFastClick(int spaceTime) {
        long currentTime = System.currentTimeMillis();//当前系统时间

        boolean isAllowClick;//是否允许点击

        if (currentTime - lastClickTime > spaceTime) {
            lastClickTime = currentTime;
            isAllowClick = false;

        } else {

            isAllowClick = true;

        }

        return isAllowClick;

    }
}
