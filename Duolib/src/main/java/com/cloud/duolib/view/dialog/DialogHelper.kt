package com.cloud.duolib.view.dialog

import android.app.Activity
import android.view.View
import com.cloud.duolib.R
import com.cloud.duolib.model.util.getToast

class DialogHelper {
    //弹窗确认（教程
    fun getViewDuoGuide(mAct: Activity, listener:  View.OnClickListener) {
        try {
            //创建弹窗
            val mDialog = CommonDialog(mAct)
            mDialog.setConfirmListener(listener)
            mDialog.setCancelListener(listener)
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }
}