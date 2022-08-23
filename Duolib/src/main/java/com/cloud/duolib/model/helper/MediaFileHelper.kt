package com.cloud.duolib.model.helper

import android.app.Activity
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import com.cloud.capture.activity.CaptureFragment
import com.cloud.capture.activity.CodeUtils
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFileFragment
import com.cloud.duolib.model.OnPackInfoResponse
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import com.cloud.duolib.ui.duo.FeedbackFragment
import com.cloud.duolib.ui.duo.FilePickerFragment

class MediaFileHelper(
    private val dAct: DeviceMediaActivity,
    private val _initMediaCallback: (Boolean) -> Unit
) {
    var mStartPushDop = true

    //活动销毁时释放
    fun resetAll() {
        mStartPushDop = true
    }

    //预置应用判断
    fun getDopAppInstalled(
        activity: Activity,
        dopPkgName: String?,
        duoCommandHelper: DuoCommandHelper,
        onPackInfoResponse: OnPackInfoResponse
    ) {
        //遍历传入列表及预置项
        if (!dopPkgName.isNullOrEmpty() && !arrayListOf(//todo online api
                //"com.tencent.android.qqdownloader",
                "com.tencent.tmgp.sgame", "com.tencent.mm"
            ).contains(dopPkgName)
        ) {
            val dopList = CloudBuilder.getDopSort()
            if (!dopList.isNullOrEmpty()) {
                //已分身列表
                for (dopInfo in dopList) {
                    //存在需分身项目
                    if (dopPkgName == dopInfo.packageName) {
                        //安装状态
                        startDopInstall(dopInfo, duoCommandHelper, onPackInfoResponse)
                        return
                    }
                }
            } else {
                try {
                    startDopInstall(
                        activity.packageManager.getPackageInfo(dopPkgName, 0),
                        duoCommandHelper,
                        onPackInfoResponse
                    )
                    return
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
        onPackInfoResponse.onFailed("空")
    }

    private fun startDopInstall(
        dopInfo: PackageInfo,
        duoCommandHelper: DuoCommandHelper,
        onPackInfoResponse: OnPackInfoResponse
    ) {
        duoCommandHelper.getAppInstallState(dopInfo.packageName, object :
            OnStrResponse {
            override fun onSuccess(str: String) {
                if (str == "6") {
                    onPackInfoResponse.onSuccess(dopInfo)
                } else {
                    onPackInfoResponse.onFailed(str)
                }
            }

            override fun onFailed(str: String) {
                onPackInfoResponse.onSuccess(dopInfo)
            }
        })
    }

    /**
     * 文件选择
     * */
    private val mFraList = arrayListOf(FilePickerFragment(), FeedbackFragment(), CaptureFragment())

    fun getKeyBack(): Boolean {
        mFraList.forEach { fra ->
            if (fra.isVisible) {
                if (fra is FilePickerFragment) {
                    fra.hideFraChild(false)
                } else {
                    hideFra(fra)
                }
                return true
            }
        }
        return false
    }

    //显示fra
    fun showFraCapture(mCommandHelper: DuoCommandHelper, mDuoResponse: OnStrResponse) {
        showFra(mFraList[2], null, null, mCommandHelper, mDuoResponse)
    }

    fun showFraFeedBack(mediaConfig: MediaConfigHelper) {
        showFra(mFraList[1], null, mediaConfig, null, null)
    }

    fun showFraFile(preDopApp: PackageInfo?, mediaConfig: MediaConfigHelper) {
        showFra(mFraList[0], preDopApp, mediaConfig, null, null)
    }

    private fun showFra(
        mFra: Fragment,
        preDopApp: PackageInfo?,
        mediaConfig: MediaConfigHelper?,
        mCommandHelper: DuoCommandHelper?,
        mDuoResponse: OnStrResponse?
    ) {
        val man = dAct.supportFragmentManager
        val trans = man.beginTransaction()
        trans.setCustomAnimations(
            R.anim.slide_up_in,
            R.anim.slide_up_out,
            R.anim.slide_down_in,
            R.anim.slide_down_out
        )
        if (mFra.isAdded) {
            trans.show(mFra)
        } else {
            when (mFra) {
                is FilePickerFragment -> {
                    mFra.arguments = FilePickerFragment.newInstance(
                        mediaConfig?.mPid,
                        mediaConfig?.mInitCloudData,
                        preDopApp
                    )
                }
                is FeedbackFragment -> {
                    mFra.arguments = FeedbackFragment.newInstance(
                        mediaConfig?.mPid,
                        mediaConfig?.mExTime,
                        mediaConfig?.mType ?: 0,
                        mediaConfig?.mInitCloudData
                    )
                }
                is CaptureFragment -> {
                    (mFra as? CaptureFragment)?.setAnalyzeCallback(object :
                        CodeUtils.AnalyzeCallback {
                        override fun onAnalyzeSuccess(mBitmap: Bitmap?, result: String?) {
                            if (!result.isNullOrEmpty()) {
                                if (mDuoResponse != null) {
                                    mCommandHelper?.getActionStart(
                                        "android.intent.action.VIEW",
                                        result,
                                        mDuoResponse
                                    )
                                }
                            }
                            hideFra(mFra)
                        }

                        override fun onAnalyzeFailed() {
                            getToast(dAct, "识别")
                            hideFra(mFra)
                        }
                    })
                }
            }
            trans.add(R.id.fcvFile_media, mFra)
        }
        trans.commit()
    }

    //隐藏fra
    fun hideFra(fra: Fragment) {
        val trans = dAct.supportFragmentManager.beginTransaction()
        if (fra.isAdded) {
            if (fra is CaptureFragment) {
                trans.remove(fra)
            } else {
                trans.hide(fra)
            }
            trans.commit()
            if (fra is FilePickerFragment) {
                hideFraChild(fra)
            }
        }
    }

    fun hideFraChild(fra: FilePickerFragment) {
        //重置预置
        if (mStartPushDop) {
            mStartPushDop = false
            _initMediaCallback.invoke(mStartPushDop)
        }
        try {
            //重置fra文件选中项
            fra.childFragmentManager.fragments.forEach {
                if (it is BaseFileFragment) {
                    it.resetSelectFile()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}