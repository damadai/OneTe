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
        //??????????????????
        mPhone = arguments?.getString(FRA_FILE_PHONE_DATA)
        mInitCloudData = arguments?.getParcelable(FRA_FILE_INIT_DATA)
        //?????????????????????
        FilePickManager.setMaxCount(7)
        //?????????????????????
        val dopPackageInfo = arguments?.getParcelable<PackageInfo>(FRA_HISTORY_PRE_DOP_INFO)
        if (dopPackageInfo != null) {
            getCxt(this@FilePickerFragment, {
                PkgInfoUtils().getInfoData(it.packageManager, dopPackageInfo).let { app ->
                    //????????????????????????
                    FilePickManager.add(
                        BeanFile(
                            app.apkName ?: app.pkgName,
                            app.path,
                            app.size ?: 0L,
                            FileType.APK
                        )
                    )
                    //????????????
                    startFraFileHistory()
                    mDenyOnce = false
                }
            }, null)
        }
        //????????????
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
        //???????????????
        initPickPage()
    }

    private fun initPickPage() {
        getAct(this@FilePickerFragment) { act ->
            (act as? DeviceMediaActivity)?.startPermissionStorage(object : OnBaseSucResponse {
                override fun onSuccess() {
                    //???????????????
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
                        mPickAdapter?.addFragment(mFraList.component2(), "??????")
                        mPickAdapter?.addFragment(mFraList.component3(), "??????")
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

    //??????????????????
    private fun startFilePush() {
        if (FilePickManager.getMaxCount() != 1 && (FilePickManager.currentCount != 0)) {
            //???????????????
            if (!mStartPush) {
                //????????????
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

    //????????????????????????
    private fun startFraFileHistory() {
        //?????????????????????
        if (!mPhone.isNullOrEmpty() && mInitCloudData != null) {
            initFraHistory(true)
            //????????????
            getCxt(this@FilePickerFragment, {
                FileJobService.initServiceWork(
                    it,
                    mPhone,
                    mInitCloudData,
                    FilePickManager.selectFiles
                )
                //????????????
                mStartPush = true
            }, null)
        } else {
            getToastTest(this@FilePickerFragment, "?????????")
        }
    }

    //??????????????????fra????????????
    fun startFraFileEnd(info: String) {
        if (mStartPush) {
            getAct(this@FilePickerFragment) {
                (it as? DeviceMediaActivity)?.startFileEndTips(info)
            }
            //????????????
            mStartPush = false
            //???????????????
            FilePickManager.reset()
            getAct(this@FilePickerFragment) { act ->
                (act as? DeviceMediaActivity)?.startHideFraChild(this@FilePickerFragment)
            }
        }
    }

    //????????????????????????????????????
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
                //isAdd??????????????????????????????
                transaction.add(R.id.fcHistory_pick, mFraList.component4(), "HISTORY")
            } else {
                transaction.show(mFraList.component4())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        transaction.commitAllowingStateLoss()
    }

    //?????????????????????????????????
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
            //????????????????????????
            if (!mStartPush) {
                //???????????????
                FilePickManager.reset()
            }
        }
        //??????fra
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
            var count = "???????????????${num}???"
            val size = getFileFormatSize(FilePickManager.curSize)
            count += if (size.isNotEmpty()) {
                "??????${size}???"
            } else {
                "???"
            }
            push_pick?.text = count
        }
    }
}