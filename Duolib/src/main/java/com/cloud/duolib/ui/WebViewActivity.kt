package com.cloud.duolib.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import androidx.core.view.isVisible
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.ResultRefreshCallBack
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.fre.CloudNodeData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.helper.MediaDialogHelper
import com.cloud.duolib.model.manager.WebViewCommonConfig
import com.cloud.duolib.model.util.getLongClick
import com.cloud.duolib.model.util.getToast
import kotlinx.android.synthetic.main.duo_activity_web_view.*

class WebViewActivity : com.cloud.duolib.base.BasePermissionActivity() {
    companion object {
        private const val WEB_VIEW_URL = "WEB_VIEW_URL"
        private const val WEB_VIEW_TIME = "WEB_VIEW_TIME"
        private const val WEB_VIEW_SERVER_TYPE = "WEB_VIEW_SERVER_TYPE"
        private const val WEB_VIEW_INIT_DATA = "WEB_VIEW_INIT_DATA"

        fun newInstance(
            act: Activity,
            time: String?,
            type: Int?,
            url: String,
            data: InitCloudData?
        ) {
            if (CloudBuilder.getActTop() is WebViewActivity) return
            Intent(act, WebViewActivity::class.java).apply {
                this.putExtra(WEB_VIEW_TIME, time)
                this.putExtra(WEB_VIEW_SERVER_TYPE, type)
                this.putExtra(WEB_VIEW_URL, url)
                this.putExtra(WEB_VIEW_INIT_DATA, data)
                if (CloudBuilder.getActExist(WebViewActivity::class.java)) {
                    this.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                act.startActivity(this)
                if (act is PreDataActivity) {
                    act.finish()
                }
            }
        }
    }

    private var mInitCloudData: InitCloudData? = null
    private var mWebViewConfig: WebViewCommonConfig? = null
    private var mType: Int? = null
    private var mRefreshTry = 0
    private var mRefreshAble = true

    override fun getLayoutId() = com.cloud.duolib.R.layout.duo_activity_web_view
    override fun initView(savedInstanceState: Bundle?) {
        //lv_web?.refreshUrl(intent.getStringExtra(WEB_VIEW_URL), this@WebViewActivity)
        intent.getStringExtra(WEB_VIEW_URL)?.let {
            if (mWebViewConfig == null) {
                mWebViewConfig = WebViewCommonConfig(null)
            }
            mWebViewConfig?.initWebView(llContext_web, it, this)
        }
        mInitCloudData = intent?.getParcelableExtra(WEB_VIEW_INIT_DATA)
        mType = intent?.getIntExtra(WEB_VIEW_SERVER_TYPE, 0)
        MediaDialogHelper().getInitFloatTime(
            this@WebViewActivity,
            intent?.getStringExtra(WEB_VIEW_TIME) ?: "60",
            mType, mInitCloudData
        )
        ivMore_web?.setOnClickListener {
            //5秒限制
            if ((pro_web?.isVisible == true) || !mRefreshAble) return@setOnClickListener
            getLongClick {
                getCloudApiData()
            }
        }
    }

    override fun initBeforeView() {

    }

    private fun getCloudApiData() {
        mInitCloudData?.let { initData ->
            if (mRefreshTry >= 5) {
                //超过5次提示
                getToast(this@WebViewActivity, R.string.click_quit_to)
                return@let
            } else if (mRefreshAble) {
                startRefresh(initData)
            }
        }
    }

    //重新获取
    private fun startRefresh(initData: InitCloudData) {
        mRefreshTry++
        pro_web?.isVisible = true
        if (CloudBuilder.getIsFree()) {
            CloudHttpUtils.getInstance().getNodeRx(
                this@WebViewActivity,
                initData.app_pkg,
                initData.app_co,
                initData.app_token,
                mType,
                initData.appn?.pkgName,
                initData.app_channel,
                initData.alert, 1, null,
                initData.app_key,
                initData.app_iv,
                object : CloudHttpUtils.NodeResponse {
                    override fun onSuccess(data: CloudNodeData?) {
                        pro_web?.isVisible = false
                        setCountDown(mRefreshTry == 0)
                        if (data != null) {
                            when (data.phoneCode) {
                                1 -> {
                                    //1表示获取到正常数据
                                    if (mWebViewConfig == null) {
                                        mWebViewConfig = WebViewCommonConfig(null)
                                    }
                                    data.url?.let { url ->
                                        mWebViewConfig?.refreshUrl(
                                            llContext_web,
                                            url, this@WebViewActivity
                                        )
                                        return
                                    }
                                }
                            }
                        }
                        getToast(this@WebViewActivity, "获取错误")
                    }

                    override fun onFailed(status: Int, msg: String) {
                        pro_web?.isVisible = false
                        setCountDown(mRefreshTry == 0)
                        getToast(this@WebViewActivity, "获取失败")
                    }
                })
        } else {
            CloudBuilder.getRefreshCallBack(object : ResultRefreshCallBack {
                override fun onSetNewUrl(url: String?) {
                    pro_web?.isVisible = false
                    setCountDown(mRefreshTry == 0)
                    if (!url.isNullOrEmpty()) {
                        if (mWebViewConfig == null) {
                            mWebViewConfig = WebViewCommonConfig(null)
                        }
                        mWebViewConfig?.refreshUrl(llContext_web, url, this@WebViewActivity)
                        return
                    }
                    getToast(this@WebViewActivity, "刷新失败")
                }
            });
        }
    }

    private val mCountDownTimer: CountDownTimer = object : CountDownTimer(6000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            mRefreshAble = false
            ivMore_web?.let {
                it.alpha = 0.4F
                it.text =
                    getString(R.string.click_refresh, (millisUntilFinished / 1000 % 60).toString())
            }
        }

        override fun onFinish() {
            mRefreshAble = true
            ivMore_web?.let {
                it.alpha = 0.8F
                it.text = getString(R.string.refresh)
            }
        }
    }

    private fun setCountDown(first: Boolean) {
        if (!first) {
            mCountDownTimer.cancel()
        }
        mCountDownTimer.start()
    }

    override fun onResume() {
        lv_web?.setResume()
        mWebViewConfig?.setResume()
        super.onResume()
    }

    override fun onPause() {
        lv_web?.setPause()
        mWebViewConfig?.setPause()
        super.onPause()
    }

    override fun onDestroy() {
        lv_web?.setDestroy()
        mWebViewConfig?.setDestroy()
        mWebViewConfig = null
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            //不执行父类点击事件
            return true
        }
        //继续执行父类其他点击事件
        return super.onKeyUp(keyCode, event)
    }

    override fun onBackPressed() {
        if (lv_web?.getBackPress() != true)
            super.onBackPressed()
    }
}