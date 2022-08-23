package com.cloud.duolib.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.ResultRewardOkCallBack
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.abs.PreActAbs
import com.cloud.duolib.model.helper.ApiQuitHelper
import com.cloud.duolib.model.util.*
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import com.cloud.duolib.ui.fre.HostFragment
import com.cloud.duolib.ui.fre.ServerFragment
import com.cloud.duolib.ui.fre.LineFragment
import com.cloud.duolib.view.dialog.CommonVerticalDialog
import com.cloud.duolib.view.dialog.DialogHorizonHelper
import com.cloud.duolib.view.dialog.DialogProgressHelper
import com.cloud.duolib.view.dialog.DialogVerticalHelper
import kotlinx.android.synthetic.main.duo_fragment_server.*
import kotlinx.android.synthetic.main.duo_layout_top.*
import kotlin.math.abs

class PreDataActivity : BasePermissionActivity() {
    //上次碎片位置
    private var mFraLastPos = 0

    //碎片列表
    private val mFragments = arrayOf(ServerFragment(), LineFragment(), HostFragment())

    //布局构建器
    private val mDialogVerHelper = DialogVerticalHelper()
    private val mDialogHorHelper = DialogHorizonHelper()
    private val mDialogProHelper = DialogProgressHelper()
    private var mDialogQuit: CommonVerticalDialog? = null

    //请求类
    private var mApiQuitHelper = ApiQuitHelper()

    //开始预跳转
    private var mStartFra = false

    //非加载状态
    private var mShowingDialog = false

    //控制器
    var mAbs: PreActAbs? = null

    companion object {
        private const val ACT_LINE_FRA_INIT_DATA = "ACT_LINE_FRA_INIT_DATA"
        private const val ACT_LINE_FRA_SERVER_TYPE = "ACT_LINE_FRA_SERVER_TYPE"
        private const val ACT_LINE_FRA_POS = "ACT_LINE_FRA_POS"

        fun newInstance(act: Activity, data: InitCloudData, type: Int?, pos: Int?) {
            Intent(act, PreDataActivity::class.java).apply {
                this.putExtra(ACT_LINE_FRA_INIT_DATA, data)
                this.putExtra(ACT_LINE_FRA_SERVER_TYPE, type)
                this.putExtra(ACT_LINE_FRA_POS, pos)
                act.startActivity(this)
                if ((act is DeviceMediaActivity) || (act is VideoPlayActivity)) {
                    act.finish()
                }
            }
        }
    }

    override fun getLayoutId() = R.layout.duo_activity_fragment_child
    override fun initView(savedInstanceState: Bundle?) {
        initAbs(intent?.getParcelableExtra(ACT_LINE_FRA_INIT_DATA))

        //初始化碎片
        initFraServer(
            intent?.getIntExtra(ACT_LINE_FRA_SERVER_TYPE, -1) ?: -1,
            intent?.getIntExtra(ACT_LINE_FRA_POS, 0) ?: 0
        )

        //返回监听
        ivBack_top?.setOnClickListener {
            mAbs?.startClickBack()
        }
    }

    override fun onBackPressed() {
        ivBack_top?.callOnClick()
    }

    override fun initBeforeView() {}

    private fun initAbs(initCloudData: InitCloudData?) {
        mAbs = object : PreActAbs() {
            override fun getApiRequestData(): InitCloudData? {
                return initCloudData
            }

            override fun startClickBack() {
                supportFragmentManager.findFragmentById(R.id.fra_child)?.let { fra ->
                    when (fra) {
                        is LineFragment -> {
                            //排队界面退出即销毁本
                            startQuitLine(fra.mCurType)
                        }
                        is HostFragment -> {
                            startFraServer(fra.mHostAbs?.type ?: -1)
                        }
                        else -> {
                            this@PreDataActivity.finish()
                        }
                    }
                }
            }

            override fun startQuitLine(type: Int?) {
                mDialogQuit = mDialogVerHelper.getViewClickSure(this@PreDataActivity) {
                    mApiQuitHelper.apiQuitLine(this@PreDataActivity, type, mAbs?.apiRequestData)
                }
            }

            override fun startSetTitle(title: String?) {
                if (!title.isNullOrEmpty()) {
                    tvTitle_top?.text = title
                }
            }

            override fun startSetNewToken(token: String?) {
                if (!token.isNullOrEmpty()) {
                    mAbs?.apiRequestData?.app_token = token
                }
            }

            override fun startFraLine(type: Int, name: String?, rid: Int?) {
                logShow(info = "排队 $type $name $rid")
                if (!mStartFra) {
                    mStartFra = true
                    CloudBuilder.getUMCallBack(UM_DUO_CHOICE_SERVER, name ?: "其它")
                    val bundle = Bundle()
                    bundle.putInt(
                        LineFragment.FRA_LINE_TYPE, type
                    )
                    if (rid != null) {
                        bundle.putInt(
                            LineFragment.FRA_ROOM_TYPE, rid
                        )
                    }
                    bundle.putParcelable(
                        LineFragment.FRA_LINE_INIT_DATA, mAbs?.apiRequestData
                    )
                    mFragments[1].arguments = bundle
                    startFraChange(mFragments[1])
                    mFraLastPos = 1
                    mStartFra = false
                }
            }

            override fun startFraHost(type: Int, name: String?, btn: String?, autoNext: Boolean) {
                if (!mStartFra) {
                    mStartFra = true
                    val bundle = Bundle()
                    bundle.putInt(
                        HostFragment.FRA_HOST_TYPE, type
                    )
                    bundle.putString(
                        HostFragment.FRA_HOST_NAME, name
                    )
                    bundle.putString(
                        HostFragment.FRA_HOST_BTN_INFO, btn
                    )
                    bundle.putParcelable(
                        HostFragment.FRA_HOST_INIT_DATA, mAbs?.apiRequestData
                    )
                    mFragments[2].arguments = bundle
                    startFraChange(mFragments[2])
                    mFraLastPos = 2
                    mStartFra = false
                }
            }

            override fun startFraServer(type: Int) {
                if (!mStartFra) {
                    mStartFra = true
                    val bundle = Bundle()
                    bundle.putInt(
                        ServerFragment.FRA_SERVER_TYPE, type
                    )
                    bundle.putParcelable(
                        ServerFragment.FRA_SERVER_INIT_DATA, mAbs?.apiRequestData
                    )
                    mFragments[0].arguments = bundle
                    startFraChange(mFragments[0])
                    mFraLastPos = 0
                    mStartFra = false
                }
            }

            override fun startCheckShowReward(): Boolean {
                return CloudBuilder.getShowReward() && (abs(
                    OsTimeUtils.getCurrentSecond() - PreferenceUtil.getLong(
                        PREFERENCE_REWARD_INTERVAL,
                        0L
                    )
                ) > (CloudBuilder.getShowRewardInterval() ?: 0))
            }

            override fun startShowReward(
                first: Boolean, nextHost: Boolean,
                type: Int, name: String?,
                rid: Int?, btn: String?
            ) {
                mStartFra = true
                CloudBuilder.getResultRewardCallBack(object :
                    ResultRewardOkCallBack {
                    override fun onCloseRewardVerify(showSuccess: Boolean) {
                        logShow(info = "$first onCloseRewardVerify=$showSuccess")
                        PreferenceUtil.putLong(
                            PREFERENCE_REWARD_INTERVAL,
                            OsTimeUtils.getCurrentSecond()
                        )
                        mStartFra = false
                        if (nextHost) {
                            startFraHost(type, name, btn, false)
                        } else {
                            startFraLine(type, name, rid)
                        }
                    }

                    override fun onErrorRewardVerify() {
                        logShow(info = "$first onErrorRewardVerify")
                        mStartFra = false
                        if (first) {
                            if (!mShowingDialog) {
                                //点击确认观看广告
                                mShowingDialog = true
                                startDlgReward(nextHost, type, name, rid, btn)
                            }
                            return
                        }
                        //第二次加载失败则直接进入
                        if (nextHost) {
                            startFraHost(type, name, btn, false)
                        } else {
                            startFraLine(type, name, rid)
                        }
                    }

                    override fun onShowRewardVideo() {
                        logShow(info = "$first onShowRewardVideo")
                        pb_server?.isVisible = false
                    }
                }, this@PreDataActivity)
            }

            override fun startDlgReward(
                nextHost: Boolean, type: Int,
                name: String?, rid: Int?, btn: String?
            ) {
                mDialogHorHelper.showDialogReward(
                    this@PreDataActivity,
                    type, name, rid, nextHost, btn,
                    {
                        startShowReward(false, nextHost, type, name, rid, btn)
                        mShowingDialog = false
                    },
                    { mShowingDialog = false }
                )
            }
        }
    }

    private fun initFraServer(type: Int, pos: Int) {
        mFraLastPos = pos
        NavigationBarUtils().setNavigationBar(
            this@PreDataActivity,
            true,
            resources.getColor(R.color.gray6)
        )
        tvTitle_top?.text = getString(R.string.view_create_cloud)
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        val bundle = Bundle()
        if (pos == 0) {
            //进入选择
            bundle.putParcelable(ServerFragment.FRA_SERVER_INIT_DATA, mAbs?.apiRequestData)
            bundle.putInt(ServerFragment.FRA_SERVER_TYPE, type)
            if (type != -1) {
                bundle.putBoolean(ServerFragment.FRA_SERVER_HOST_AUTO, true)
            }
        } else if (pos == 1) {
            //进入排队
            bundle.putInt(
                LineFragment.FRA_LINE_TYPE, type
            )
            bundle.putParcelable(LineFragment.FRA_LINE_INIT_DATA, mAbs?.apiRequestData)
        } else if (pos == 2) {
            bundle.putInt(
                HostFragment.FRA_HOST_TYPE, type
            )
            bundle.putParcelable(
                HostFragment.FRA_HOST_INIT_DATA, mAbs?.apiRequestData
            )
        }
        mFragments[mFraLastPos].arguments = bundle
        transaction.replace(
            R.id.fra_child, mFragments[mFraLastPos]
        ).commit()
    }

    /**
     * 通用碎片部分
     * */
    private fun startFraChange(newFra: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        if (!newFra.isAdded) {
            transaction.remove(mFragments[mFraLastPos])
                .add(R.id.fra_child, newFra)
        } else {
            transaction.remove(mFragments[mFraLastPos]).show(newFra)
        }
        transaction.commitAllowingStateLoss()
    }

    //上报错误
    fun getErrorDialog(
        app_pkg: String,
        app_token: String,
        app_key: String,
        app_iv: String,
        app_co: Int,
        errorInfo: String,
        orderId: String,
        expire: String?,
        channel: String,
        type: Int?,
        response: CloudHttpUtils.StrResponse
    ) {
        mApiQuitHelper.apiReportError(
            app_pkg, app_token, app_key, app_iv, app_co,
            errorInfo, 7, 3,
            orderId, expire, channel
        ) { _, _ -> //显示弹窗
            mDialogHorHelper.getViewDuoChange(this@PreDataActivity, errorInfo) {
                mApiQuitHelper.apiRecoveryDevice(
                    1,
                    type,
                    mAbs?.apiRequestData,
                    response
                )
            }
        }
    }

    //开始使用
    fun startNextAct(_startActCallback: () -> Unit) {
        val mDialog =
            mDialogProHelper.getViewInfoSure(
                this@PreDataActivity,
                this@PreDataActivity.getString(R.string.start_use),
                true
            )
        //初始化设备使用
        Handler().postDelayed({
            if (!this@PreDataActivity.isDestroyed) {
                mDialogQuit?.dismiss()
                mDialog?.dismiss()
            }
            _startActCallback.invoke()
        }, 1000)
    }
}