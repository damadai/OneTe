package com.cloud.duolib.model

import android.content.pm.PackageInfo

interface OnBaseSucResponse {
    fun onSuccess()
}

interface OnBaseFailStr {
    fun onFailed(str: String)
}

interface OnStrResponse : OnBaseFailStr {
    fun onSuccess(str: String)
}

interface OnBoolResponse : OnBaseFailStr {
    fun onSuccess(ok: Boolean)
}

interface OnPackInfoResponse : OnBaseFailStr {
    fun onSuccess(data: PackageInfo)
}