package com.cloud.duolib.ui.fre

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.ResultNewTokenCallBack
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.fre.CloudLineData
import com.cloud.duolib.bean.fre.CloudNodeData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.helper.DuoCommandHelper
import com.cloud.duolib.model.manager.WebViewCommonConfig
import com.cloud.duolib.model.util.*
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.ui.VideoPlayActivity
import com.cloud.duolib.ui.WebViewActivity
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import kotlinx.android.synthetic.main.duo_fragment_server_line.*
import kotlinx.android.synthetic.main.duo_view_tips.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timerTask

class LineFragment : BaseFragment() {
    private var lineOk = false
    private var firstClickClose = false
    var mCurType: Int? = null
    private var mRoom: Int? = null
    private var mInitCloudData: InitCloudData? = null
    private var mWebViewConfig: WebViewCommonConfig? = null

    override fun getLayoutId() = R.layout.duo_fragment_server_line

    override fun initView(saveInstanceState: Bundle?) {
        mCurType = arguments?.getInt(FRA_LINE_TYPE)
        mRoom = arguments?.getInt(FRA_ROOM_TYPE)
        mInitCloudData = arguments?.getParcelable(FRA_LINE_INIT_DATA)
        initViewStyle()
        tvYeah_line?.setOnClickListener {
            getClick {
                getAct(this@LineFragment) {
                    refreshLineOrGet()
                }
            }
        }
        tvQuit_line?.setOnClickListener {
            if (pb_line?.isVisible != true) {
                getAct(this@LineFragment) { act -> (act as? PreDataActivity)?.mAbs?.startQuitLine(mCurType)}
            }
        }
        tvOk_line?.setOnClickListener {
            tvYeah_line?.callOnClick()
        }
        tvTips_line?.setOnClickListener {
            if (pb_line?.isVisible != true) {
                tvTips_line?.isVisible = false
                firstClickClose = true
            }
        }
    }

    //初始化布局控件风格 0 lz,1 wk,2 yx
    private fun initViewStyle() {
        if (CloudBuilder.getUiStyle() == 2) {
            llLz_line?.isVisible = false
            llYx_line?.isVisible = true
        } else {
            llLz_line?.isVisible = true
            llYx_line?.isVisible = false
        }
        rlTop_line?.isVisible = false
        //初始化网页
        llContext_line?.let { ll ->
            CloudBuilder.getGameCallBack()?.let { url ->
                if (mWebViewConfig == null) {
                    mWebViewConfig = WebViewCommonConfig(pb_line)
                }
                mWebViewConfig?.refreshUrl(ll, url, this@LineFragment)
            }
        }
        handler = MyHandler(this@LineFragment)
        //第一次获取排队数据
        getLineApiData(true)
    }

    //点击刷新或获取
    fun refreshLineOrGet(auto: Boolean = false) {
        //排队或获取请求结束
        if (pb_line?.isVisible != true) {
            if (!lineOk) {
                //未成功才刷新排队
                getLineApiData(false)
            } else if (!auto) {
                //非自动刷新状态下，即人为点击才直接获取
                startNode()
            }
        }
    }

    private fun startNode() {
        val server = when (mCurType) {
            1 -> "chinac"
            2 -> "多多云（华为云鲲鹏云）"
            else -> "其它服务商$mCurType"
        }
        CloudBuilder.getUMCallBack(UM_DUO_SERVER_USE, server)
        getCloudApiData()
    }

    /**
     * 获取手机排队数据
     * */
    private fun getLineApiData(first: Boolean) {
        mInitCloudData?.let { initData ->
            pb_line?.isVisible = true
            CloudHttpUtils.getInstance()
                .getLineRx(
                    initData.app_pkg,
                    initData.app_co,
                    initData.app_token,
                    mCurType,
                    initData.app_key,
                    initData.app_iv,
                    object : CloudHttpUtils.LineResponse {
                        override fun onSuccess(data: CloudLineData?) {
                            pb_line?.isVisible = false
                            //排队进度部分
                            if (data != null) {
                                lineOk = data.num == 0
                                getCxt(this@LineFragment, { cxt ->
                                    refreshLines(
                                        cxt.getString(R.string.wait_num, data.num.toString()),
                                        cxt.getString(R.string.wait_time, data.time.toString()),
                                        data.tips
                                    )
                                }, null)
                                if (lineOk && !firstClickClose) {
                                    showLineOkTips()
                                }
                                if (lineOk) {
                                    //直接获取并使用
                                    startNode()
                                }
                            } else {
                                getCxt(this@LineFragment, { cxt ->
                                    refreshLines("", cxt.getString(R.string.wait_ready), null)
                                }, null)
                            }
                            //刷新按钮部分
                            refreshInfo(first)
                            if (first) {
                                initNativeAndTimer(lineOk, null)
                            }
                        }

                        override fun onFailed(status: Int, msg: String) {
                            //刷新按钮部分
                            showFailRefresh("排队", status, msg, first)
                            //刷新网页信息流部分
                            if (first) {
                                initNativeAndTimer(lineOk, status)
                            }
                        }
                    })
        }
    }

    /**
     * 获取云手机
     * */
    private fun getCloudApiData() {
        mInitCloudData?.let { initData ->
            pb_line?.isVisible = true
            getCxt(this@LineFragment, { cxt ->
                CloudHttpUtils.getInstance().getNodeRx(
                    cxt,
                    initData.app_pkg,
                    initData.app_co,
                    initData.app_token,
                    mCurType,
                    initData.appn?.pkgName,
                    initData.app_channel,
                    initData.alert, 0, mRoom,
                    initData.app_key,
                    initData.app_iv,
                    object : CloudHttpUtils.NodeResponse {
                        override fun onSuccess(data: CloudNodeData?) {
                            if (data != null) {
                                when (data.phoneCode) {
                                    0 -> {
                                        pb_line?.isVisible = false
                                        //0表示错误重新排队
                                        lineOk = false
                                        refreshInfo(false)
                                    }
                                    1 -> {
                                        //1表示获取到正常数据
                                        lineOk = true
                                        //准备跳转
                                        getAct(this@LineFragment) { act ->
                                            if (!data.url.isNullOrEmpty()) {
                                                //跳转华云
                                                getStartWeb(act, mCurType, data)
                                            } else if (data.key != null && data.OpenID != null) {
                                                //跳转多多云
                                                getStartMedia(data, mCurType, initData)
                                            } else if (!data.connect.isNullOrEmpty()) {
                                                //百度云
                                                getStartVideo(act, mCurType, data)
                                            } else {
                                                getToast(act, "无云址")
                                                pb_line?.isVisible = false
                                            }
                                        }
                                    }
                                    else -> {
                                        pb_line?.isVisible = false
                                        getToast(this@LineFragment, "无队列")
                                    }
                                }
                            } else {
                                pb_line?.isVisible = false
                                getToast(this@LineFragment, "无队列")
                            }
                        }

                        override fun onFailed(status: Int, msg: String) {
                            showFailRefresh("获取云机", status, msg, false)
                        }
                    })

            }, null)
        }
    }

    //显示错误或继续的刷新显示文字
    private fun showFailRefresh(
        str: String,
        status: Int,
        msg: String?,
        first: Boolean
    ) {
        pb_line?.isVisible = false
        lineOk = false
        getAct(this@LineFragment) { act ->
            when (status) {
                602 -> {
                    msg?.let { getToast(act, it) }
                    CloudBuilder.getTokenFreshCallback(object : ResultNewTokenCallBack {
                        override fun onSetNewToken(newToken: String) {
                            (act as? PreDataActivity)?.mAbs?.startSetNewToken(newToken)
                        }
                    }, act)
                }
                701, 703, 705 -> {
                    getToast(this@LineFragment, "$str $msg")
                    refreshLines(msg ?: "", act.getString(R.string.wait_ready), null)
                    refreshInfo(first)
                }
                else -> {
                    getToast(this@LineFragment, "$str$status")
                    refreshLines(
                        act.getString(R.string.refresh_gain),
                        act.getString(R.string.wait_ready),
                        null
                    )
                    refreshInfo(first)
                }
            }
        }
    }

    private fun refreshLines(num: String, time: String, tips: String?) {
        tvNum_line?.text = num
        tvTime_line?.text = time
        if (tips.isNullOrEmpty()) {
            rlTop_line?.isVisible = false
        } else {
            rlTop_line?.isVisible = true
            tvTop_tips?.let { tvMarqueeTip ->
                tvMarqueeTip.text = tips
                tvMarqueeTip.ellipsize = TextUtils.TruncateAt.MARQUEE
                tvMarqueeTip.isSingleLine = true
                tvMarqueeTip.isSelected = true
                tvMarqueeTip.isFocusable = true
                tvMarqueeTip.isFocusableInTouchMode = true
            }
        }
    }

    //刷新立即获取按钮显示文字
    private fun refreshInfo(first: Boolean) {
        getCxt(this@LineFragment, { cxt ->
            val info =
                if (lineOk) cxt.getString(R.string.getCloud) else cxt.getString(R.string.refresh)
            tvYeah_line?.text = info
            tvOk_line?.text = info
        }, null)
        if (first) {
            cmTime_line?.base = SystemClock.elapsedRealtime()
            cmTime_line?.start()
        }
    }

    //刷新排队成功提示框出现
    private fun showLineOkTips() {
        //初始化颜色提示
        this@LineFragment.context?.getString(R.string.lineTip)?.let { hintText ->
            val stringBuilder = SpannableStringBuilder(hintText)
            stringBuilder.setSpan(
                ForegroundColorSpan(Color.RED),
                hintText.indexOf("三"),
                hintText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvTips_line?.text = stringBuilder
        }
        tvTips_line?.isVisible = true
    }

    //首次排队成功与否判断
    private fun initNativeAndTimer(showAd: Boolean, delay: Int?) {
        //刷新网页信息流部分显示ad/game
        if (showAd) {
            llContext_line?.removeAllViews()
            getAct(this@LineFragment) {
                CloudBuilder.getNativeCallBack(llContext_line, it)
            }
        }
        //首次获取排队情况即开始自动任务
        isRunning = false
        startTimer(if (delay == 705) 32000 else 9000)//频繁请求则期限后开刷
    }

    override fun onResume() {
        if (mWebViewConfig == null) {
            mWebViewConfig = WebViewCommonConfig(pb_line)
        }
        mWebViewConfig?.setResume()
        super.onResume()
    }

    override fun onPause() {
        if (mWebViewConfig == null) {
            mWebViewConfig = WebViewCommonConfig(pb_line)
        }
        mWebViewConfig?.setPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopTimer()
        if (mWebViewConfig == null) {
            mWebViewConfig = WebViewCommonConfig(pb_line)
        }
        mWebViewConfig?.setDestroy()
        mWebViewConfig = null
        super.onDestroy()
    }

    private var sTask: TimerTask? = null
    private var sTimer: Timer? = null
    private var isRunning = true
    private var handler: Handler? = null

    private fun startTimer(delay: Long) {
        if (!isRunning && sTimer == null && sTask == null && handler != null) {
            sTimer = Timer()
            sTask = timerTask {
                run {
                    handler?.removeMessages(TIME_UPDATE)
                    handler?.sendEmptyMessage(TIME_UPDATE)
                }
            }
            sTimer?.schedule(sTask, delay, 45000) //9秒后开始启动，间隔45s循环执行
            isRunning = true
        }
    }

    private fun stopTimer() {
        if (isRunning && sTimer != null && sTask != null) {
            sTimer?.cancel()
            sTimer = null
            sTask?.cancel()
            sTask = null
            isRunning = false
            handler?.removeMessages(TIME_UPDATE)
            handler = null
        }
    }

    private class MyHandler(oneService: Fragment) : Handler() {
        private val reference = WeakReference(oneService)
        override fun handleMessage(msg: Message?) {
            val timesService = reference.get() ?: return
            when (msg?.what) {
                TIME_UPDATE -> {
                    (timesService as? LineFragment)?.refreshLineOrGet(true)
                }
            }
        }
    }

    companion object {
        private const val TIME_UPDATE = 0x123
        const val FRA_LINE_TYPE = "FRA_LINE_TYPE"
        const val FRA_ROOM_TYPE = "FRA_ROOM_TYPE"
        const val FRA_LINE_INIT_DATA = "FRA_LINE_INIT_DATA"
    }

    //跳网页
    private fun getStartWeb(act: Activity, type: Int?, data: CloudNodeData) {
        (act as? PreDataActivity)?.startNextAct {
            CloudBuilder.getStartUseCallback(type)
            WebViewActivity.newInstance(
                act,
                time = data.expire,
                type = type,
                url = data.url!!,
                data = mInitCloudData
            )
        }
    }

    //跳媒体
    private fun getStartMedia(data: CloudNodeData, type: Int?, initData: InitCloudData) {
        DuoCommandHelper().initOrderHelper(
            data.key,
            data.OpenID,
            data.phoneID, data.expireTimestamp, type, mInitCloudData,
            object : OnStrResponse {
                override fun onSuccess(str: String) {
                    getAct(this@LineFragment) { act ->
                        if (str.contains("正常")) {
                            (act as? PreDataActivity)?.startNextAct {
                                CloudBuilder.getStartUseCallback(type)
                                DeviceMediaActivity.newInstance(
                                    act,
                                    time = data.expire,
                                    type = type,
                                    key = data.key!!,
                                    uid = data.OpenID!!,
                                    pid = data.phoneID,
                                    limit = data.expireTimestamp,
                                    data = mInitCloudData
                                )
                            }
                        } else {
                            pb_line?.isVisible = false
                            (act as? PreDataActivity)?.getErrorDialog(
                                initData.app_pkg,
                                initData.app_token,
                                initData.app_key,
                                initData.app_iv,
                                initData.app_co,
                                str,
                                data.phoneID,
                                data.expire,
                                initData.app_channel,type,
                                object :
                                    CloudHttpUtils.StrResponse {
                                    override fun onFailed(
                                        status: Int,
                                        str: String?
                                    ) {
                                        //设备错误重新排队
                                        lineOk = false
                                        refreshInfo(false)
                                    }

                                    override fun onSuccess(
                                        str: String?
                                    ) {
                                        //设备错误重新排队
                                        lineOk = false
                                        refreshInfo(false)
                                    }
                                }
                            )
                        }
                    }
                }

                override fun onFailed(str: String) {
                    pb_line?.isVisible = false
                }
            })
    }

    //跳视频
    private fun getStartVideo(act: Activity, type: Int?, data: CloudNodeData) {
        (act as? PreDataActivity)?.startNextAct {
            CloudBuilder.getStartUseCallback(type)
            VideoPlayActivity.newInstance(
                act,
                data.expire,
                type,
                mInitCloudData,
                data.connect,
                mRoom
            )
        }
    }
}