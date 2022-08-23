package com.cloud.duolib.bean.duo

import android.graphics.drawable.Drawable

data class BeanApp(
    var pkgName: String,
    var path: String,
    var size: Long? = 0L,
    var apkName: String?,
    var actName: String?,
    val icon: Drawable?
)