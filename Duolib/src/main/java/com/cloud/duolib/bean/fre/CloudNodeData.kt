package com.cloud.duolib.bean.fre

data class CloudNodeData(
    val key: String?,
    val OpenID: String?,
    val url: String?,
    val phoneCode: Int,
    val phoneID: String,
    val expire: String?,
    val expireTimestamp: Long?,
    val connect: String?
)