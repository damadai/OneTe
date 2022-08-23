package com.cloud.duolib.bean.fre

data class CloudHostData(
    val roomId: Int,
    val room: String,
    val net: String?,
    var defaultSelect: Boolean = false
)