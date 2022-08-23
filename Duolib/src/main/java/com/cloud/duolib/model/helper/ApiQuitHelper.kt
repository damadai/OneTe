package com.cloud.duolib.model.helper

import android.app.Activity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.util.LOG_EXCEPT
import com.cloud.duolib.model.util.getNet
import com.cloud.duolib.model.util.getToast

class ApiQuitHelper {
    //请求退队
    fun apiQuitLine(act: Activity, type: Int?, data: InitCloudData?) {
        if (type != null && data != null) {
            CloudHttpUtils.getInstance().getExitRx(
                data.app_pkg,
                data.app_co,
                data.app_token,
                type,
                data.app_key,
                data.app_iv,
                object : CloudHttpUtils.StrResponse {
                    override fun onSuccess(str: String?) {
                        act.finish()
                        getToast(act, str)
                    }

                    override fun onFailed(status: Int, msg: String) {
                        act.finish()
                        getToast(act, "退出排队$status")
                    }
                })
        } else {
            act.finish()
        }
    }

    //请求回收
    fun apiRecoveryDevice(
        repair: Int,
        type: Int?,
        data: InitCloudData?,
        response: CloudHttpUtils.StrResponse
    ) {
        if (type != null && data != null) {
            getNet({
                CloudHttpUtils.getInstance().getRecoverRx(
                    data.app_pkg,
                    data.app_co,
                    data.app_token,
                    type, repair, data.app_key,
                    data.app_iv, response
                )
            }, { response.onFailed(555, "无网络") })
        } else {
            response.onFailed(555, "无初始")
        }
    }

    fun apiReportError(
        app_pkg: String,
        app_token: String,
        app_key: String,
        app_iv: String,
        app_co: Int,
        errorInfo: String,
        errorType: Int,
        fixType: Int,
        orderId: String,
        expire: String?,
        channel: String,
        baseResponse: CloudHttpUtils.BaseResponse
    ) {
        try {
            CloudHttpUtils.getInstance()
                .getErrorRx(
                    app_pkg,
                    app_co,
                    app_token,
                    app_key,
                    app_iv,
                    errorType,
                    fixType,
                    errorInfo,
                    orderId,
                    channel,
                    android.os.Build.MODEL,
                    android.os.Build.VERSION.RELEASE,
                    expire,
                    baseResponse
                )
        } catch (e: Exception) {
            e.printStackTrace()
            baseResponse.onFailed(LOG_EXCEPT, e.message)
        }
    }
}