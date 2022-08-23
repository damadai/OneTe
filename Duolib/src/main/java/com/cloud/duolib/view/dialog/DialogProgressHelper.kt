package com.cloud.duolib.view.dialog

import android.app.Activity
import com.cloud.duolib.R
import com.cloud.duolib.model.util.getClick
import com.cloud.duolib.model.util.getToast

class DialogProgressHelper {
    //上传完成信息、开始使用、结束使用
    fun getViewInfoSure(
        mAct: Activity,
        titleInfo: String,
        onlyTips: Boolean
    ): CommonProgressDialog? {
        var mDialog: CommonProgressDialog? = null
        try {
            //创建弹窗
            mDialog = CommonProgressDialog(mAct)
            //显示标题
            mDialog.setMsg(titleInfo)
            //隐藏部分
            if (onlyTips) {
                mDialog.hideViews(showClose = false, showQuit = false, showProgress = false)
            } else {
                mDialog.hideViews(showClose = false, showQuit = true, showProgress = false)
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
        return mDialog
    }

    //重启刷新弹窗
    fun showDialogRefresh(mAct: Activity): CommonProgressDialog? {
        var mDialog: CommonProgressDialog? = null
        try {
            //创建弹窗
            mDialog = CommonProgressDialog(mAct)
            //显示标题
            mDialog.setMsg(mAct.getString(R.string.fix_start))
            //进度样式
            mDialog.setProgressGreen()
            //隐藏其它
            mDialog.hideViews(showClose = true, showQuit = false, showProgress = true)
            //监听
            mDialog.setCancelListener {
                getClick {
                    getToast(mAct, R.string.fix_can_not_pause)
                }
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
        return mDialog
    }
}