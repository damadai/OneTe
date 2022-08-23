package com.cloud.duolib.model.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt

class NavigationBarUtils{
    /**
     * 导航栏，状态栏透明
     * @param activity
     */
    fun setNavigationBar(activity: Activity, light: Boolean, color: Int?) {
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = activity.window.decorView
            val option = if (Build.VERSION.SDK_INT >= 23 && light) {
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            } else {
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
            //activity.window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 23 && color != null) {
                activity.window.statusBarColor = calculateStatusColor(color, 0)
            } else {
                activity.window.statusBarColor = Color.TRANSPARENT
            }
            decorView.systemUiVisibility = option
        }
        activity.actionBar?.hide()
    }

    //状态栏颜色换算
    private fun calculateStatusColor(@ColorInt color: Int, alpha: Int): Int {
        return if (alpha == 0) {
            color
        } else {
            val a = 1.0f - alpha.toFloat() / 255.0f
            var red = color shr 16 and 255
            var green = color shr 8 and 255
            var blue = color and 255
            red = ((red.toFloat() * a).toDouble() + 0.5).toInt()
            green = ((green.toFloat() * a).toDouble() + 0.5).toInt()
            blue = ((blue.toFloat() * a).toDouble() + 0.5).toInt()
            -16777216 or (red shl 16) or (green shl 8) or blue
        }
    }
}