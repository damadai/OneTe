package com.cloud.duolib.bean.duo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class BaseFile(open var name: String, open var path: String) : Parcelable