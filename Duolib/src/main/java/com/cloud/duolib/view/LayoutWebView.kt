package com.cloud.duolib.view

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.cloud.duolib.base.MiddlewareChromeClient
import com.cloud.duolib.base.MiddlewareWebViewClient
import com.just.agentweb.*
import kotlinx.android.synthetic.main.duo_view_web.view.*

class LayoutWebView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
            this(context, attrs, defStyleAttr, 0)

    private var canOpenWeb = false
    private var mAgentWeb: AgentWeb? = null
    private var mMiddleWareWebClient: MiddlewareWebClientBase? = null
    private var mMiddleWareWebChrome: MiddlewareWebChromeBase? = null

    init {
        LayoutInflater.from(context).inflate(com.cloud.duolib.R.layout.duo_view_web, this, false)
    }

    private fun initWebView(url: String?, act: Activity) {
        if (!url.isNullOrEmpty()){
            mAgentWeb = AgentWeb.with(act)
                .setAgentWebParent(
                    fl_web,
                    -1,
                    LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                ) //传入AgentWeb的父控件。
                .closeIndicator()
                .setAgentWebWebSettings(getSettings()) //设置 IAgentWebSettings。
                .setWebChromeClient(mWebChromeClient)
                .setWebViewClient(mWebViewClient) //WebViewClient ， 与 WebView 使用一致 ，但是请勿获取WebView调用setWebViewClient(xx)方法了,会覆盖AgentWeb DefaultWebClient,同时相应的中间件也会失效。
                .setPermissionInterceptor(mPermissionInterceptor) //权限拦截 2.0.0 加入。
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK) //严格模式 Android 4.2.2 以下会放弃注入对象 ，使用AgentWebView没影响。
                .useMiddlewareWebChrome(getMiddlewareWebChrome()) //设置WebChromeClient中间件，支持多个WebChromeClient，AgentWeb 3.0.0 加入。
                .useMiddlewareWebClient(getMiddlewareWebClient()) //设置WebViewClient中间件，支持多个WebViewClient， AgentWeb 3.0.0 加入。
                .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.ASK) //打开其他页面时，弹窗质询用户前往其他应用 AgentWeb 3.0.0 加入。
                .interceptUnkownUrl() //拦截找不到相关页面的Url AgentWeb 3.0.0 加入。
                .createAgentWeb() //创建AgentWeb。
                .ready() //设置 WebSettings。
                .go(url) //WebView载入该url地址的页面并显示。
        }
        mAgentWeb?.let { aWeb ->
            aWeb.webCreator.webView.setLayerType(View.LAYER_TYPE_NONE, null)
            aWeb.webCreator.webView.settings.mediaPlaybackRequiresUserGesture = false
            aWeb.agentWebSettings.webSettings.useWideViewPort = true //将图片调整到适合webview的大小
            aWeb.agentWebSettings.webSettings.loadWithOverviewMode = true
            aWeb.webCreator.webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        }
    }

    fun refreshUrl(url: String?, act: Activity) {
        if (mAgentWeb == null) {
            initWebView(url, act)
        } else {
            mAgentWeb?.urlLoader?.loadUrl(url)
        }
    }

    fun setPause() {
        this.mAgentWeb?.webLifeCycle?.onPause()
    }

    fun setResume() {
        this.mAgentWeb?.webLifeCycle?.onResume()
    }

    fun setDestroy() {
        this.mAgentWeb?.webLifeCycle?.onDestroy()
        this.mAgentWeb = null
    }

    fun getBackPress() = mAgentWeb?.back() == true

    /**
     * @return IAgentWebSettings
     */
    private fun getSettings(): IAgentWebSettings<*>? {
        return object : AbsAgentWebSettings() {
            private var sAgentWeb: AgentWeb? = null
            override fun bindAgentWebSupport(agentWeb: AgentWeb) {
                this.sAgentWeb = agentWeb
            }
        }
    }

    private var mWebChromeClient: com.just.agentweb.WebChromeClient =
        object : com.just.agentweb.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {}

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                super.onReceivedIcon(view, icon)
            }
        }

    /**
     * 注意，重写WebViewClient的方法,super.xxx()请务必正确调用， 如果没有调用super.xxx(),则无法执行DefaultWebClient的方法
     * 可能会影响到AgentWeb自带提供的功能,尽可能调用super.xxx()来完成洋葱模型
     */
    private var mWebViewClient: com.just.agentweb.WebViewClient? =
        object : com.just.agentweb.WebViewClient() {
            private val timer = HashMap<String, Long>()
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val requestUrl = request.url.toString()
                return if (!canOpenWeb) {
                    super.shouldOverrideUrlLoading(view, request)
                } else {
                    mAgentWeb?.urlLoader?.loadUrl(requestUrl)
                    true
                }
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                //优酷想唤起自己应用播放该视频 ， 下面拦截地址返回 true  则会在应用内 H5 播放 ，禁止优酷唤起播放该视频， 如果返回 false ， DefaultWebClient  会根据intent 协议处理 该地址 ， 首先匹配该应用存不存在 ，如果存在 ， 唤起该应用播放 ， 如果不存在 ， 则跳到应用市场下载该应用 .
                if (url.startsWith("intent://") && url.contains("com.youku.phone")) {
                    return true
                }
                return if (!canOpenWeb) {
                    super.shouldOverrideUrlLoading(view, url)
                } else {
                    mAgentWeb?.urlLoader?.loadUrl(url)
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) {
                    timer[url] = System.currentTimeMillis()
                }
                super.onPageStarted(view, url, favicon)
                pb_web?.isVisible = true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                pb_web?.isVisible = false
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                super.onReceivedSslError(view, handler, error)
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
            }
        }

    /**
     * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
     * @param url
     * @param permissions
     * @param action
     * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
     */
    private var mPermissionInterceptor =
        PermissionInterceptor { url, permissions, action ->
            false
        }

    private fun getMiddlewareWebChrome(): MiddlewareWebChromeBase {
        return object : MiddlewareChromeClient() {}.also { mMiddleWareWebChrome = it }
    }

    /**
     * MiddlewareWebClientBase 是 AgentWeb 3.0.0 提供一个强大的功能，
     * 如果用户需要使用 AgentWeb 提供的功能， 不想重写 WebClientView方
     * 法覆盖AgentWeb提供的功能，那么 MiddlewareWebClientBase 是一个
     * 不错的选择 。
     *
     * @return
     */
    private fun getMiddlewareWebClient(): MiddlewareWebClientBase {
        return object : MiddlewareWebViewClient() {
            /**
             *
             * @param view
             * @param url
             * @return
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                if (url.startsWith("agentweb")) { // 拦截 url，不执行 DefaultWebClient#shouldOverrideUrlLoading
                    return true
                }
                return super.shouldOverrideUrlLoading(
                    view,
                    url
                )
                // do you work
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
            }
        }.also { mMiddleWareWebClient = it }
    }
}