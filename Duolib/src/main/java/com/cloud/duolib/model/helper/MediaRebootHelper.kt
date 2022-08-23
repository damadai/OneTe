package com.cloud.duolib.model.helper

import android.app.Activity
import android.os.Handler
import com.cloud.duolib.R
import com.cloud.duolib.model.util.getToastTest
import com.cloud.duolib.model.util.logShow
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import com.cloud.duolib.view.dialog.CommonProgressDialog
import com.cloud.duolib.view.dialog.DialogProgressHelper

class MediaRebootHelper(
    private val act: DeviceMediaActivity,
    private val _initMediaCallback: () -> Unit
) {
    //root status change
    var changeReboot = false
    var mOrderTry = 0

    private var startReboot = false
    private var resetKeyTime = 0L
    private var mCommandHelper: DuoCommandHelper? = null
    private val mDialogRefresh = DialogProgressHelper()
    private var mDialog: CommonProgressDialog? = null

    //活动销毁时释放
    fun resetAll() {
        changeReboot = false
        startReboot = false
        mOrderTry = 0
        resetKeyTime = 0L
        Handler().removeCallbacks(mOrderRunnable)
    }

    /**
     * 媒体流响应
     * */
    fun getReInitKeyCallback(firstInit: Boolean, _reInitCallback: () -> Unit): () -> Unit = {
        //不在刷新设备状态下
        if (!startReboot) {
            //弹窗未显示
            if (changeReboot) {
                startReboot = true
                //重启后显示状态恢复弹窗
                mOrderTry = 0
                refreshOrder(mCommandHelper, true)
            } else {
                //首次进入界面出错断开重连;未重连过;非重启状态
                if (firstInit || (resetKeyTime == 0L) || (System.currentTimeMillis() - resetKeyTime > 20000)) {
                    //20秒
                    resetKeyTime = System.currentTimeMillis()
                    _reInitCallback.invoke()
                }
            }
        }
    }

    //刷新状态,切换下附带首次循环判断
    fun refreshOrder(
        commandHelper: DuoCommandHelper?,
        firstReboot: Boolean?, _dopCallback: (() -> Unit)? = null
    ) {
        mCommandHelper = commandHelper.also {
            it?.getOrderInfo({ order ->
                logShow(info = "refreshOrder " + (_dopCallback == null) + " order " + order.toString())
                if (order.OrderStatus == 2) {
                    //重置
                    mOrderTry = 0
                    changeReboot = false
                    startReboot = false
                    if (firstReboot != null) {
                        //停止刷新设备状态
                        stopRunOrderAndReset()
                    }
                    _dopCallback?.invoke()
                } else {
                    refreshOrderDialog(
                        firstReboot,
                        DuoKeyHelper().keyDeviceStatus(order.OrderStatus, order.ProtectTime)
                    )
                }
            }, { str ->
                refreshOrderDialog(firstReboot, str)
            }, true)
        }
    }

    /**
     * 状态弹窗
     * */
    private fun reOrderDialog(firstReboot: Boolean) {
        //递增进度
        when {
            mOrderTry < 60 -> {
                mOrderTry += 3
            }
            mOrderTry < 80 -> {
                mOrderTry += 2
            }
            mOrderTry >= 100 -> {
                //总不能pxx位小数点吧
            }
            else -> {
                mOrderTry += 1
            }
        }
        //循环刷新
        if (firstReboot) {
            mDialog = mDialogRefresh.showDialogRefresh(act)
        } else {
            mDialog?.setProgress(
                if (mOrderTry == 100) act.getString(R.string.fix_finish) else act.getString(
                    R.string.fixing
                ), mOrderTry
            )
        }
        Handler().removeCallbacks(mOrderRunnable)
        Handler().postDelayed(mOrderRunnable, 2000)
    }

    //关闭弹窗和刷新
    private fun stopRunOrderAndReset() {
        //关闭
        if (!act.isDestroyed) {
            mDialog?.dismiss()
        }
        //停止
        Handler().removeCallbacks(mOrderRunnable)
        //reBoot后重连
        _initMediaCallback.invoke()
    }

    //刷新状态
    private val mOrderRunnable = Runnable {
        if (mOrderTry < 70) {
            reOrderDialog(false)
        } else {
            //34秒后再请求状态
            refreshOrder(mCommandHelper, false)
        }
    }

    private fun refreshOrderDialog(firstReboot: Boolean?, str: String) {
        firstReboot?.let {
            //刷新设备状态
            reOrderDialog(it)
            return
        }
        getToastTest(act, str)
    }

    fun getViewInfoSure(act: Activity, info: String) {
        mDialogRefresh.getViewInfoSure(act, info, false)
    }
}