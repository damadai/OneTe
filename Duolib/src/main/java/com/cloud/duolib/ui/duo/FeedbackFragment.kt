package com.cloud.duolib.ui.duo

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.duo.DuoHelpData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.helper.ApiQuitHelper
import com.cloud.duolib.model.util.getAct
import com.cloud.duolib.model.util.getClick
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.view.adapter.CommonRecyclerAdapter
import com.cloud.duolib.view.getItemMargin
import kotlinx.android.synthetic.main.duo_fragment_help.*
import kotlinx.android.synthetic.main.duo_layout_retry.*
import kotlinx.android.synthetic.main.duo_layout_top.*

class FeedbackFragment : BaseFragment() {
    private var mApiQuitHelper: ApiQuitHelper? = null
    private var mPhone: String? = null
    private var mTime: String? = null
    private var mType: Int? = null
    private var mInitCloudData: InitCloudData? = null

    companion object {
        private const val FRA_FEED_PHONE = "FRA_FEED_PHONE"
        private const val FRA_FEED_TIME = "FRA_FEED_TIME"
        private const val FRA_SERVER_TYPE = "FRA_SERVER_TYPE"
        private const val FRA_INIT_DATA = "FRA_INIT_DATA"

        fun newInstance(
            phone: String?,
            time: String?,
            type: Int,
            initData: InitCloudData?
        ): Bundle {
            val bun = Bundle()
            bun.putString(FRA_FEED_PHONE, phone)
            bun.putString(FRA_FEED_TIME, time)
            bun.putInt(FRA_SERVER_TYPE, type)
            bun.putParcelable(FRA_INIT_DATA, initData)
            return bun
        }
    }

    override fun getLayoutId() = R.layout.duo_fragment_help
    override fun initView(saveInstanceState: Bundle?) {
        mPhone = arguments?.getString(FRA_FEED_PHONE)
        mTime = arguments?.getString(FRA_FEED_TIME)
        mType = arguments?.getInt(FRA_SERVER_TYPE)
        mInitCloudData = arguments?.getParcelable(FRA_INIT_DATA)
        tvTitleEnd_top?.isVisible = true
        tvTitle_top?.text = this@FeedbackFragment.context?.getString(R.string.help_feedback)
        tvTitleEnd_top?.setOnClickListener {
            getAct(this@FeedbackFragment) { act ->
                getViewLinkGroup(act)
            }
        }
        ivBack_top?.setOnClickListener {
            //隐藏fra
            getAct(this@FeedbackFragment) {
                (it as? DeviceMediaActivity)?.startHideFra(this@FeedbackFragment)
            }
        }
        tvRefresh_retry?.setOnClickListener {
            getClick() {
                initHelpData()
            }
        }
        initHelpData()
    }

    private fun initHelpData() {
        mInitCloudData?.let { iData ->
            CloudHttpUtils.getInstance()
                .getHelpRx(iData.app_pkg, iData.app_co, object : CloudHttpUtils.HelpResponse {
                    override fun onFailed(status: Int, str: String?) {
                        ilRetry_help?.isVisible = true
                    }

                    override fun onSuccess(data: ArrayList<DuoHelpData>?) {
                        if (!data.isNullOrEmpty()) {
                            ilRetry_help?.isVisible = false
                            initRecyclerview(data)
                        } else {
                            ilRetry_help?.isVisible = true
                        }
                    }
                })
        }
    }

    private fun initRecyclerview(list: ArrayList<DuoHelpData>) {
        rvQue_help?.adapter = CommonRecyclerAdapter.Builder()
            .setData(list).setLayout(R.layout.duo_item_help)
            .bindView { holder, _, position ->
                val data = list[position]
                holder.itemView.findViewById<TextView>(R.id.tvDescribe_help)?.isVisible = false
                holder.itemView.findViewById<TextView>(R.id.tvQue_help)?.text = data.question
                holder.itemView.findViewById<TextView>(R.id.tvFix_help)?.text = data.fix
                this@FeedbackFragment.context?.let { cxt ->
                    Glide.with(cxt)
                        .load(data.background)
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(20 /*圆角*/)))
                        .error(R.mipmap.ic_default_480)
                        .into(holder.itemView.findViewById(R.id.ivDes_help))
                }
                holder.itemView.findViewById<TextView>(R.id.tvFix_help)?.setOnClickListener {
                    getAct(this@FeedbackFragment) { act ->
                        startFix(data.fixType, data.question ?: position.toString(), data.type, act)
                    }
                }
            }.create()
        rvQue_help?.addItemDecoration(getItemMargin(12, 12))
    }

    private fun startFix(fix: Int?, info: String, type: Int?, act: Activity) {
        when (fix) {
            1 -> {
                (act as? DeviceMediaActivity)?.startResume(this@FeedbackFragment)
            }
            2 -> {
                getToast(this@FeedbackFragment, R.string.fix_prepare)
                (act as? DeviceMediaActivity)?.startReboot(this@FeedbackFragment)
            }
            3 -> {
                getToast(this@FeedbackFragment, R.string.wait_for_jump)
                mInitCloudData?.let { initData ->
                    getReportScreen(
                        initData.app_pkg,
                        initData.app_token,
                        initData.app_key,
                        initData.app_iv,
                        initData.app_co,
                        info, type ?: 8, fix,
                        mPhone ?: "777",
                        mTime ?: "777",
                        initData.app_channel
                    )
                }
            }
            5 -> {
                getToast(this@FeedbackFragment, R.string.fix_prepare)
                (act as? DeviceMediaActivity)?.startReset(this@FeedbackFragment)
            }
            else -> {
                getViewLinkGroup(act)
            }
        }
    }

    private fun getReportScreen(
        app_pkg: String,
        app_token: String,
        app_key: String,
        app_iv: String,
        app_co: Int,
        errorInfo: String,
        errorType: Int,
        fixType: Int,
        orderId: String,
        expire: String,
        channel: String
    ) {
        if (mApiQuitHelper == null) {
            mApiQuitHelper = ApiQuitHelper()
        }
        mApiQuitHelper?.apiReportError(
            app_pkg, app_token, app_key, app_iv, app_co,
            errorInfo, errorType, fixType,
            orderId, expire, channel
        ) { _, _ ->
            mApiQuitHelper?.apiRecoveryDevice(
                1,
                mType,
                mInitCloudData,
                object : CloudHttpUtils.StrResponse {
                    override fun onSuccess(str: String?) {
                        //重新排队
                        getAct(this@FeedbackFragment) { act ->
                            mInitCloudData?.let { PreDataActivity.newInstance(act, it, mType,0) }
                        }
                    }

                    override fun onFailed(status: Int, msg: String) {
                        //释放未成功
                        getToast(this@FeedbackFragment, R.string.fix_fail)
                    }
                })
        }
    }

    //弹窗确认（跳转客服
    private fun getViewLinkGroup(mAct: Activity) {
        (mAct as? DeviceMediaActivity)?.startShowGroupDialog(
            mInitCloudData?.qq_number,
            mInitCloudData?.qq_group
        )
    }
}