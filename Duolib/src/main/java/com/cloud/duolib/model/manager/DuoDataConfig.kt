package com.cloud.duolib.model.manager

import com.cloud.duolib.bean.InitCloudData
import com.cyjh.ddy.media.bean.DdyUserInfo
import com.cyjh.ddysdk.order.base.bean.DdyOrderInfo

object DuoDataConfig {
    //数据存储
    private var mLoginId: String? = null
    private var mUser = DdyUserInfo()
    var mOrder: DdyOrderInfo? = null
    var mLimit: Long? = null
    var mType: Int? = null
    var mQuitData: InitCloudData? = null
    var mNtpDif: Long? = null

    fun getLoginId() = mLoginId

    fun setLoginOrder(loginId: String, orderId: String) {
        mLoginId = loginId
        try {
            mUser.OrderId = orderId.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLimitData(limit: Long?, type: Int?, data: InitCloudData?) {
        mLimit = limit
        mType = type
        mQuitData = data
    }

    fun setNtpDif(dif: Long?) {
        mNtpDif = dif
    }

    fun setUserUid(newData: String) {
        mUser.UCID = newData
    }

    fun setUserOid(newData: Long) {
        mUser.OrderId = newData
    }

    fun getUser() = mUser
}