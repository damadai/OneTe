package com.cloud.duolib.model

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import com.cloud.duolib.R
import com.cloud.duolib.model.util.getToastTest
import com.cloud.duolib.view.dialog.CommonProgressDialog
import com.cloud.duolib.view.dialog.DialogHorizonHelper
import com.cloud.duolib.view.dialog.DialogProgressHelper
import com.cloud.duolib.view.dialog.DialogVerticalHelper

class TestDialogHelper {
    private val mDialogHorHelper = DialogHorizonHelper()
    private val mDialogVerHelper = DialogVerticalHelper()
    private val mDialogProHelper = DialogProgressHelper()

    fun startTest(act: Activity, reset: () -> Unit) {
        val themeColor =
            arrayOf(
                "0hor换机重置",
                "1hor激励",
                "2ver确认退出",
                "3ver权限允许",
                "4ver权限拒绝",
                "5ver客服跳转",
                "6pro开始使用",
                "7pro上传完毕",
                "8pro修复进度"
            )
        AlertDialog.Builder(act).apply {
            setTitle("弹窗样式预览 ( >.< )")
            setCancelable(true)
            setSingleChoiceItems(
                themeColor, 0
            ) { _, pos ->
                when (pos) {
                    0 -> {
                        mDialogHorHelper.getViewDuoChange(act, "确定要重置么！！！") {
                            getToastTest(act, "click ok")
                            reset.invoke()
                        }
                    }
                    1 -> {
                        mDialogHorHelper.showDialogReward(act, null, null, null, false, null,
                            { getToastTest(act, "click ok") },
                            { getToastTest(act, "click no") })
                    }
                    2 -> {
                        mDialogVerHelper.getViewClickSure(act) {
                            getToastTest(act, "click ok")
                        }
                    }
                    3 -> {
                        mDialogVerHelper.getViewPermission(
                            act,
                            "ver权限允许",
                            R.mipmap.ic_call_210,
                            act.getString(R.string.lineTip),
                            { getToastTest(act, "click ok") },
                            { getToastTest(act, "click no") })
                    }
                    4 -> {
                        mDialogVerHelper.getViewPermission(
                            act,
                            "ver权限拒绝",
                            null,
                            "您已拒绝此权限，如果继续使用请去设置开启",
                            { getToastTest(act, "click ok") },
                            null
                        )
                    }
                    5 -> {
                        mDialogVerHelper.getViewFeedback(act, "number", "group")
                    }
                    6 -> {
                        val mDialog = mDialogProHelper.getViewInfoSure(act, "6pro开始使用", true)
                        Handler().postDelayed({
                            if (!act.isDestroyed) {
                                mDialog?.dismiss()
                            }
                            getToastTest(act, "start ok")
                        }, 2000)
                    }
                    7 -> {
                        mDialogProHelper.getViewInfoSure(act, "pro上传完毕", false)
                    }
                    8 -> {
                        mDialog = mDialogProHelper.showDialogRefresh(act)
                        refreshProgress()
                    }
                }
            }
            show()
        }
    }

    private var mDialog: CommonProgressDialog? = null
    private var mOrderTry = 0
    private val mFreshRunnable = Runnable {
        refreshProgress()
    }

    private fun refreshProgress() {
        mDialog?.setProgress("infoMsg", mOrderTry)
        if (mOrderTry == 100) {
            Handler().removeCallbacks(mFreshRunnable)
            mDialog?.dismiss()
            mOrderTry = 0
        } else {
            Handler().removeCallbacks(mFreshRunnable)
            Handler().postDelayed(mFreshRunnable, 500)
        }
        mOrderTry += 10
    }
}