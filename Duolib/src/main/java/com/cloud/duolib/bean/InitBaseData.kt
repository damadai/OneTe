package com.cloud.duolib.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class InitBaseData(
    open val app_key: String,
    open val app_iv: String,
    open val app_pkg: String,
    open val app_co: Int,
    open var app_token: String,
    open val app_uid: String,
    open val app_channel: String
) : Parcelable
