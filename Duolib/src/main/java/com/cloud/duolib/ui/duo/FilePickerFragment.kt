package com.cloud.duolib.ui.duo

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.bean.duo.FileType
import com.cloud.duolib.bean.duo.getFileFormatSize
import com.cloud.duolib.model.FileJobService
import com.cloud.duolib.model.OnBaseSucResponse
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.*
import com.cloud.duolib.view.adapter.FileChildFragmentListener
import com.cloud.duolib.view.adapter.TabViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.duo_fragment_file.*
import kotlinx.android.synthetic.main.duo_layout_top.*

class FilePickerFragment : BaseFragment(), TabLayout.OnTabSelectedListener,
    FileChildFragmentListener {
    private var mFraList =
        listOf<Fragment>(
            PickFileFragment(),
            PickFoldFragment(),
            PickMediaFragment(),
            PickHistoryFragment()
        )
    private var mPickAdapter: TabViewPagerAdapter? = null
    private var mStartPush = false
    private var mPhone: String? = null
    private var mInitCloudData: InitCloudData? = null
    private var mDenyOnce = true

    companion object {
        private const val FRA_HISTORY_PRE_DOP_INFO = "FRA_HISTORY_PRE_DOP_INFO"
        private const val FRA_FILE_INIT_DATA = "FRA_FILE_INIT_DATA"
        private const val FRA_FILE_PHONE_DATA = "FRA_FILE_PHONE_DATA"

        fun newInstance(
            phone: String?,
            initData: InitCloudData?,
            dopPackageInfo: PackageInfo?
        ): Bundle {
            val bun = Bundle()
            bun.putString(FRA_FILE_PHONE_DATA, phone)
            bun.putParcelable(FRA_FILE_INIT_DATA, initData)
            bun.putParcelable(FRA_HISTORY_PRE_DOP_INFO, dopPackageInfo)
            return bun
        }
    }

    override fun getLayoutId() = R.layout.duo_fragment_file
    override fun initView(saveInstanceState: Bundle?) {
        tvTitle_top?.text = getString(R.string.push_collect)
        tvTitleEnd_top?.text = getString(R.string.push_history)
        tvTitleEnd_top?.isVisible = true
        flPush_pick?.isVisible = false
        //获取请求需参
        mPhone = arguments?.getString(FRA_FILE_PHONE_DATA)
        mInitCloudData = arguments?.getParcelable(FRA_FILE_INIT_DATA)
        //设置最大可选量
        FilePickManager.setMaxCount(7)
        //先添加历史预置
        val dopPackageInfo = arguments?.getParcelable<PackageInfo>(FRA_HISTORY_PRE_DOP_INFO)
        if (dopPackageInfo != null) {
            getCxt(this@FilePickerFragment, {
                PkgInfoUtils().getInfoData(it.packageManager, dopPackageInfo).let { app ->
                    //添加选中预置应用
                    FilePickManager.add(
                        BeanFile(
                            app.apkName ?: app.pkgName,
                            app.path,
                            app.size ?: 0L,
                            FileType.APK
                        )
                    )
                    //准备上传
                    startFraFileHistory()
                    mDenyOnce = false
                }
            }, null)
        }
        //点击监听
        initListener()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            getAct(this@FilePickerFragment) { act ->
                (act as? BasePermissionActivity)?.hideSoftKeyBoard()
            }
        } else if (mDenyOnce) {
            initPickPage()
        }
        mDenyOnce = true
    }

    override fun onResume() {
        super.onResume()
        //初始化碎片
        initPickPage()
    }

    private fun initPickPage() {
        getAct(this@FilePickerFragment) { act ->
            (act as? DeviceMediaActivity)?.startPermissionStorage(object : OnBaseSucResponse {
                override fun onSuccess() {
                    //添加碎片集
                    if (mPickAdapter == null) {
                        mPickAdapter = TabViewPagerAdapter(childFragmentManager)
                        mPickAdapter?.addFragment(
                            mFraList.component1().also { fra ->
                                val bundle = Bundle()
                                bundle.putString(
                                    PickFileFragment.FRA_APP_DOP,
                                    mInitCloudData?.appn?.pkgName
                                )
                                fra.arguments = bundle
                            },
                            "APK"
                        )
                        mPickAdapter?.addFragment(mFraList.component2(), "文件")
                        mPickAdapter?.addFragment(mFraList.component3(), "图片")
                        viewPager_pick?.offscreenPageLimit = viewPager_pick.adapter?.count ?: 3
                        viewPager_pick?.adapter = mPickAdapter
                        tabs_pick?.setupWithViewPager(viewPager_pick)
                        viewPager_pick?.addOnPageChangeListener(
                            TabLayout.TabLayoutOnPageChangeListener(tabs_pick)
                        )
                        tabs_pick?.addOnTabSelectedListener(this@FilePickerFragment)
                        setProgress(false)
                    }
                }
            })
        }
    }

    private fun initListener() {
        ivBack_top?.setOnClickListener {
            if (getProgress() == true) return@setOnClickListener
            hideFraChild(false)
        }
        flPush_pick?.setOnClickListener {
            push_pick?.callOnClick()
        }
        push_pick?.setOnClickListener {
            if ((getProgress() == true) || (mFraList.component4().isVisible)) return@setOnClickListener
            getClick { startFilePush() }
        }
        tvTitleEnd_top?.setOnClickListener {
            if ((getProgress() == true) || (mFraList.component4().isVisible)) return@setOnClickListener
            getClick { initFraHistory(false) }
        }
    }

    //判断上传文件
    private fun startFilePush() {
        if (FilePickManager.getMaxCount() != 1 && (FilePickManager.currentCount != 0)) {
            //当前无上传
            if (!mStartPush) {
                //准备上传
                startFraFileHistory()
            } else {
                getToast(this@FilePickerFragment, R.string.not_done_now)
            }
        } else {
            getToast(this@FilePickerFragment, R.string.not_select_now)
        }
    }

    fun setProgress(show: Boolean) {
        pro_file?.isVisible = show
    }

    fun getProgress() = pro_file?.isVisible

    //开始上传所选文件
    private fun startFraFileHistory() {
        //上传需参先判断
        if (!mPhone.isNullOrEmpty() && mInitCloudData != null) {
            initFraHistory(true)
            //开始后台
            getCxt(this@FilePickerFragment, {
                FileJobService.initServiceWork(
                    it,
                    mPhone,
                    mInitCloudData,
                    FilePickManager.selectFiles
                )
                //重置状态
                mStartPush = true
            }, null)
        } else {
            getToastTest(this@FilePickerFragment, "无初始")
        }
    }

    //历史界面完成fra文件上传
    fun startFraFileEnd(info: String) {
        if (mStartPush) {
            getAct(this@FilePickerFragment) {
                (it as? DeviceMediaActivity)?.startFileEndTips(info)
            }
            //重置状态
            mStartPush = false
            //取消选择项
            FilePickManager.reset()
            getAct(this@FilePickerFragment) { act ->
                (act as? DeviceMediaActivity)?.startHideFraChild(this@FilePickerFragment)
            }
        }
    }

    //历史界面是否开始刷新列表
    private fun initFraHistory(startPush: Boolean) {
        tvTitleEnd_top?.isVisible = false
        tvTitle_top?.text = getString(R.string.push_history)
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        val bundle = Bundle()
        bundle.putString(PickHistoryFragment.FRA_HISTORY_JD_INTENT, mInitCloudData?.appn?.open_intent)
        if (startPush) {
            bundle.putParcelableArrayList(
                PickHistoryFragment.FRA_HISTORY_REFRESH_LIST,
                FilePickManager.selectFiles
            )
        }
        mFraList.component4().arguments = bundle
        try {
            if (!mFraList.component4().isAdded && (childFragmentManager.findFragmentByTag("HISTORY") == null)) {
                //isAdd不准确，附加标签判断
                transaction.add(R.id.fcHistory_pick, mFraList.component4(), "HISTORY")
            } else {
                transaction.show(mFraList.component4())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        transaction.commitAllowingStateLoss()
    }

    //隐藏文件界面部分或全部
    fun hideFraChild(both: Boolean) {
        tvTitleEnd_top?.isVisible = true
        tvTitle_top?.text = getString(R.string.push_collect)
        val trans = childFragmentManager.beginTransaction()
        if (mFraList.component4().isVisible) {
            trans.hide(mFraList.component4())
            trans.commit()
            if (!both) {
                return
            }
        } else {
            //取消时当前无任务
            if (!mStartPush) {
                //取消选择项
                FilePickManager.reset()
            }
        }
        //隐藏fra
        getAct(this@FilePickerFragment) { act ->
            (act as? DeviceMediaActivity)?.startHideFra(this@FilePickerFragment)
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        viewPager_pick?.currentItem = tab!!.position
    }

    override fun onChildSelected() {
        val num = FilePickManager.currentCount
        if (num == 0) {
            flPush_pick?.isVisible = false
        } else {
            flPush_pick?.isVisible = true
            getCxt(this@FilePickerFragment, {
                push_pick?.setTextColor(ContextCompat.getColor(it, R.color.white))
            }, null)
            push_pick?.setBackgroundResource(R.drawable.duo_radius_blue_24)
            var count = "上传所选（${num}项"
            val size = getFileFormatSize(FilePickManager.curSize)
            count += if (size.isNotEmpty()) {
                "，共${size}）"
            } else {
                "）"
            }
            push_pick?.text = count
        }
    }
}