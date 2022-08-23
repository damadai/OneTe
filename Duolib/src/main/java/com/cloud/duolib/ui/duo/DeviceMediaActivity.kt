package com.cloud.duolib.ui.duo

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.bean.DopAppData
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.model.*
import com.cloud.duolib.model.helper.*
import com.cloud.duolib.model.util.*
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.view.ToolsPopWindow
import com.cloud.duolib.view.setNavigationBarTranslucent
import kotlinx.android.synthetic.main.duo_activity_device_media.*
import kotlinx.android.synthetic.main.duo_view_media_menu.*
import kotlinx.android.synthetic.main.duo_view_media_tools.*

class DeviceMediaActivity : com.cloud.duolib.base.BasePermissionActivity(), View.OnClickListener {
    //工具类
    private var mPopWindow: ToolsPopWindow? = null
    private var mPermission = PermissionApplyHelper()

    //参数配置
    private val mDuoMediaConfig = MediaConfigHelper()

    //布局构建器
    private val mDialogHelper = MediaDialogHelper()

    //响应构建器
    private val mCommandHelper = DuoCommandHelper()

    //媒体构建器
    private val mResumeHelper = MediaResumeHelper(mTag)
    private val mRebootHelper = MediaRebootHelper(this@DeviceMediaActivity) { initData(null) }
    private val mFileHelper = MediaFileHelper(this@DeviceMediaActivity) {
        initData(null)
        startInitGuideTool(it)
    }

    companion object {
        private const val mTag = "77777DeviceMedia"
        private const val ACT_MEDIA_VIEW_TIME = "ACT_MEDIA_VIEW_TIME"
        private const val ACT_MEDIA_VIEW_LIMIT = "ACT_MEDIA_VIEW_LIMIT"
        private const val ACT_MEDIA_SERVER_TYPE = "ACT_MEDIA_SERVER_TYPE"
        private const val ACT_MEDIA_DATA_KEY = "ACT_MEDIA_DATA_KEY"
        private const val ACT_MEDIA_DATA_UID = "ACT_MEDIA_DATA_UID"
        private const val ACT_MEDIA_DATA_PID = "ACT_MEDIA_DATA_PID"
        private const val ACT_MEDIA_INIT_DATA = "ACT_MEDIA_INIT_DATA"

        fun newInstance(
            act: Activity,
            time: String?,
            type: Int?,
            key: String,
            uid: String,
            pid: String,
            limit: Long?,
            data: InitCloudData?
        ) {
            if (CloudBuilder.getActTop() is DeviceMediaActivity) return
            Intent(act, DeviceMediaActivity::class.java).apply {
                this.putExtra(ACT_MEDIA_VIEW_TIME, time)
                this.putExtra(ACT_MEDIA_VIEW_LIMIT, limit)
                this.putExtra(ACT_MEDIA_SERVER_TYPE, type)
                this.putExtra(ACT_MEDIA_DATA_KEY, key)
                this.putExtra(ACT_MEDIA_DATA_UID, uid)
                this.putExtra(ACT_MEDIA_DATA_PID, pid)
                this.putExtra(ACT_MEDIA_INIT_DATA, data)
                if (CloudBuilder.getActExist(DeviceMediaActivity::class.java)) {
                    this.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                act.startActivity(this)
                if (act is PreDataActivity) {
                    act.finish()
                }
            }
        }
    }

    override fun getLayoutId() = R.layout.duo_activity_device_media
    override fun initView(saveInstanceState: Bundle?) {
        setNavigationBarTranslucent(this@DeviceMediaActivity, true, null)
        //接收传值
        getIntentData(intent)
        //初始化倒计时、小红点
        mDuoMediaConfig.initFirstRes(this@DeviceMediaActivity, btRed_media, mDialogHelper)
        //点按监听
        initListener()
    }

    override fun initBeforeView() {

    }

    /**
     * 点按监听
     * */
    private fun initListener() {
        //菜单按钮
        btBack_media?.setOnClickListener(this)
        btHome_media?.setOnClickListener(this)
        btMenu_media?.setOnClickListener(this)
        //上传文件
        btRed_media?.setOnClickListener(this)
        btAudio_media?.setOnClickListener(this)
        btAudio_media?.setOnLongClickListener {
            startShowFraFile()
            true
        }
        //工具栏隐藏开关
        ivSet_media?.setOnClickListener(this)
        ivSet_media?.setOnLongClickListener {
            ivSet_media?.callOnClick()
            true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                logShow(mTag, "onConfigurationChanged=横屏")
                this.findViewById<ConstraintLayout>(R.id.clTool_media)?.let { cl ->
                    cl.clearAnimation()
                    cl.invalidate()
                    //组合动画方式2
                    (cl.layoutParams as? RelativeLayout.LayoutParams)?.let { params ->
                        params.width = this.window.decorView.width
                        params.addRule(RelativeLayout.ALIGN_PARENT_END)
                        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        cl.layoutParams = params
                    }
                    val xy = cl.width.toFloat() / 2 - (cl.height) / 2
                    ObjectAnimator.ofPropertyValuesHolder(
                        cl,
                        PropertyValuesHolder.ofFloat("rotation", -90f),
                        PropertyValuesHolder.ofFloat("translationX", 0f, xy),
                        PropertyValuesHolder.ofFloat("translationY", 0f, xy)
                    ).start()
                    (this.findViewById<FrameLayout>(R.id.videoContainer_media)?.layoutParams as? RelativeLayout.LayoutParams)?.let {
                        it.removeRule(RelativeLayout.ABOVE)
                        //it.addRule(RelativeLayout.START_OF, R.id.clTool_media)
                        it.setMargins(0, 0, cl.height, 0)
                    }
                    cl.findViewById<Button>(R.id.btRed_media).rotation = 90f
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                logShow(mTag, "onConfigurationChanged=竖屏")
                this.findViewById<ConstraintLayout>(R.id.clTool_media).let { cl ->
                    cl.clearAnimation()
                    cl.invalidate()
                    (cl.layoutParams as? RelativeLayout.LayoutParams)?.let { params ->
                        params.width = this.window.decorView.width
                        params.removeRule(RelativeLayout.ALIGN_PARENT_END)
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        cl.layoutParams = params
                    }
                    ObjectAnimator.ofPropertyValuesHolder(
                        cl,
                        PropertyValuesHolder.ofFloat("rotation", 0f),
                        PropertyValuesHolder.ofFloat("translationX", 0f),
                        PropertyValuesHolder.ofFloat("translationY", 0f)
                    ).start()
                    cl.findViewById<Button>(R.id.btRed_media).rotation = 0f
                }
                (this.findViewById<FrameLayout>(R.id.videoContainer_media)?.layoutParams as? RelativeLayout.LayoutParams)?.let {
                    it.addRule(RelativeLayout.ABOVE, R.id.clTool_media)
                    //it.removeRule(RelativeLayout.START_OF)
                    it.setMargins(0, 0, 0, 0)
                }
            }
            else -> logShow(mTag, "onConfigurationChanged=${newConfig.orientation}")
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        startInitGuideTool(mFileHelper.mStartPushDop)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        getIntentData(intent)
        getOrderInit("重载")
    }

    private fun getIntentData(intent: Intent?) {
        mDuoMediaConfig.mExTime = intent?.getStringExtra(ACT_MEDIA_VIEW_TIME)
        mDuoMediaConfig.mExLimit = intent?.getLongExtra(ACT_MEDIA_VIEW_LIMIT, 0)
        mDuoMediaConfig.mType = intent?.getIntExtra(ACT_MEDIA_SERVER_TYPE, 0)
        mDuoMediaConfig.mKey = intent?.getStringExtra(ACT_MEDIA_DATA_KEY)
        mDuoMediaConfig.mUid = intent?.getStringExtra(ACT_MEDIA_DATA_UID)
        mDuoMediaConfig.mPid = intent?.getStringExtra(ACT_MEDIA_DATA_PID)
        mDuoMediaConfig.mInitCloudData = intent?.getParcelableExtra(ACT_MEDIA_INIT_DATA)
    }

    private fun getOrderInit(type: String?) {
        mCommandHelper.initOrderHelper(
            mDuoMediaConfig.mKey,
            mDuoMediaConfig.mUid,
            mDuoMediaConfig.mPid,
            mDuoMediaConfig.mExLimit,
            mDuoMediaConfig.mType,
            mDuoMediaConfig.mInitCloudData,
            mResponse = object : OnStrResponse {
                override fun onSuccess(str: String) {
                    mRebootHelper.refreshOrder(mCommandHelper, null)
                    getToastTest(this@DeviceMediaActivity, "$type $str")
                }

                override fun onFailed(str: String) {
                    getToastTest(this@DeviceMediaActivity, "$type $str")
                }
            }
        )
        this.finish()
    }

    override fun onResume() {
        super.onResume()
        hideSoftKeyBoard()
        if (mFileHelper.mStartPushDop) {
            //检查预置
            mFileHelper.getDopAppInstalled(this@DeviceMediaActivity,
                mDuoMediaConfig.mInitCloudData?.appn?.pkgName,
                mCommandHelper,
                object : OnPackInfoResponse {
                    override fun onSuccess(data: PackageInfo) {
                        //存在则先预置
                        mFileHelper.showFraFile(data, mDuoMediaConfig)
                    }

                    override fun onFailed(str: String) {
                        //不存在则标记不需检查
                        mFileHelper.mStartPushDop = false
                        if ((mDuoMediaConfig.mInitCloudData?.appn?.pkgName == "com.jingdong.app.mall")) {
                            if (str == "7") {
                                initData(mDuoMediaConfig.mInitCloudData?.appn)
                            }
                        } else {
                            initData(mDuoMediaConfig.mInitCloudData?.appn)
                        }
                    }
                })
        } else if (mRebootHelper.mOrderTry == 0) {
            //非重启状态
            initData(null)
        }
    }

    override fun onPause() {
        super.onPause()
        mResumeHelper.stopMediaCamera()
    }

    override fun onDestroy() {
        mPopWindow?.dismiss()
        super.onDestroy()
        mResumeHelper.destroyMediaCommand()
        mDuoMediaConfig.resetAll()
        mRebootHelper.resetAll()
        mFileHelper.resetAll()
    }

    private fun initData(dopOption: DopAppData?) {
        /**
         * 设备请求类初始化
         * */
        mCommandHelper.getUserInfo({ data ->
            mResumeHelper.initMediaResume(
                this@DeviceMediaActivity, videoContainer_media,
                data, mCommandHelper, mDuoMediaConfig, mDialogHelper, mPermission,
                mRebootHelper.getReInitKeyCallback(mDuoMediaConfig.firstInit) {
                    getOrderInit(
                        "重播"
                    )
                }
            ) { startShowFraFile() }
            /**
             * 设备请求类实现
             * */
            if (dopOption != null) {
                mRebootHelper.refreshOrder(mCommandHelper, null) {
                    startZy()
                }
            } else {
                mRebootHelper.refreshOrder(mCommandHelper, null)
            }
        }, {
            if (mDuoMediaConfig.mKey.isNullOrEmpty() || mDuoMediaConfig.mPid.isNullOrEmpty() || mDuoMediaConfig.mUid.isNullOrEmpty()) {
                getIntentData(intent)
            }
            getOrderInit("重连")
        })
        if (dopOption != null && !dopOption.open_intent.isNullOrEmpty()) {
            //打开领券
            mCommandHelper.getAppStart(dopOption.open_intent,
                "${dopOption.open_intent}.MainActivity",
                object : OnStrResponse {
                    override fun onSuccess(str: String) {}

                    override fun onFailed(str: String) {}
                })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (mFileHelper.getKeyBack()) {
                    return true
                }
                mCommandHelper.getUserInfo(
                    { mResumeHelper.getKeyBack(it.OrderId) },
                    { getToast(this@DeviceMediaActivity, "无云机") })
            }
            else -> {
                return super.onKeyUp(keyCode, event)
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvPass_guide -> {
                startGuideClick()
            }
            R.id.tvOk_guide -> {
                startGuideClick()
            }
            R.id.btBack_media -> {
                mCommandHelper.getUserInfo({
                    mResumeHelper.getKeyBack(it.OrderId)
                }, { getToast(this@DeviceMediaActivity, "无云机") })
            }
            R.id.btHome_media -> {
                mCommandHelper.getUserInfo({
                    mResumeHelper.getKeyHome(it.OrderId)
                }, { getToast(this@DeviceMediaActivity, "无云机") })
            }
            R.id.btMenu_media -> {
                mCommandHelper.getUserInfo({
                    mResumeHelper.getKeySwitch(it.OrderId)
                }, { getToast(this@DeviceMediaActivity, "无云机") })
            }
            R.id.ivSet_media -> {
                if (mPopWindow == null) {
                    mPopWindow = ToolsPopWindow(this@DeviceMediaActivity, this)
                    mPopWindow?.showAsDropDown(ivSet_media)
                    mPopWindow?.dismiss()
                }
                mPopWindow?.showPop(
                    ivSet_media,
                    this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                )
            }
            R.id.btAudio_media -> {
                getClick {
                    startShowFraFile()
                }
            }
            R.id.btRed_media -> {
                btAudio_media?.callOnClick()
            }
            R.id.btScan_tools -> {
                mPopWindow?.dismiss()
                startPermissionScan()
            }
            R.id.btShake_tools -> {
                mCommandHelper.getMediaShake(mDuoMediaConfig.mDuoResponse(this@DeviceMediaActivity))
                mPopWindow?.dismiss()
            }
            R.id.btShot_tools -> {
                startPermissionShot()
                mPopWindow?.dismiss()
            }
            R.id.btHelp_tools -> {
                mPopWindow?.dismiss()
                mFileHelper.showFraFeedBack(mDuoMediaConfig)
            }
            R.id.btFix_tools -> {
                //todo delete test
                mCommandHelper.getRootState(object : OnBoolResponse {
                    override fun onSuccess(ok: Boolean) {
                        mCommandHelper.getOrderRoot(
                            !ok,
                            mDuoMediaConfig.mDuoResponse(this@DeviceMediaActivity)
                        )
                    }

                    override fun onFailed(str: String) {
                        TestDialogHelper().startTest(this@DeviceMediaActivity) { startReset(null) }
                    }
                })
            }
            R.id.btFix2_tools -> {
                //同步zy
                startZy()
                /*MediaAppDopHelper().startAppLogin(
                    this@DeviceMediaActivity,
                    mCommandHelper,
                    mRebootHelper,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        .absolutePath + "/.zy/.temp.xml",
                    "/data/data/zanyouninesix.say/shared_prefs/user_config.xml", true,
                ) {
                }*/
            }
            R.id.btFix3_tools -> {
                mCommandHelper.getRunAppLogin(
                    "", "", "", arrayListOf(""), 0,
                    object : OnStrResponse {
                        override fun onSuccess(str: String) {
                            getCxt(this@DeviceMediaActivity, {
                                ToastUtils.showMsgLong(it, "记录$str")
                            }, null)
                        }

                        override fun onFailed(str: String) {
                            getCxt(this@DeviceMediaActivity, {
                                ToastUtils.showMsgLong(it, "记录$str")
                            }, null)
                        }
                    })
            }
        }
    }

    private fun startZy() {
        mDuoMediaConfig.mInitCloudData?.appn?.let { dopOption ->
            if (!dopOption.pkgName.isNullOrEmpty() && !dopOption.localPath.isNullOrEmpty() && !dopOption.remotePath.isNullOrEmpty()) {
                MediaAppDopHelper().startAppLogin(
                    this@DeviceMediaActivity, mCommandHelper, mRebootHelper,
                    dopOption.pkgName, dopOption.launchAct,
                    dopOption.localPath, dopOption.remotePath,
                    dopOption.rootControl ?: true,
                ) {
                    runOnUiThread {
                        mPopWindow?.dismiss()
                    }
                }
                return
            }
        }
        getCxt(this@DeviceMediaActivity, {
            ToastUtils.showMsgShort(it, "无分身信息")
        }, null)
    }

    //初始化教程点击
    private fun startGuideClick() {
        PreferenceUtil.putBoolean(PREFERENCE_SHOW_GUIDE, true)
        ivSet_media?.callOnClick()
    }

    //初始化教程工具栏
    private fun startInitGuideTool(startDop: Boolean) {
        mDuoMediaConfig.initGuideAndTool(startDop,
            { ivSet_media?.callOnClick() },
            {
                mDialogHelper.getViewGuideShow(this@DeviceMediaActivity, this@DeviceMediaActivity)
            })
    }

    //显示扫描请求弹窗
    private fun startPermissionScan() {
        mPermission.applyPermissionCamera(
            this@DeviceMediaActivity,
            object : OnBaseSucResponse {
                override fun onSuccess() {
                    mFileHelper.showFraCapture(
                        mCommandHelper,
                        mDuoMediaConfig.mDuoResponse(this@DeviceMediaActivity)
                    )
                }
            }, mDialogHelper
        )
    }

    //显示截图请求弹窗
    private fun startPermissionShot() {
        mCommandHelper.getScreenShot(
            this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
            mDuoMediaConfig.mDuoResponse(this@DeviceMediaActivity),
            this@DeviceMediaActivity,
            mDialogHelper, mPermission
        )
    }

    //显示存储权限弹窗
    fun startPermissionStorage(listener: OnBaseSucResponse) {
        mPermission.applyPermissionStorage(
            this@DeviceMediaActivity, listener, mDialogHelper, true
        )
    }

    //显示文件完毕弹窗
    fun startFileEndTips(info: String) {
        mRebootHelper.getViewInfoSure(this@DeviceMediaActivity, info)
    }

    //显示文件碎片
    private fun startShowFraFile() {
        btRed_media?.isVisible = false
        mFileHelper.showFraFile(null, mDuoMediaConfig)
    }

    //隐藏碎片
    fun startHideFra(fra: Fragment) {
        mFileHelper.hideFra(fra)
    }

    //隐藏文件子碎片
    fun startHideFraChild(fra: FilePickerFragment) {
        mFileHelper.hideFraChild(fra)
    }

    //开始显示客服
    fun startShowGroupDialog(number: String?, group: String?) {
        mDialogHelper.getViewFeedback(this@DeviceMediaActivity, number, group)
    }

    //开始重载活动
    fun startResume(fra: Fragment) {
        onPause()
        onResume()
        mFileHelper.hideFra(fra)
    }

    //开始重启媒体
    fun startReboot(fra: Fragment) {
        mCommandHelper.getOrderReboot(object : OnStrResponse {
            override fun onSuccess(str: String) {
                runOnUiThread {
                    mPopWindow?.dismiss()
                }
                mRebootHelper.changeReboot = true
                mFileHelper.hideFra(fra)
            }

            override fun onFailed(str: String) {
                runOnUiThread {
                    getToast(this@DeviceMediaActivity, str)
                }
            }
        })
    }

    //开始重置媒体
    fun startReset(fra: Fragment?) {
        mCommandHelper.getOrderReset(object : OnStrResponse {
            override fun onSuccess(str: String) {
                runOnUiThread {
                    mPopWindow?.dismiss()
                }
                mRebootHelper.changeReboot = true
                if (fra != null) {
                    mFileHelper.hideFra(fra)
                }
            }

            override fun onFailed(str: String) {
                runOnUiThread {
                    getToast(this@DeviceMediaActivity, str)
                }
            }
        })
    }
}