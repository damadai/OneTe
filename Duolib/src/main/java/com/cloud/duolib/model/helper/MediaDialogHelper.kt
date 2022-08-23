package com.cloud.duolib.model.helper

import android.app.Activity
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.core.content.ContextCompat
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.OsTimeUtils
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.view.dialog.DialogHelper
import com.cloud.duolib.view.dialog.DialogVerticalHelper
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.SidePattern

class MediaDialogHelper {
    private var mDialogVerHelper: DialogVerticalHelper? = null

    /**
     * 弹窗确认（权限请求
     * */
    fun getViewPermission(
        mAct: Activity,
        titleInfo: String,
        logoRes: Int?,
        contentInfo: String,
        sureListener: View.OnClickListener,
        denyListener: View.OnClickListener?
    ) {
        //创建弹窗
        if (mDialogVerHelper == null) {
            mDialogVerHelper = DialogVerticalHelper()
        }
        mDialogVerHelper?.getViewPermission(
            mAct,
            titleInfo,
            logoRes,
            contentInfo,
            sureListener,
            denyListener
        )
    }

    /**
     * 弹窗确认（云机截图
     * */
    fun getViewBg(
        mAct: BasePermissionActivity,
        imageBytes: ByteArray, ver: Boolean?,
        mListener: OnStrResponse,
        permissionApplyHelper: PermissionApplyHelper
    ) {
        //创建弹窗
        if (mDialogVerHelper == null) {
            mDialogVerHelper = DialogVerticalHelper()
        }
        mDialogVerHelper?.getViewBg(mAct, imageBytes, ver, mListener, this, permissionApplyHelper)
    }

    /**
     * 弹窗确认（跳转客服
     * */
    fun getViewFeedback(mAct: Activity, number: String?, group: String?) {
        //创建弹窗
        if (mDialogVerHelper == null) {
            mDialogVerHelper = DialogVerticalHelper()
        }
        mDialogVerHelper?.getViewFeedback(mAct, number, group)
    }

    /**
     * 弹窗确认（跳转排队
     * */
    fun getViewFeedLine(
        mAct: Activity,
        titleInfo: String,
        sureBtn: String,
        listener: View.OnClickListener
    ) {
        //创建弹窗
        if (mDialogVerHelper == null) {
            mDialogVerHelper = DialogVerticalHelper()
        }
        mDialogVerHelper?.getViewFeedLine(mAct, titleInfo, sureBtn, listener)
    }


    /**
     * 初始化子布局显示教程
     * */
    fun getViewGuideShow(mAct: Activity, listener: View.OnClickListener) {
        DialogHelper().getViewDuoGuide(mAct, listener)
    }

    /**
     * 初始化倒计时、显示浮窗
     * */
    fun getInitFloatTime(
        mAct: Activity,
        exTime: String?,
        type: Int?,
        data: InitCloudData?
    ) {
        initFloat(mAct, exTime) {
            //创建弹窗
            if (mDialogVerHelper == null) {
                mDialogVerHelper = DialogVerticalHelper()
            }
            //显示确认弹窗
            mDialogVerHelper?.getViewClickSure(mAct) {
                //点击确认
                if ((data != null) && (type != null) && CloudBuilder.getIsFree()) {
                    ApiQuitHelper().apiRecoveryDevice(
                        0,
                        type,
                        data,
                        object : CloudHttpUtils.StrResponse {
                            override fun onSuccess(str: String?) {
                                finishAllCloudAct(mAct)
                                getToast(mAct, str)
                            }

                            override fun onFailed(status: Int, msg: String) {
                                //释放未成功，跳转主界面
                                finishAllCloudAct(mAct)
                                getToast(mAct, msg)
                            }
                        })
                } else {
                    finishAllCloudAct(mAct)
                }
            }
        }
    }

    //首次进入媒体流开启倒计时
    private fun initFloat(act: Activity, exTime: String?, _quitCallback: () -> Unit) {
        var clickAble = false
        var time: Int? = null
        try {
            time = exTime?.toInt() ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        EasyFloat.with(act)
            .setLayout(R.layout.duo_view_float_time)
            .setSidePattern(SidePattern.RESULT_SIDE)
            .setDragEnable(true)
            .setLocation(100, 100)
            .setFilter(BasePermissionActivity::class.java)
            .registerCallback {
                createResult { isCreated, _, view ->
                    if (isCreated) {
                        view?.alpha = 0.4f
                        view?.findViewById<Chronometer>(R.id.ct_float)?.let { ch ->
                            if (time != null) {
                                ch.base =
                                    SystemClock.elapsedRealtime() + 60 * 1000 * time
                                ch.format = "%s"
                                ch.onChronometerTickListener =
                                    Chronometer.OnChronometerTickListener {
                                        ch.text = ch.text.toString().substring(1)
                                        if (SystemClock.elapsedRealtime() - ch.base >= 0) {
                                            //计时结束
                                            ch.stop()
                                        }
                                    }
                            } else {
                                ch.base = SystemClock.elapsedRealtime()
                                ch.onChronometerTickListener =
                                    Chronometer.OnChronometerTickListener {
                                        ch.text = exTime
                                        if ((OsTimeUtils.string2Millis(exTime) - OsTimeUtils.getNowMills()) < 1000 * 60 * 2) {
                                            //计时结束
                                            ch.stop()
                                            //剩 2分钟
                                            ch.setTextColor(
                                                ContextCompat.getColor(
                                                    ch.context,
                                                    R.color.red
                                                )
                                            )
                                        }
                                    }
                            }
                            ch.start()
                        }
                    }
                }
                touchEvent { view, _ ->
                    view.setOnClickListener {
                        if (clickAble) {
                            view.alpha = 0.4f
                            clickAble = false
                            //点击显示退出弹窗
                            _quitCallback.invoke()
                        } else {
                            view.alpha = 0.8f
                            clickAble = true
                            Handler().postDelayed({
                                view.alpha = 0.4f
                                clickAble = false
                            }, 3000)
                        }
                    }
                }
                drag { view, _ ->
                    view.alpha = 0.8f
                    clickAble = true
                }
                dragEnd { view ->
                    Handler().postDelayed({
                        view.alpha = 0.4f
                        clickAble = false
                    }, 2600)
                }
            }
            .show()
    }

    private fun finishAllCloudAct(act: Activity?) {
        FilePickManager.resetAll()
        act?.finish()
        CloudBuilder.getQuitSureCallback()
    }
}