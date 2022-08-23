package com.cloud.duolib.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DopAppData(
    val pkgName: String?,
    val launchAct: String?,
    val localPath: String?,
    val remotePath: String?,
    val rootControl: Boolean?,
    val modeName: String?,
    val open_intent: String?,
    var type: Int?
) : Parcelable