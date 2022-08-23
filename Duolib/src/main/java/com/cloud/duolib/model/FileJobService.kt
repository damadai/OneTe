package com.cloud.duolib.model

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.RemoteException
import androidx.core.app.JobIntentService
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.duo.*
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.model.helper.DuoCommandHelper
import com.cloud.duolib.model.helper.PushFileHelper
import com.cloud.duolib.model.util.*
import java.io.File
import kotlin.math.abs

class FileJobService : JobIntentService() {
    companion object {
        const val JOB_KEY = "JOB_KEY"

        private const val KEY_START_PUSH = "KEY_START_PUSH"//pushFile方式上传
        private const val KEY_START_PUSH_PID = "KEY_START_PUSH_PID"//pushFile方式上传
        private const val KEY_START_PUSH_INIT_DATA = "KEY_START_PUSH_INIT_DATA"//pushFile方式上传
        private const val KEY_START_PUSH_LIST_DATA = "KEY_START_PUSH_LIST_DATA"//pushFile方式上传

        /**
         * Unique job ID for this service.
         */
        private const val JOB_ID = 7001

        /**
         * Convenience method for enqueuing work in to this service.
         */
        fun initServiceWork(
            context: Context,
            pid: String?,
            data: InitCloudData?,
            list: ArrayList<BeanFile>
        ) {
            if (!pid.isNullOrEmpty() && data != null) {
                try {
                    val work = Intent()
                    work.putExtra(JOB_KEY, KEY_START_PUSH)
                    work.putExtra(KEY_START_PUSH_PID, pid)
                    work.putExtra(KEY_START_PUSH_INIT_DATA, data)
                    work.putExtra(KEY_START_PUSH_LIST_DATA, list)
                    enqueueWork(
                        context,
                        FileJobService::class.java, JOB_ID, work
                    )
                    return
                } catch (e: RemoteException) {
                    e.printStackTrace()
                    logShow(info = "initServiceWork=$e")
                }
            }
            PushFileHelper().sendStatusEnd(context, "初始化任务失败", null)
        }
    }

    override fun onHandleWork(intent: Intent) {
        when (intent.getStringExtra(JOB_KEY)) {
            KEY_START_PUSH -> {
                mPhoneId = intent.getStringExtra(KEY_START_PUSH_PID)
                mInitCloudData = intent.getParcelableExtra(KEY_START_PUSH_INIT_DATA)
                mFileList = intent.getParcelableArrayListExtra(KEY_START_PUSH_LIST_DATA)
                if (!mPhoneId.isNullOrEmpty() && (mInitCloudData != null) && !mFileList.isNullOrEmpty()) {
                    startPush(this@FileJobService, mFileList!!.first())
                } else {
                    sendFinishBroad("失败")
                }
            }
        }
    }

    //需参
    private val mDirPath = "/sdcard/"
    private var mPhoneId: String? = null
    private var mInitCloudData: InitCloudData? = null

    //本地时间不匹配
    private var mErrorTime: String? = null

    //方法上传失败后重试一次
    private var mFailLocalPushTry = false

    //有上传失败应用
    private var mUnAppFileExit = false

    //有上传失败应用
    private var mFailFilePushList = ArrayList<String>()

    //有自动安装应用
    private var mAppStatePos = 0
    private var mAppStateList = ArrayList<BeanApp>()

    //有查询失败应用
    private var mFailStateList = ArrayList<String>()

    //当前任务队列
    private var mFilePos = 0
    private var mFileList: ArrayList<BeanFile>? = null

    //当前安装状态
    private var mAppInstateStatus: String? = null

    //上传桶
    private var mObs: DuoObData? = null

    //请求类
    private val mCommandHelper = DuoCommandHelper()
    private val mPushFileHelper = PushFileHelper()

    //刷新状态线程
    private var mHandler: Handler? = null

    /**
     * 1\ 开始上传
     * */
    private fun startPush(mCxt: Context, file: BeanFile) {
        mInitCloudData?.let { data ->
            mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.START, file.path)
            if (file.type == FileType.APK) {
                //当前为应用
                mCxt.packageManager?.let {
                    PkgInfoUtils().getPathData(it, file.path).let { bean ->
                        startApiPush(mCxt, file, bean, data)
                        if (!CloudBuilder.getShowLog()) {
                            //上报开始传送
                            CloudBuilder.getUMCallBack(UM_DUO_APP_PUSH, bean.apkName + bean.pkgName)
                        }
                    }
                }
            } else {
                //当前非应用
                startLocalPush(
                    mCxt,
                    file,
                    null,
                    null,
                    data
                )
            }
        }
    }

    //接口次数判断
    private fun startApiPush(mCxt: Context, file: BeanFile, app: BeanApp, data: InitCloudData) {
        val fileMd5 = MD5Utils.getFileMD5String(File(app.path))
        mFailLocalPushTry = false
        CloudHttpUtils.getInstance().getUploadRx(
            data.app_pkg,
            data.app_co,
            data.app_token,
            fileMd5,
            mPhoneId,
            app.pkgName,
            data.app_key,
            data.app_iv,
            object : CloudHttpUtils.StrResponse {
                override fun onSuccess(str: String?) {
                    when (str) {
                        "0" -> {//首次记录
                            startLocalPush(mCxt, file, app, null, data)
                        }
                        "1" -> {//回调上传文件成功的接口
                            startLocalPush(mCxt, file, app, fileMd5, data)
                        }
                        "2" -> {//客户端不再上传 直接从obs拉取安装并打开
                            mPushFileHelper.sendStatusRefresh(
                                mCxt,
                                PushStatusType.SPEED_UPLOAD,
                                app.path
                            )
                            mAppStateList.add(app)
                            if (!CloudBuilder.getShowLog()) {
                                //上报加速
                                CloudBuilder.getUMCallBack(
                                    UM_DUO_APP_PUSH_SPEED,
                                    app.apkName + app.pkgName
                                )
                            }
                            startNext(mCxt)
                        }
                        else -> {
                            //开始本地推送
                            startLocalPush(mCxt, file, app, null, data)
                        }
                    }
                }

                override fun onFailed(status: Int, msg: String) {
                    startLocalPush(mCxt, file, app, null, data)
                }
            })
    }

    //获取上传桶
    private fun startLocalPush(
        mCxt: Context,
        file: BeanFile,
        app: BeanApp?,
        md5: String?,
        pData: InitCloudData
    ) {
        var dotName = file.name
        var fPath = file.path
        if (app != null) {
            dotName = app.apkName ?: app.pkgName
            fPath = app.path
            if (!dotName.endsWith("apk")) {
                dotName += ".apk"
            }
        }
        if (mObs == null) {
            val cur = OsTimeUtils.getCurrentTime1()
            CloudHttpUtils.getInstance().getUploadObsRx(
                pData.app_pkg,
                pData.app_co,
                pData.app_token,
                pData.app_key,
                pData.app_iv,
                object : CloudHttpUtils.ObsResponse {
                    override fun onSuccess(data: DuoObData?) {
                        if (data != null) {
                            var error = false
                            if (!data.SafeTime.isNullOrEmpty()) {
                                //15分钟之内差值有效
                                error = abs(OsTimeUtils.getTimeMix(cur, data.SafeTime)) >= 15
                            }
                            startPushLocal(mCxt, file, app, md5, pData, data, dotName, fPath, error)
                        } else {
                            localObsFail(mCxt, dotName, fPath)
                        }
                    }

                    override fun onFailed(status: Int, str: String?) {
                        localObsFail(mCxt, dotName, fPath)
                    }
                })
        } else {
            startPushLocal(mCxt, file, app, md5, pData, mObs!!, dotName, fPath, false)
        }
    }

    //拿桶失败
    private fun localObsFail(
        mCxt: Context,
        name: String,
        path: String
    ) {
        mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.FAIL_OBS, path)
        mFailFilePushList.add(name)
        startNext(mCxt)
    }

    //本地方法上传
    private fun startPushLocal(
        mCxt: Context,
        file: BeanFile,
        app: BeanApp?,
        md5: String?,
        pData: InitCloudData,
        obData: DuoObData,
        dotName: String,
        fPath: String,
        errorTime: Boolean
    ) {
        mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.UPLOAD, fPath)
        mCommandHelper.getFilePush(
            fPath,
            "$mDirPath$dotName",
            obData,
            object : OnBoolResponse {
                override fun onSuccess(ok: Boolean) {
                    if (ok) {
                        if (app != null) {
                            //直接安装
                            startAppInstall(mCxt, md5, pData, app, dotName, fPath)
                            if (!CloudBuilder.getShowLog()) {
                                //上报本地
                                CloudBuilder.getUMCallBack(
                                    UM_DUO_APP_PUSH_LOCAL,
                                    app.apkName + app.pkgName
                                )
                            }
                        } else {
                            //非应用文件
                            mUnAppFileExit = true
                            mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.VIEW, fPath)
                            startNext(mCxt)
                        }
                        //上传成功才算obs可以使用
                        if (mObs == null) {
                            mObs = obData
                        }
                    } else {
                        localPushFail(mCxt, file, app, md5, pData, dotName, fPath)
                    }
                }

                override fun onFailed(str: String) {
                    if (errorTime) {
                        mErrorTime = "时间安全检测不匹配 !\n请确认手机日期与网络是否同步!"
                    }
                    localPushFail(mCxt, file, app, md5, pData, dotName, fPath)
                }
            }
        )
    }

    //上传失败
    private fun localPushFail(
        mCxt: Context,
        file: BeanFile,
        app: BeanApp?,
        md5: String?,
        pData: InitCloudData,
        name: String,
        path: String
    ) {
        if (!mFailLocalPushTry) {
            startLocalPush(mCxt, file, app, md5, pData)
            mFailLocalPushTry = true
        } else {
            mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.FAIL_UPLOAD, path)
            mFailFilePushList.add(name)
            startNext(mCxt)
        }
    }

    //推送完安装应用
    private fun startAppInstall(
        mCxt: Context,
        md5: String?,
        pData: InitCloudData,
        app: BeanApp,
        dotName: String, fPath: String
    ) {
        mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.INSTALL, fPath)
        if (!app.actName.isNullOrEmpty()) {
            mCommandHelper.getInstallApp(
                "$mDirPath$dotName",
                app.pkgName,
                app.actName!!,
                false,//mFileList?.size == 1,
                object : OnBoolResponse {
                    override fun onSuccess(ok: Boolean) {
                        if (ok) {
                            mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.OPEN, fPath)
                            if (md5 != null) {
                                //上报后继续
                                startApiPushSuc(mCxt, md5, pData, dotName)
                            } else {
                                //直接继续
                                startNext(mCxt)
                            }
                        } else {
                            appInstallFail(mCxt, fPath)
                        }
                    }

                    override fun onFailed(str: String) {
                        appInstallFail(mCxt, fPath)
                    }
                }
            )
        } else {
            appInstallFail(mCxt, fPath)
        }
    }

    //安装失败
    private fun appInstallFail(mCxt: Context, fPath: String) {
        mPushFileHelper.sendStatusRefresh(mCxt, PushStatusType.FAIL_INSTALL, fPath)
        startNext(mCxt)
    }

    //接口上报推送
    private fun startApiPushSuc(
        mCxt: Context,
        md5: String,
        pData: InitCloudData,
        dotName: String
    ) {
        CloudHttpUtils.getInstance().getUploadSucRx(
            pData.app_pkg,
            pData.app_co,
            pData.app_token,
            md5,
            mPhoneId,
            "$mDirPath$dotName",
            pData.app_key,
            pData.app_iv,
            object : CloudHttpUtils.StrResponse {
                override fun onSuccess(str: String?) {
                    startNext(mCxt)
                }

                override fun onFailed(
                    status: Int,
                    str: String?
                ) {
                    startNext(mCxt)
                }
            })
    }

    /**
     * 2\ 完成上传
     * */
    //开始下一个
    private fun startNext(mCxt: Context) {
        try {
            if (!mFileList.isNullOrEmpty()) {
                mFilePos++
                if (mFilePos == mFileList?.size ?: 0) {
                    startNextState()
                } else {
                    //推送下一个
                    startPush(mCxt, mFileList!![mFilePos])
                }
            } else {
                startNextState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            startNextState()
        }
    }

    //开始下一步
    private fun startNextState() {
        if (!mAppStateList.isNullOrEmpty()) {
            //需要刷新进度
            startRefreshState()
        } else {
            startNextEnd()
        }
    }

    //全部完成
    private fun startNextEnd() {
        var suc = getSucMsg()
        //错误文件
        val error = getErrorMsg()
        if (error.isNotEmpty() && error.isNotBlank()) {
            suc = error
        }
        sendFinishBroad(suc)
    }

    /**
     * 3\ 查询自动安装进度
     * */
    //执行刷新指令
    private fun startRefreshState() {
        if (!mAppStateList.isNullOrEmpty()) {
            val app = mAppStateList[mAppStatePos]
            mCommandHelper.getAppInstallState(
                app.pkgName,
                object : OnStrResponse {
                    override fun onSuccess(str: String) {
                        //查询下一个
                        stateFailOrNext(app, str)
                        //状态赋值
                        mAppInstateStatus = str
                    }

                    override fun onFailed(str: String) {
                        //查询下一个
                        mFailStateList.add(app.apkName ?: app.pkgName)
                        stateFailOrNext(app, null)
                    }
                })
        } else {
            startNextEnd()
        }
    }

    //延时刷新
    private fun stateFailOrNext(app: BeanApp, newStatus: String?) {
        val isInstall = (newStatus == "4") || (newStatus == "5")
        //查询失败、6未安装、7已安装
        if (!isInstall) {
            //发送广播
            mPushFileHelper.sendStatusRefresh(
                this,
                if (newStatus == "7") PushStatusType.OPEN else PushStatusType.FAIL_STATE,
                app.path
            )
            //状态置空
            mAppInstateStatus = null
            //下标递增
            mAppStatePos++
            if (mAppStatePos == mAppStateList.size) {
                //全部完成
                startNextEnd()
                return
            }
        } else if (!newStatus.isNullOrEmpty() && (mAppInstateStatus != newStatus)) {
            //查询成功45安装中、状态不同
            mPushFileHelper.sendStatusRefresh(
                this, getFinishState(newStatus) ?: PushStatusType.FAIL_STATE,
                app.path
            )
        }
        //未完成、查询下一个
        startRunState(getSpeedFormatSize(app.size) * 1000)
    }

    //刷新循环
    private fun startRunState(mills: Long) {
        try {
            if (mHandler == null) {
                mHandler = Handler(this.mainLooper)
            }
            //当前未完成
            mHandler?.removeCallbacks(mFreshRunnable)
            mHandler?.postDelayed(mFreshRunnable, mills)
        } catch (e: Exception) {
            e.printStackTrace()
            CloudBuilder.getUMCallBack(UM_DUO_APP_PUSH_HANDLE, e.message.toString())
            //刷新任务失败
            startNextEnd()
        }
    }

    //刷新进度
    private val mFreshRunnable = Runnable {
        startRefreshState()
    }

    /**
     * 4\ 获取本次上传情况
     * */
    //获取上传、安装失败信息
    private fun getSucMsg() = "本次${mFileList?.size}个文件全部完成"
    private fun getErrorMsg(): String {
        var info = ""
        if (!mFailFilePushList.isNullOrEmpty()) {
            var msg = ""
            mFailFilePushList.forEach {
                msg += "$it "
            }
            if (msg.isNotEmpty() && msg.isNotBlank()) {
                info += "$msg (上传失败)\n"
            }
        }
        if (!mFailStateList.isNullOrEmpty()) {
            var msg = ""
            mFailStateList.forEach {
                msg += "$it "
            }
            if (msg.isNotEmpty() && msg.isNotBlank()) {
                info += "$msg (查询失败)\n"
            }
        }
        if (!mErrorTime.isNullOrEmpty()) {
            info += "\n$mErrorTime"
        }
        return info
    }

    private fun sendFinishBroad(endMsg: String) {
        //停止
        mHandler?.removeCallbacks(mFreshRunnable)
        //广播
        mPushFileHelper.sendStatusEnd(
            this,
            endMsg,
            if (mUnAppFileExit) null else mFileList?.last()?.path
        )
    }
}