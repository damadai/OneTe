package com.cloud.duolib.ui.fre

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.fre.CloudHostData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.http.NetUtil
import com.cloud.duolib.model.abs.HostAbs
import com.cloud.duolib.model.util.FAIL_CLOUD_GET_DEVICE_LIST
import com.cloud.duolib.model.util.getAct
import com.cloud.duolib.model.util.getClick
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.ui.PreDataActivity
import com.cloud.duolib.view.adapter.CommonRecyclerAdapter
import kotlinx.android.synthetic.main.duo_fragment_host.*
import kotlinx.android.synthetic.main.duo_layout_retry.*

class HostFragment : BaseFragment() {
    //当前厂商编号
    private var mCurRoom: Int? = null

    var mHostAbs: HostAbs? = null

    //列表适配器
    private var mAdapter: CommonRecyclerAdapter? = null
    private var oldList: ArrayList<CloudHostData>? = null

    companion object {
        const val FRA_HOST_TYPE = "FRA_HOST_TYPE"
        const val FRA_HOST_NAME = "FRA_HOST_NAME"
        const val FRA_HOST_BTN_INFO = "FRA_HOST_BTN_INFO"
        const val FRA_HOST_INIT_DATA = "FRA_HOST_INIT_DATA"
    }

    override fun getLayoutId() = R.layout.duo_fragment_host
    override fun initView(saveInstanceState: Bundle?) {
        initAbs(
            arguments?.getParcelable(FRA_HOST_INIT_DATA), arguments?.getInt(FRA_HOST_TYPE),
            arguments?.getString(FRA_HOST_NAME), arguments?.getString(FRA_HOST_BTN_INFO)
        )

        tvPay_host?.isVisible = false
        //监听
        tvPay_host?.setOnClickListener {
            if (pb_host?.isVisible == true) return@setOnClickListener
            getAct(this@HostFragment) { act ->
                mHostAbs?.startNextFra(act, false)
            }
        }
        tvRefresh_retry?.setOnClickListener {
            getClick {
                mHostAbs?.startRefreshView(false)
            }
        }

        //刷新
        mHostAbs?.btn?.let {
            tvPay_host?.text = it
        }
        getAct(this@HostFragment) {
            (it as PreDataActivity).mAbs?.startSetTitle(mHostAbs?.name)
        }

        mHostAbs?.startRefreshView(false)
    }

    private fun initAbs(initCloudData: InitCloudData?, type: Int?, name: String?, btn: String?) {
        mHostAbs = object : HostAbs() {
            override fun getApiRequestData(): InitCloudData? {
                return initCloudData
            }

            //获取列表数据
            override fun startRefreshView(autoNext: Boolean) {
                if (NetUtil.hasInternet()) {
                    apiRequestData?.let { iData ->
                        pb_host?.isVisible = true
                        CloudHttpUtils.getInstance().getNewHostRx(
                            iData.app_pkg,
                            iData.app_co,
                            object : CloudHttpUtils.HostResponse {
                                override fun onFailed(status: Int, msg: String) {
                                    pb_host?.isVisible = false
                                    ilRetry_host?.isVisible = true
                                    getToast(
                                        this@HostFragment,
                                        "$FAIL_CLOUD_GET_DEVICE_LIST$status"
                                    )
                                }

                                override fun onSuccess(data: ArrayList<CloudHostData>?) {
                                    pb_host?.isVisible = false
                                    if (!data.isNullOrEmpty()) {
                                        ilRetry_host?.isVisible = false
                                        tvPay_host?.isVisible = true
                                        //初始化列表
                                        initNewRecyclerView(data, autoNext)
                                    } else {
                                        getToast(this@HostFragment, "无机房")
                                    }
                                }
                            })
                    }
                }
            }

            override fun startNextFra(activity: Activity?, autoNext: Boolean) {
                (activity as PreDataActivity).mAbs?.let { abs ->
                    if (!abs.startCheckShowReward()) {
                        abs.startFraLine(getType(), getName(), mCurRoom)
                        return
                    } else if (autoNext) {
                        //外部跳转至需要激励，自动则止步于此
                        return
                    }
                    pb_host?.isVisible = true
                    //开始展示激励
                    abs.startShowReward(true, false, getType(), getName(), mCurRoom, getBtn())
                }
            }

            override fun startSelectRoom(id: Int?) {
                mCurRoom = id
                //选中按钮
                if (!oldList.isNullOrEmpty()) {
                    oldList?.forEach { host ->
                        host.defaultSelect = host.roomId == mCurRoom
                    }
                    mAdapter?.notifyItemRangeChanged(0, oldList?.size ?: 0)
                }
            }

            override fun getType(): Int? {
                return type
            }

            override fun getName(): String? {
                return name
            }

            override fun getBtn(): String? {
                return btn
            }
        }
    }

    //初始化新列表
    private fun initNewRecyclerView(dataList: ArrayList<CloudHostData>, autoNext: Boolean) {
        oldList = dataList
        //产商
        mAdapter = CommonRecyclerAdapter.Builder()
            .setData(oldList!!).setLayout(R.layout.duo_item_specify)
            .bindView { holder, _, position ->
                val data = oldList!![position]
                holder.itemView.findViewById<TextView>(R.id.tvTitle_item)?.text = data.room
                holder.itemView.findViewById<TextView>(R.id.tvList_item)?.isVisible = false
                holder.itemView.findViewById<ImageView>(R.id.ivSelect_item)?.visibility =
                    if (data.defaultSelect) View.VISIBLE else View.INVISIBLE
                holder.itemView.findViewById<TextView>(R.id.tvSpeed_item)?.let { sp ->
                    sp.isVisible = true
                    sp.text = data.net
                    sp.setTextColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            getNetColor(data.net)
                        )
                    )
                }
                //进入按钮
                holder.itemView.setOnClickListener {
                    mHostAbs?.startSelectRoom(data.roomId)
                }
            }.create()
        rvRoom_host?.adapter = mAdapter
        //初始化选中
        mHostAbs?.startSelectRoom(dataList.first().roomId)
    }

    //0-89ms是绿色，90-199是橙色，200以上是红色
    private fun getNetColor(net: String?): Int {
        var speed = 0
        if (!net.isNullOrEmpty()) {
            try {
                speed = net.replace("s", "").replace("m", "").toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return when (speed) {
            in 0..90 -> R.color.green
            in 90..199 -> R.color.orange
            else -> R.color.red
        }
    }
}