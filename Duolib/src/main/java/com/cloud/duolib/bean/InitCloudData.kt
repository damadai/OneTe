package com.cloud.duolib.bean

import kotlinx.android.parcel.Parcelize

@Parcelize
class InitCloudData constructor(
    override var app_key: String,
    override val app_iv: String,
    override val app_pkg: String,
    override val app_co: Int,
    override var app_token: String,
    override val app_uid: String,
    override val app_channel: String,
    val alert: Int,
    val appn: DopAppData?,
    val qq_number: String?,
    val qq_group: String?
) : InitBaseData(app_key, app_iv, app_pkg, app_co, app_token, app_uid, app_channel)