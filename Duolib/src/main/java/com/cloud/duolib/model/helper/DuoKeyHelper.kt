package com.cloud.duolib.model.helper

import android.text.TextUtils
import com.cyjh.ddysdk.order.base.bean.DdyOrderInfo

class DuoKeyHelper {
    /**
     * 文件推送状态码释义
     * */
    private val keyPushFileMap = mapOf(0 to "失败", 1 to "成功", 2 to "商户未授权", 3 to "参数错误", 4 to "无数据")

    //安装进度回调
    private val keyInstallStateMap = mapOf("4" to "下载中", "5" to "安装中", "6" to "未安装", "7" to "已安装")

    //安装应用回调
    private val keyInstallAppMap = mapOf(1 to "安装成功", 2 to "下载失败", 3 to "安装失败")

    /**
     * 订单状态码释义
     * */
    fun keyDeviceStatus(status: Int, ProtectTime: String?): String {
        var str = ""
        when (status) {
            DdyOrderInfo.STATUS_Ended_Exception, DdyOrderInfo.STATUS_Ended -> {
                str = "设备异常"
            }
            DdyOrderInfo.STATUS_Data_Back_Fail, DdyOrderInfo.STATUS_Recovery_Ab_Normal, DdyOrderInfo.STATUS_Recovery_Normal -> {
                str = "备份恢复异常"
            }
            DdyOrderInfo.STATUS_NO_Device -> {
                str = "设备异常"
            }
            DdyOrderInfo.STATUS_Recover_Optimization -> str = "设备优化中"
            DdyOrderInfo.STATUS_Recover_Recharge -> str = "正在获取云端数据"
            DdyOrderInfo.STATUS_Recover_Exception -> str = "正在迁移数据"
            DdyOrderInfo.STATUS_Rebooting -> str = "重启中"
            DdyOrderInfo.STATUS_Resetting -> str = "重置中"
            DdyOrderInfo.STATUS_Deleted_Save, DdyOrderInfo.STATUS_Deleted_Extend -> {
                var strState = "设备过期"
                if (!TextUtils.isEmpty(ProtectTime)) {
                    strState += "，数据保存剩余时间：$ProtectTime"
                }
                str = strState
            }
            else -> str = "设备正常"
        }
        return str
    }

    class KeyPingShow(rtt: String) {
        var min: String? = null
        var avg: String? = null
        var max: String? = null
        var mdev: String? = null

        init {
            val values = rtt.split("/".toRegex()).toTypedArray()
            if (values.size == 4) {
                min = values[0]
                avg = values[1]
                max = values[2]
                mdev = values[3]
            } else {
                min = "?"
                avg = "?"
                max = "?"
                mdev = "?"
            }
        }
    }
}