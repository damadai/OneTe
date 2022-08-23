package com.cloud.duolib.model.helper

import android.app.Activity
import android.widget.Button
import com.cloud.capture.DisplayUtil
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.util.PREFERENCE_SHOW_GUIDE
import com.cloud.duolib.model.util.PreferenceUtil
import com.cloud.duolib.model.util.getToast

//媒体流界面参数
class MediaConfigHelper {
    //first media
    var firstInit = false

    //camera read
    var mJson = ""

    //camera open
    var isCamera = false

    fun initFirstRes(act: Activity, btn: Button?, mDialogHelper: MediaDialogHelper) {
        firstInit = true
        //初始化倒计时
        mDialogHelper.getInitFloatTime(act, mExTime, mType, mInitCloudData)
        //取控件当前的布局参数
        val params = btn?.layoutParams
        if (CloudBuilder.getUiStyle() == 1) {
            btn?.text = act.resources.getString(R.string.push_dop)
            params?.width = DisplayUtil.dip2px(act, 36f)
        } else {
            btn?.text = "1"
            params?.width = DisplayUtil.dip2px(act, 22f)
        }
        params?.height = DisplayUtil.dip2px(act, 22f)
        btn?.layoutParams = params
    }

    //初始化教程工具栏
    fun initGuideAndTool(startDop: Boolean, showTool: () -> Unit, showGuide: () -> Unit) {
        //首次进入已经显示过、预置检查过
        if (firstInit && !startDop) {
            firstInit = false
            //初始化教程、工具栏
            if (PreferenceUtil.getBoolean(PREFERENCE_SHOW_GUIDE, false)) {
                //已经展示过教程
                showTool.invoke()
            } else {
                //初始化教程
                mInitCloudData?.let { data ->
                    if (data.alert == 0) {
                        showGuide.invoke()
                    }
                }
            }
        }
    }

    fun mDuoResponse(act: Activity) = object : OnStrResponse {
        override fun onSuccess(str: String) {
            act.runOnUiThread {
                getToast(act, str)
            }
        }

        override fun onFailed(str: String) {
            act.runOnUiThread {
                getToast(act, str)
            }
        }
    }


    //数据存储
    var mExTime: String? = null
    var mType: Int? = null
    var mKey: String? = null
    var mUid: String? = null
    var mPid: String? = null
    var mExLimit: Long? = null
    var mInitCloudData: InitCloudData? = null

    //活动销毁时释放
    fun resetAll() {
        mJson = ""
        isCamera = false
        firstInit = false
        mExLimit = null
        mExTime = null
        mType = null
        mKey = null
        mUid = null
        mPid = null
        mInitCloudData = null
    }
}