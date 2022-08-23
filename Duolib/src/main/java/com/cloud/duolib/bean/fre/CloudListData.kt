package com.cloud.duolib.bean.fre

data class CloudListData(
    var types: Int = 0,
    var logo: String? = null,
    var title: String? = null,
    var price: Int = 0,
    var duration: Int = 0,
    var content: String? = null,
    var btn: String? = null,
    var defaultSelect: Boolean = false
)

//old
data class CloudListNewData(
    val server: ArrayList<Server>,
    val price: ArrayList<Price>?,
    val btn: String?,
    val marketBtn: String?,
    val marketUrl: String?,
    val marketTypeList: ArrayList<Int>?
)

data class Server(
    val types: Int,
    val name: String,
    val content: String,
    val openServer: Int? = 0,
    var defaultSelect: Boolean = false
)

data class Price(
    val name: String,
    val value: String
)