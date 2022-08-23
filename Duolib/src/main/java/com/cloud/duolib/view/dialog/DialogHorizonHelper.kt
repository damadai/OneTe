package com.cloud.duolib.view.dialog

import android.app.Activity
import android.os.CountDownTimer
import com.cloud.duolib.R
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.ui.PreDataActivity

class DialogHorizonHelper {
    //弹窗确认（换机
    fun getViewDuoChange(mAct: Activity, errorInfo: String, _sureCallback: () -> Unit) {
        try {
            //创建弹窗
            val mDialog = CommonHorizonDialog(mAct)
            mDialog.setMsg(null, mAct.getString(R.string.thisCloud, errorInfo))
            mDialog.setBtn(
                mAct.getString(R.string.changeCloud),
                mAct.getString(R.string.stillWait)
            )
            mDialog.setConfirmListener { _, _ ->
                mDialog.dismiss()
                _sureCallback.invoke()
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }

    //弹窗确认（激励
    fun showDialogReward(
        mAct: Activity, type: Int?, name: String?, rId: Int?, nextHost: Boolean, btn: String?,
        _sureCallback: () -> Unit,
        _denyCallback: () -> Unit
    ) {
        try {
            //创建弹窗
            val mDialog = CommonHorizonDialog(mAct)
            //显示提示
            mDialog.setMsg(null, mAct.getString(R.string.view_to_ad_line))
            mDialog.setBtn(mAct.getString(R.string.view_to_ad), null)
            //设置计时
            val aDownTimer = DownTimer(mAct, mDialog, type, name, rId, nextHost, btn)
            //设置按钮
            mDialog.setCancelListener { dInter, _ ->
                aDownTimer.cancel()
                if (!mAct.isDestroyed) {
                    dInter?.dismiss()
                }
                _denyCallback.invoke()
            }
            mDialog.setConfirmListener { dInter, _ ->
                aDownTimer.cancel()
                if (!mAct.isDestroyed) {
                    dInter.dismiss()
                }
                _sureCallback.invoke()
            }
            mDialog.show()
            //开始计时
            aDownTimer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }

    //倒计时2分钟
    private class DownTimer(
        val act: Activity,
        val dialog: CommonHorizonDialog?,
        val type: Int?, val name: String?, val rId: Int?, val nextHost: Boolean, val btn: String?
    ) :
        CountDownTimer(120000, 1) {
        override fun onFinish() {
            this.cancel()
            dialog?.setMsg(act.getString(R.string.view_enter_line), null)
            if (!act.isDestroyed) {
                dialog?.dismiss()
            }
            //倒计结束进入
            if (nextHost) {
                (act as? PreDataActivity)?.mAbs?.startFraLine(type, name, rId)
            } else {
                (act as? PreDataActivity)?.mAbs?.startFraHost(type, name, btn, false)
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            dialog?.setMsg(
                act.getString(
                    R.string.view_wait_line_time,
                    (millisUntilFinished / 1000).toString()
                ), null
            )
        }
    }
}