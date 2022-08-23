package com.cloud.duolib

import android.app.Activity
import android.app.Application
import android.content.pm.PackageInfo
import android.util.Log
import android.view.ViewGroup
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.helper.DuoCommandHelper
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.ui.VideoPlayActivity
import com.cloud.duolib.ui.WebViewActivity
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import com.cyjh.ddy.base.utils.CLog

object CloudBuilder {
    var mHost: String? = null

    private var mApp: Application? = null
    private val activities = ArrayList<BasePermissionActivity>()

    //权限申请时间
    private var PREFER_PERMISSION = "permission_apply"

    //布局样式类型
    private var uiStyle = 0
    private var isFree = true

    //wk传入应用地址
    private var appDopList: List<PackageInfo>? = null
    private val appHotSort = ArrayList<String>()

    //刷新验证
    private var tokenFreshCallBack: ((ResultNewTokenCallBack, Activity) -> Unit)? = null

    //激励展示
    private var showReward = false
    private var showRewardInterval: Long? = null
    private var showRewardCallBack: ((ResultRewardOkCallBack, Activity) -> Unit)? = null

    //信息流展示
    private var showGameUrl: String? = null
    private var showNativeCallBack: ((ViewGroup, Activity) -> Unit)? = null

    //退出弹窗监听
    private var quitSureCallBack: (() -> Unit)? = null

    //超时弹窗监听
    private var timeOutCallBack: (() -> Unit)? = null

    //开始使用监听
    private var startUseCallBack: ((Int?) -> Unit)? = null

    //跳转外部监听
    private var startGotoUrlCallBack: ((Activity, Int, String) -> Unit)? = null

    //刷新网页监听
    private var refreshWebCallBack: ((ResultRefreshCallBack?) -> Unit)? = null

    //友盟上报
    private var uMCallBack: ((String, String) -> Unit)? = null

    //是否打印日志
    private var showLog = false

    /**
     *  init model 开始
     *  1、初始化依赖库、SDK日志
     * */
    fun initCloud(app: Application, isShowLog: Boolean): CloudBuilder {
        this@CloudBuilder.mApp = app
        //    Utils.init(app)
        showLog = isShowLog
        //所有相关收集IMEI，MAC地址等全部放在用户点击同意后（Application里面也是要判断同意之后才初始化）
        //开放sdk打印日志
        CLog.mPrintLog = isShowLog
        CLog.mLogPriority = Log.VERBOSE
        //HTTPDNS要早于友盟初始化，否则accountid会错误，https://help.aliyun.com/knowledge_detail/58422.html?spm=a2c4g.11186623.4.4.7c674c07HGzLia  第7点
        //如果SDK UTDID冲突，看 https://help.aliyun.com/knowledge_detail/59152.html?spm=a2c4g.11186623.2.21.37756a4asgmQiu
        //第二参数：是否启用httpdns
        //DdyOrderHelper.getInstance().initHTTPDNS(app, false)
        return this
    }

    /**
     * 2、设置是否展示激励，传入执行后成功回调
     * （选择产商类型后开始调用）
     * */
    fun initShowRewardCallback(
        _show: Boolean,
        _showInterval: Long,
        _callback: (ResultRewardOkCallBack, Activity) -> Unit
    ): CloudBuilder {
        this@CloudBuilder.showReward = _show
        this@CloudBuilder.showRewardInterval = _showInterval
        this@CloudBuilder.showRewardCallBack = _callback
        return this
    }

    /**
     * 3、设置是否展示信息流，传入执行后成功回调
     * （开始排队界面展示时调用）
     * */
    fun initShowNativeCallback(
        _gameUrl: String,
        _callback: (ViewGroup, Activity) -> Unit
    ): CloudBuilder {
        this@CloudBuilder.showGameUrl = _gameUrl
        this@CloudBuilder.showNativeCallBack = _callback
        return this
    }

    /**
     * 4、刷新验证
     * （刷新token,并传入新值（排队及获取时返回602需要））
     * */
    fun initTokenRefreshCallback(_callback: (ResultNewTokenCallBack, Activity) -> Unit): CloudBuilder {
        this@CloudBuilder.tokenFreshCallBack = _callback
        return this
    }

    /**
     * 5、友盟上报信息
     * */
    fun initUmCallback(_callback: (String, String) -> Unit): CloudBuilder {
        this@CloudBuilder.uMCallBack = _callback
        return this
    }

    /**
     * 6、传入apk路径、传入在线参数包名排序列表
     * （非wk不必传）
     * */
    fun initApkInfo(listApk: List<PackageInfo>, listSort: List<String>?): CloudBuilder {
        this@CloudBuilder.appDopList = listApk
        if (listSort != null) {
            this@CloudBuilder.appHotSort.clear()
            this@CloudBuilder.appHotSort.addAll(listSort)
        }
        return this
    }

    /**
     * 7、传入ui界面类型（0lz、1wk、2yx)
     * */
    fun initUiStyle(style: Int): CloudBuilder {
        this@CloudBuilder.uiStyle = style
        return this
    }

    /**
     * 8、传入权限申请Preference时间
     * （非必传）
     * */
    fun initPermissionTimeKey(key: String): CloudBuilder {
        this@CloudBuilder.PREFER_PERMISSION = key
        return this
    }

    /**
     * 9、退出回调
     * （结束使用并释放机子后回调）
     * */
    fun initQuitCallback(_callback: () -> Unit): CloudBuilder {
        this@CloudBuilder.quitSureCallBack = _callback
        return this
    }

    /**
     * 9。1、退出回调
     * （超时自动结束回调，非yx一般不需要使用）
     * */
    fun initTimeOutCallback(_callback: () -> Unit): CloudBuilder {
        this@CloudBuilder.timeOutCallBack = _callback
        return this
    }

    /**
     * 10、开始使用回调
     * */
    fun initStartUseCallback(_callback: (Int?) -> Unit): CloudBuilder {
        this@CloudBuilder.startUseCallBack = _callback
        return this
    }

    /**
     * 11、配置是否释放
     * */
    fun initIsFreeToRecycle(free: Boolean): CloudBuilder {
        this@CloudBuilder.isFree = free
        return this
    }

    /**
     * 12、跳转外部回调
     * （非wk一般不需要使用）
     * */
    fun initRefreshWebCallback(_callback: (ResultRefreshCallBack?) -> Unit): CloudBuilder {
        this@CloudBuilder.refreshWebCallBack = _callback
        return this
    }

    /**
     * 13、跳转外部回调
     * （非yx一般不需要使用）
     * */
    fun initStartOutCallback(_callback: (Activity, Int, String) -> Unit): CloudBuilder {
        this@CloudBuilder.startGotoUrlCallBack = _callback
        return this
    }

    /**
     * 13、开始选择服务商
     * （跳转页面前先登录）
     * */
    fun startLibActivity(activity: Activity, data: InitCloudData, type: Int?, host: String) {
        mHost = host
        PreDataActivity.newInstance(activity, data, type, 0)
    }

    /**
     * 14、开始跳转使用媒体流
     * （非yx勿进）
     * */
    fun startPhoneActivity(
        activity: Activity, webUrl: String,
        dataTime: String, host: String
    ) {
        mHost = host
        WebViewActivity.newInstance(
            activity,
            time = dataTime,
            type = null,
            url = webUrl,
            data = null
        )
    }

    /**
     * 15、开始跳转使用网页流
     * （非yx勿进）
     * */
    fun startPhoneActivity(
        activity: Activity, data: InitCloudData,
        dataTime: String, extTime: Long?,
        key: String?, uid: String?, pid: String?, host: String
    ) {
        mHost = host
        if (key != null && uid != null && pid != null) {
            DuoCommandHelper().initOrderHelper(
                key,
                uid,
                pid,
                extTime,
                null,
                data,
                mResponse = object :
                    OnStrResponse {
                    override fun onSuccess(str: String) {
                        when {
                            str.contains("正常") -> {
                                DeviceMediaActivity.newInstance(
                                    activity,
                                    time = dataTime,
                                    type = null,
                                    key = key,
                                    uid = uid,
                                    pid = pid,
                                    limit = extTime,
                                    data = data
                                )
                            }
                            str.contains("重启") -> {
                                getToast(activity, str)
                            }
                            else -> {
                                data.let { initData ->
                                    CloudHttpUtils.getInstance()
                                        .getErrorReport(
                                            initData.app_pkg, initData.app_co,
                                            initData.app_token, initData.app_key, initData.app_iv,
                                            9, 9, str, pid, data.app_channel,
                                            android.os.Build.MODEL,
                                            android.os.Build.VERSION.RELEASE, dataTime
                                        ) { _, _ ->
                                            getToast(activity, "查询失败")
                                        }
                                }
                            }
                        }
                    }

                    override fun onFailed(str: String) {
                        getToast(activity, str)
                    }
                })
        } else {
            getToast(activity, "无初始化")
        }
    }

    /**
     * 16、开始跳转使用视频流
     * （非yx勿进）
     * */
    fun startPhoneActivity(
        activity: Activity, dataTime: String,
        content: String, rid: Int,
        data: InitCloudData, host: String
    ) {
        mHost = host
        VideoPlayActivity.newInstance(activity, dataTime, null, data, content, rid)
    }

    /**
     *  init model 结束
     * */

    fun getShowLog() = this@CloudBuilder.showLog

    fun getShowReward() = this@CloudBuilder.showReward

    fun getShowRewardInterval() = this@CloudBuilder.showRewardInterval

    fun getResultRewardCallBack(listener: ResultRewardOkCallBack, activity: Activity) =
        this@CloudBuilder.showRewardCallBack?.invoke(listener, activity)

    fun getTokenFreshCallback(listener: ResultNewTokenCallBack, activity: Activity) =
        this@CloudBuilder.tokenFreshCallBack?.invoke(listener, activity)

    fun getQuitSureCallback() = this@CloudBuilder.quitSureCallBack?.invoke()
    fun getTimeOutCallback() = this@CloudBuilder.timeOutCallBack?.invoke()
    fun getStartUseCallback(type: Int?) = this@CloudBuilder.startUseCallBack?.invoke(type)

    fun getNativeCallBack(viewGroup: ViewGroup, act: Activity) =
        this@CloudBuilder.showNativeCallBack?.invoke(viewGroup, act)

    fun getGameCallBack() = this@CloudBuilder.showGameUrl
    fun getStartOutCallback(act: Activity, type: Int, url: String) =
        this@CloudBuilder.startGotoUrlCallBack?.invoke(act, type, url)

    fun getRefreshCallBack(listener: ResultRefreshCallBack?) =
        this@CloudBuilder.refreshWebCallBack?.invoke(listener)

    //选择服务商、接口非602型错误、媒体流初始化失败
    fun getUMCallBack(key: String, msg: String) {
        try {
            this@CloudBuilder.uMCallBack?.invoke(key, "云机:$msg")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPermissionKey() = this@CloudBuilder.PREFER_PERMISSION
    fun getUiStyle() = this@CloudBuilder.uiStyle
    fun getIsFree() = this@CloudBuilder.isFree

    fun getDopSort() = this@CloudBuilder.appDopList
    fun getHotSort() = this@CloudBuilder.appHotSort

    fun getApp() = this@CloudBuilder.mApp

    fun add(_act: BasePermissionActivity) {
        activities.add(_act)
    }

    fun remove(_act: BasePermissionActivity) {
        activities.remove(_act)
    }

    fun finishAll() {
        for (item in activities) {
            if (!item.isFinishing) {
                item.finish()
            }
        }
    }

    fun getActMedia(): DeviceMediaActivity? {
        for (item in activities) {
            if (!item.isFinishing && (item is DeviceMediaActivity)) {
                return item
            }
        }
        return null
    }

    fun getActTop(): BasePermissionActivity? {
        if (activities.isNotEmpty()) {
            return activities.last()
        }
        return null
    }

    fun getActExist(clz: Class<out BasePermissionActivity?>): Boolean {
        for (act in activities) {
            if (act.javaClass == clz) {
                return true;
            }
        }
        return false;
    }
}