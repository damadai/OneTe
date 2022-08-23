package com.cloud.duolib.ui.fre

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.fre.*
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.http.NetUtil
import com.cloud.duolib.model.abs.ServerAbs
import com.cloud.duolib.model.util.*
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.view.CornerLabelView
import com.cloud.duolib.view.adapter.CommonRecyclerAdapter
import com.cloud.duolib.view.getItemMargin
import kotlinx.android.synthetic.main.duo_fragment_server.*
import kotlinx.android.synthetic.main.duo_layout_retry.*

class ServerFragment : BaseFragment() {
    //当前厂商编号
    var mCurType: Int? = null

    //当前厂商名称
    private var mCurName = ""

    //列表适配器
    private var mAdapter: CommonRecyclerAdapter? = null
    private var oldList: CloudListNewData? = null
    private var newList = ArrayList<CloudListData>()

    private var mServerAbs: ServerAbs? = null

    companion object {
        const val FRA_SERVER_INIT_DATA = "FRA_SERVER_INIT_DATA"
        const val FRA_SERVER_TYPE = "FRA_SERVER_TYPE"

        //自动到机房界面
        const val FRA_SERVER_HOST_AUTO = "FRA_SERVER_HOST_AUTO"
    }

    override fun getLayoutId() = R.layout.duo_fragment_server
    override fun initView(saveInstanceState: Bundle?) {
        mCurType = arguments?.getInt(FRA_SERVER_TYPE)
        logShow(info = "ServerFragment mCurType $mCurType")
        initAbs(arguments?.getParcelable(FRA_SERVER_INIT_DATA))

        //监听
        tvPay_server?.isVisible = false
        tvPay_server?.setOnClickListener {
            if (pb_server?.isVisible == true) return@setOnClickListener
            getClick {
                getAct(this@ServerFragment) { act ->
                    //跳转外部
                    if (oldList?.marketTypeList?.contains(mCurType) == true) {
                        CloudBuilder.getStartOutCallback(
                            act,
                            mCurType ?: 0,
                            oldList?.marketUrl ?: ""
                        )
                        return@getAct
                    }
                    //跳转本机
                    mServerAbs?.startNextFra(act, false)
                }
            }
        }
        tvRefresh_retry?.setOnClickListener {
            getClick {
                mServerAbs?.startRefreshView(false)
            }
        }

        //刷新
        mServerAbs?.startRefreshView(arguments?.getBoolean(FRA_SERVER_HOST_AUTO, false) == true)
        getAct(this@ServerFragment) {
            (it as PreDataActivity).mAbs?.startSetTitle(getString(R.string.view_create_cloud))
        }
    }

    private fun initAbs(initCloudData: InitCloudData?): ServerAbs? {
        if (mServerAbs == null) {
            mServerAbs = object : ServerAbs() {
                override fun getApiRequestData(): InitCloudData? {
                    return initCloudData
                }

                //获取列表数据
                override fun startRefreshView(autoNext: Boolean) {
                    if (NetUtil.hasInternet()) {
                        rv_server?.visibility = View.VISIBLE
                        pb_server?.isVisible = true
                        apiRequestData?.let { iData ->
                            if (CloudBuilder.getUiStyle() != 2) {
                                CloudHttpUtils.getInstance().getNewPriceRx(
                                    iData.app_pkg,
                                    iData.app_co,
                                    iData.appn?.pkgName,
                                    object : CloudHttpUtils.ListNewResponse {
                                        override fun onFailed(status: Int, msg: String) {
                                            pb_server?.isVisible = false
                                            ilRetry_server?.isVisible = true
                                            getToast(
                                                this@ServerFragment,
                                                "$FAIL_CLOUD_GET_DEVICE_LIST$status"
                                            )
                                        }

                                        override fun onSuccess(data: CloudListNewData?) {
                                            pb_server?.isVisible = false
                                            ilRetry_server?.isVisible = false
                                            if ((data != null) && data.server.isNotEmpty()) {
                                                tvPay_server?.isVisible = true
                                                //初始化列表
                                                initNewRecyclerView(data, autoNext)
                                            } else {
                                                getToast(this@ServerFragment, "无厂商")
                                            }
                                        }
                                    })
                            } else {
                                //todo delete old type unSelectRefresh
                                CloudHttpUtils.getInstance().getPriceRx(
                                    iData.app_pkg,
                                    iData.app_co,
                                    iData.appn?.pkgName,
                                    object : CloudHttpUtils.ListResponse {
                                        override fun onFailed(status: Int, msg: String) {
                                            pb_server?.isVisible = false
                                            ilRetry_server?.isVisible = true
                                            getToast(
                                                this@ServerFragment,
                                                "$FAIL_CLOUD_GET_DEVICE_LIST$status"
                                            )
                                        }

                                        override fun onSuccess(data: ArrayList<CloudListData>?) {
                                            pb_server?.isVisible = false
                                            ilRetry_server?.isVisible = false
                                            if (!data.isNullOrEmpty()) {
                                                tvPay_server?.isVisible = true
                                                //初始化列表
                                                initRecyclerView(data)
                                            } else {
                                                getToast(this@ServerFragment, "无厂商")
                                            }
                                        }
                                    })
                            }
                        }
                    } else {
                        ilRetry_server?.isVisible = true
                    }
                }

                override fun startNextFra(activity: Activity, autoNext: Boolean) {
                    (activity as PreDataActivity).mAbs?.let { abs ->
                        if (mServerAbs?.startCheckGotoHost() == true) {
                            abs.startFraHost(mCurType, mCurName, payBtnInfo, autoNext)
                            return
                        } else if (!abs.startCheckShowReward()) {
                            abs.startFraLine(mCurType, mCurName, null)
                            return
                        } else if (autoNext) {
                            //外部跳转至需要激励，自动则止步于此
                            return
                        }
                        mCountDownTimer.cancel()
                        pb_server?.isVisible = true
                        //开始展示激励
                        abs.startShowReward(
                            true, mServerAbs?.startCheckGotoHost() == true,
                            mCurType, mCurName, null,
                            payBtnInfo
                        )
                    }
                }

                //倒计时
                override fun startCountDown(cancel: Boolean, start: Boolean) {
                    tvPay_server?.text = payBtnInfo
                    if (cancel) {
                        mCountDownTimer.cancel()
                    }
                    if (start) {
                        mCountDownTimer.start()
                    }
                }

                //选中
                override fun startSelectType(selectType: Int?, first: Boolean, autoNext: Boolean) {
                    logShow(info = "ServerFragment startSelectType mCurType $mCurType $selectType")
                    mCurType = selectType
                    //选中按钮
                    if (oldList != null && !oldList?.server.isNullOrEmpty()) {
                        oldList?.server?.forEach { server ->
                            if (server.types == mCurType) {
                                tvDetail_server?.text = server.content
                                mCurName = server.name
                                server.defaultSelect = true
                                mServerAbs?.startCountDown(!first, server.openServer == 0)
                            } else {
                                server.defaultSelect = false
                            }
                        }
                        mAdapter?.notifyItemRangeChanged(0, oldList?.server?.size ?: 0)
                    } else if (newList.isNotEmpty()) {
                        for (pos in 0 until newList.size) {
                            if (newList[pos].types == mCurType) {
                                newList[pos].title?.let {
                                    mCurName = it
                                }
                                newList[pos].defaultSelect = true
                                mServerAbs?.startCountDown(!first, false)
                            } else {
                                newList[pos].defaultSelect = false
                            }
                        }
                        mAdapter?.notifyItemRangeChanged(0, newList.size)
                    }
                    if (autoNext) {
                        getAct(this@ServerFragment) { act ->
                            startNextFra(act, true)
                        }
                    }
                }

                override fun getPayBtnInfo(): String {
                    return if ((oldList?.marketTypeList?.contains(mCurType) == true) && !oldList?.marketBtn.isNullOrEmpty()) {
                        oldList?.marketBtn!!
                    } else {
                        oldList?.btn ?: ""
                    }
                }

                override fun startCheckGotoHost(): Boolean {
                    oldList?.server?.forEach { server ->
                        if (server.types == mCurType) {
                            return server.openServer != 0
                        }
                    }
                    return false
                }
            }
        }
        return mServerAbs
    }

    //初始化新列表
    private fun initNewRecyclerView(dataList: CloudListNewData, autoNext: Boolean) {
        oldList = dataList
        //产商
        mAdapter = CommonRecyclerAdapter.Builder()
            .setData(oldList!!.server).setLayout(R.layout.duo_item_specify)
            .bindView { holder, _, position ->
                val data = oldList!!.server[position]
                holder.itemView.findViewById<TextView>(R.id.tvTitle_item)?.text = data.name
                holder.itemView.findViewById<TextView>(R.id.tvList_item)?.isVisible = false
                holder.itemView.findViewById<ImageView>(R.id.ivSelect_item)?.visibility =
                    if (data.defaultSelect) View.VISIBLE else View.INVISIBLE
                //进入按钮
                holder.itemView.setOnClickListener {
                    //详情
                    if (mCurType != data.types) {
                        mServerAbs?.startSelectType(data.types, false, false)
                    }
                }
            }.create()
        rv_server?.adapter = mAdapter
        //规格
        if (!dataList.price.isNullOrEmpty()) {
            rvDetail_server?.adapter = CommonRecyclerAdapter.Builder()
                .setData(dataList.price).setLayout(R.layout.duo_item_specify)
                .bindView { holder2, _, position ->
                    val data = dataList.price[position]
                    holder2.itemView.findViewById<TextView>(R.id.tvTitle_item)?.text = data.name
                    holder2.itemView.findViewById<TextView>(R.id.tvList_item)?.text = data.value
                }.create()
        } else {
            tvTitle_server?.isVisible = false
        }
        //初始化选中
        if ((mCurType != null) && (mCurType != -1)) {
            mServerAbs?.startSelectType(mCurType, true, autoNext)
        } else {
            mServerAbs?.startSelectType(dataList.server.first().types, true, autoNext)
        }
    }

    //初始化镜像列表
    private fun initRecyclerView(dataList: ArrayList<CloudListData>) {
        newList = dataList
        //隐藏其它
        tvDetail_server?.isVisible = false
        tvTitle_server?.isVisible = false
        this@ServerFragment.context?.let { cxt ->
            tvType_server?.setBackgroundColor(ContextCompat.getColor(cxt, R.color.gray6))
        }
        //列表适配
        mAdapter = CommonRecyclerAdapter.Builder()
            .setData(newList).setLayout(R.layout.duo_item_server)
            .bindView { holder, _, position ->
                val data = newList[position]
                holder.itemView.findViewById<CornerLabelView>(R.id.flIcon_item).isVisible =
                    data.types == 2
                holder.itemView.findViewById<TextView>(R.id.tvServerName_item)?.text = data.title
                holder.itemView.findViewById<ImageView>(R.id.ivSelect_item).let { iv ->
                    iv.isVisible = data.defaultSelect
                    if (!data.logo.isNullOrEmpty()) {
                        this@ServerFragment.context?.let { cxt ->
                            iv.isVisible = true
                            Glide.with(cxt)
                                .load(data.logo)
                                .apply(RequestOptions.bitmapTransform(RoundedCorners(20 /*圆角*/)))
                                .into(iv)
                        }
                    } else {
                        iv.isVisible = false
                    }
                }
                //详情
                holder.itemView.findViewById<TextView>(R.id.tvDetail_text)?.let { mTv ->
                    val msg = "时长：${data.duration}分钟\n详情介绍：${data.content}"
                    mTv.text = if (data.price == 0) {
                        "免费\n$msg"
                    } else {
                        "${data.price}\n" + msg
                    }
                }
                //进入按钮
                holder.itemView.setOnClickListener {
                    if (mCurType != data.types) {
                        mServerAbs?.startSelectType(data.types, false, false)
                    }
                }
            }.create()
        rv_server?.adapter = mAdapter
        rv_server?.addItemDecoration(getItemMargin(12, 0))
        //初始化选中
        if ((mCurType != null) && (mCurType != -1)) {
            mServerAbs?.startSelectType(mCurType, true, false)
        } else {
            mServerAbs?.startSelectType(dataList.first().types, true, false)
        }
    }

    private val mCountDownTimer: CountDownTimer = object : CountDownTimer(6000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val sec = millisUntilFinished / 1000 % 60
            tvPay_server?.text = "${mServerAbs?.payBtnInfo}（" + sec + "s）"
        }

        override fun onFinish() {
            tvPay_server?.callOnClick()
        }
    }

    override fun onPause() {
        super.onPause()
        mCountDownTimer.cancel()
        tvPay_server?.text = mServerAbs?.payBtnInfo
    }
}