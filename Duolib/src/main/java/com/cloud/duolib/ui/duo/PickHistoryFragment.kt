package com.cloud.duolib.ui.duo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.SimpleItemAnimator
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFragment
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.bean.duo.PushStatusType
import com.cloud.duolib.bean.duo.getAddFormatSize
import com.cloud.duolib.bean.duo.getProgressFormatStatus
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.helper.DuoCommandHelper
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.*
import com.cloud.duolib.view.adapter.FileAdapterListener
import com.cloud.duolib.view.adapter.FileListAdapter
import kotlinx.android.synthetic.main.duo_fragment_history_pre.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timerTask

class PickHistoryFragment : BaseFragment(), FileAdapterListener {
    private val mCommandHelper = DuoCommandHelper()
    private var mFileListStart = ArrayList<BeanFile>()
    private var mAdapterStart: FileListAdapter? = null
    private var mJdIntent: String? = null

    //根据路径，获取进度
    private var mCurFileState = HashMap<String, PushStatusType>()

    override fun getLayoutId() = R.layout.duo_fragment_history_pre
    override fun initView(savedInstanceState: Bundle?) {
        mJdIntent = arguments?.getString(FRA_HISTORY_JD_INTENT)
        tvStart_pre?.isVisible = false
        tvHistory_pre?.isVisible = false
        //初始化列表
        initFilePushData()
        //初始化信息流
        llContext_pre?.removeAllViews()
        getAct(this@PickHistoryFragment) { act ->
            CloudBuilder.getNativeCallBack(llContext_pre, act)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            initFilePushData()
        }
    }

    //开始文件上传
    private fun initFilePushData() {
        val newFileList = arguments?.getParcelableArrayList<BeanFile>(FRA_HISTORY_REFRESH_LIST)
        if (!newFileList.isNullOrEmpty()) {
            newFileList.forEach {
                if (!mFileListStart.contains(it)) {
                    mFileListStart.add(it)
                }
            }
            initListStartView()
        }
    }

    //上传开始
    private fun initListStartView() {
        getCxt(this@PickHistoryFragment, { cxt ->
            if (mAdapterStart == null) {
                mAdapterStart = FileListAdapter(
                    cxt, mFileListStart, null,
                    FilePickManager.selectAppMap, this, 4, null
                )
                rvStart_pre?.adapter = mAdapterStart
                val animator = rvStart_pre.itemAnimator
                if (animator is SimpleItemAnimator) {
                    animator.supportsChangeAnimations = false
                } else {
                    rvStart_pre?.itemAnimator?.changeDuration = 0
                }
            } else {
                mAdapterStart?.setData(mFileListStart, null)
            }
        }, null)
    }

    companion object {
        const val FRA_HISTORY_REFRESH_LIST = "FRA_HISTORY_REFRESH_LIST"
        const val FRA_HISTORY_JD_INTENT = "FRA_HISTORY_JD_INTENT"
        private const val TIME_UPDATE = 0x125
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //注册接收上传进度
        val filter = IntentFilter()
        filter.addAction(BROADCAST_FILE_PUSH_START)
        filter.addAction(BROADCAST_FILE_PUSH_END)
        (this.parentFragment?.activity as? DeviceMediaActivity)?.registerReceiver(mReceiver, filter)
        handler = MyHandler(this@PickHistoryFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        (this.parentFragment?.activity as? DeviceMediaActivity)?.unregisterReceiver(mReceiver)
        handler = null
    }

    private val mReceiver = MyReceiver()

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(cxt: Context?, intent: Intent?) {
            when (intent?.action) {
                BROADCAST_FILE_PUSH_START -> {
                    //刷新状态
                    intent.getStringExtra(RECEIVER_FILE_PUSH_FILE_PATH)?.let { mPath ->
                        (intent.getSerializableExtra(RECEIVER_FILE_PUSH_FILE_STATUS) as? PushStatusType)?.let { mStatus ->
                            mCurFileState[mPath] = mStatus
                            startTimer()
                        }
                    }
                }
                BROADCAST_FILE_PUSH_END -> {
                    //上传结束
                    intent.getStringExtra(RECEIVER_FILE_PUSH_FILE_STATUS)?.let { endInfo ->
                        intent.getStringExtra(RECEIVER_FILE_PUSH_FILE_PATH).let { existUnApp ->
                            //重置选中
                            (this@PickHistoryFragment.parentFragment as? FilePickerFragment)?.startFraFileEnd(
                                if (existUnApp.isNullOrEmpty()) (endInfo + "\n" + OPEN_ZAR_SUC) else endInfo
                            )
                            if (existUnApp.isNullOrEmpty()) {
                                //存在非应用文件
                                startAppOpen(showToast = false)
                            } else {
                                //打开最后一个
                                mFileListStart.forEach { file ->
                                    if (existUnApp == file.path) {
                                        startFileOpen(file, false)
                                        return@forEach
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //按钮点击时文件状态
    private fun startFileOpen(file: BeanFile, showToast: Boolean) {
        when (file.status) {
            PushStatusType.OPEN -> {
                FilePickManager.selectAppMap[file.path]?.let { app ->
                    app.actName?.let {
                        startAppOpen(app.pkgName, it, showToast = showToast)
                    }
                }
            }
            PushStatusType.VIEW -> {
                startAppOpen(showToast = showToast)
            }
        }
    }

    //点击按钮打开
    private fun startAppOpen(
        pkg: String = OPEN_ZAR_PKG,
        act: String = OPEN_ZAR_ACT,
        showToast: Boolean
    ) {
        getToast(this@PickHistoryFragment, R.string.open_now)
        if ((pkg == "com.jingdong.app.mall") && !mJdIntent.isNullOrEmpty()) {
            mCommandHelper.getAppStart(
                mJdIntent!!,
                "$mJdIntent.MainActivity",
                object : OnStrResponse {
                    override fun onSuccess(str: String) {
                        //成功打开
                        if (showToast && (pkg == OPEN_ZAR_PKG)) {
                            getToastLong(this@PickHistoryFragment, OPEN_ZAR_SUC)
                        }
                        //隐藏
                        (this@PickHistoryFragment.parentFragment as? FilePickerFragment)?.hideFraChild(
                            true
                        )
                    }

                    override fun onFailed(str: String) {
                        getAppStart(pkg, act, showToast)
                    }
                })
        } else {
            getAppStart(pkg, act, showToast)
        }
    }

    private fun getAppStart(
        pkg: String,
        act: String,
        showToast: Boolean
    ) {
        mCommandHelper.getAppStart(pkg, act, object : OnStrResponse {
            override fun onSuccess(str: String) {
                //成功打开
                if (showToast && (pkg == OPEN_ZAR_PKG)) {
                    getToastLong(this@PickHistoryFragment, OPEN_ZAR_SUC)
                }
                //隐藏
                (this@PickHistoryFragment.parentFragment as? FilePickerFragment)?.hideFraChild(
                    true
                )
            }

            override fun onFailed(str: String) {
                if (showToast && (pkg == OPEN_ZAR_PKG)) {
                    //提示错误
                    getToastLong(this@PickHistoryFragment, OPEN_ZAR_FAIL)
                }
            }
        })
    }

    override fun onFoldClicked(beanFile: BeanFile, pos: Int) {
        startFileOpen(beanFile, true)
    }

    override fun onFileClicked(outSize: Boolean) {}

    private var sTask: TimerTask? = null
    private var sTimer: Timer? = null
    private var isRunning = false
    private var handler: Handler? = null

    private fun startTimer() {
        if (!isRunning && sTimer == null && sTask == null && handler != null) {
            sTimer = Timer()
            sTask = timerTask {
                run {
                    handler?.removeMessages(TIME_UPDATE)
                    handler?.sendEmptyMessage(TIME_UPDATE)
                }
            }
            sTimer?.schedule(sTask, 100, 500) //启动间隔0.5s循环执行
            isRunning = true
            logShow(info = "startTimer")
        }
    }

    private fun stopTimer() {
        if (isRunning) {
            sTimer?.cancel()
            sTimer = null
            sTask?.cancel()
            sTask = null
            handler?.removeMessages(TIME_UPDATE)
            isRunning = false
            logShow(info = "stopTimer")
        }
    }

    private class MyHandler(oneService: Fragment) : Handler() {
        private val reference = WeakReference(oneService)
        override fun handleMessage(msg: Message?) {
            val timesService = reference.get() ?: return
            when (msg?.what) {
                TIME_UPDATE -> {
                    (timesService as? PickHistoryFragment)?.refreshState()
                }
            }
        }
    }

    private fun refreshState() {
        mFileListStart.iterator().let { iterator ->
            //是否需要刷新
            var flagRefresh = true
            while (iterator.hasNext()) {
                val item = iterator.next()
                mCurFileState[item.path]?.let { state ->
                    item.status = state
                    //当前进度状态
                    getProgressFormatStatus(state).let {
                        when {
                            it <= 10.0 -> {
                                //开始
                                item.progress = it
                            }
                            it == 99.0 -> {
                                //安装中
                                item.progress = it
                            }
                            it == 100.0 -> {
                                //已安装
                                item.progress = it
                                mCurFileState.remove(item.path)
                            }
                            item.progress < 98 -> {
                                //递增值
                                item.progress = item.progress + getAddFormatSize(item.size)
                            }
                            else -> {
                                //近峰值
                                flagRefresh = false
                            }
                        }
                    }
                }
            }
            //刷新列表
            if (flagRefresh) {
                mAdapterStart?.notifyItemRangeChanged(0, mFileListStart.size)
            }
            //停止任务
            if (mCurFileState.isNullOrEmpty()) {
                stopTimer()
            }
        }
    }
}