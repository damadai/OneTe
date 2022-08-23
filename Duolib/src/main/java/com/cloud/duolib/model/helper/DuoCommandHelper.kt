package com.cloud.duolib.model.helper

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.InitCloudData
import com.cloud.duolib.bean.duo.DuoObData
import com.cloud.duolib.http.CloudHttpUtils
import com.cloud.duolib.http.NtpUtil
import com.cloud.duolib.model.OnBoolResponse
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.manager.DuoDataConfig
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.*
import com.cloud.duolib.view.dialog.DialogProgressHelper
import com.cyjh.ddy.media.bean.DdyUserInfo
import com.cyjh.ddy.thirdlib.lib_hwobs.ObsCert
import com.cyjh.ddysdk.device.base.constants.DdyDeviceErrorConstants
import com.cyjh.ddysdk.device.bean.AppInfo
import com.cyjh.ddysdk.device.command.DdyDeviceCommandContract
import com.cyjh.ddysdk.device.command.DdyDeviceCommandHelper
import com.cyjh.ddysdk.order.DdyOrderContract
import com.cyjh.ddysdk.order.DdyOrderHelper
import com.cyjh.ddysdk.order.base.bean.DdyOrderInfo
import com.cyjh.ddysdk.order.base.bean.SdkLoginRespone
import com.cyjh.ddysdk.order.base.constants.DdyOrderErrorConstants
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class DuoCommandHelper {
    //多多云类型
    private val mTAG = "77777CloudManager"
    private val mNtpUtil = NtpUtil()

    /**
     * 初始化
     * */
    fun initOrderHelper(
        key: String?,
        loginId: String?,
        orderId: String?,
        limit: Long?,
        type: Int?, data: InitCloudData?,
        mResponse: OnStrResponse
    ) {
        if (!key.isNullOrEmpty()) {
            DdyOrderHelper.getInstance().initKey(key, "yunphone")
        }
        if (!loginId.isNullOrEmpty() && !orderId.isNullOrEmpty()) {
            DuoDataConfig.setLoginOrder(loginId, orderId)
        }
        DuoDataConfig.setLimitData(limit, type, data)
        GlobalScope.launch(Dispatchers.IO) {
            mNtpUtil.initTimeDif { data ->
                DuoDataConfig.setNtpDif(data)
                try {
                    getOrderInfo({
                        mResponse.onSuccess(
                            DuoKeyHelper().keyDeviceStatus(
                                it.OrderStatus,
                                null
                            )
                        )
                    }, {
                        //无设备状态
                        mResponse.onFailed(str = it)
                    }, true)
                } catch (e: Exception) {
                    mResponse.onFailed(DUO_ERROR_REQUEST)
                    logShow(mTAG, "initOrderHelper=$e")
                }
            }
        }
    }

    /**
     * 共享池先登录
     * */
    fun getUserInfo(_dataCallback: (DdyUserInfo) -> Unit, _emptyCallback: (String) -> Unit) {
        getNet({
            if (!DuoDataConfig.getUser().UCID.isNullOrEmpty()) {
                _dataCallback.invoke(DuoDataConfig.getUser())
            } else {
                if (!DuoDataConfig.getLoginId().isNullOrEmpty()) {
                    DdyOrderHelper.getInstance().requestSDKLogin(
                        DuoDataConfig.getLoginId(),
                        object : DdyOrderContract.TCallback<SdkLoginRespone> {
                            override fun onSuccess(sdkLoginRespone: SdkLoginRespone) {
                                DuoDataConfig.setUserUid(sdkLoginRespone.UCID)
                                _dataCallback.invoke(DuoDataConfig.getUser())
                            }

                            override fun onFail(
                                ddyOrderErrorConstants: DdyOrderErrorConstants,
                                s: String
                            ) {
                                val info = "登录失败,code:$ddyOrderErrorConstants,message:$s"
                                _emptyCallback.invoke(info)
                                logShow(info = info)
                                CloudBuilder.getUMCallBack(UM_DUO_INIT_ORDER, info)
                            }
                        })
                } else {
                    _emptyCallback.invoke("登录号空")
                }
            }
        }, { _emptyCallback.invoke("无网络") })
    }


    /**
     * 请求订单详情信息
     * code == 1 返回成功onSuccess(info)；// info可能为空
     * code != 1 返回失败onFail(0,msg).
     */
    fun getOrderInfo(
        _dataCallback: (DdyOrderInfo) -> Unit,
        _emptyCallback: (String) -> Unit,
        refresh: Boolean = false
    ) {
        getNet({
            if (!refresh && DuoDataConfig.mOrder != null) {
                _dataCallback.invoke(DuoDataConfig.mOrder!!)
            } else {
                getUserInfo({ data ->
                    //时间戳网络校验
                    getOrderTimeSafeLimit(object : NtpUtil.SafeDifResponse {
                        override fun onTimeout() {
                            finishAllCloudAct()
                            _emptyCallback.invoke("超时")
                        }

                        override fun onTimeSafe() {
                            DdyOrderHelper.getInstance().requestOrderDetail(
                                data.OrderId, data.UCID, "", "",
                                object : DdyOrderContract.TCallback<DdyOrderInfo> {
                                    override fun onSuccess(info: DdyOrderInfo?) {
                                        if (CloudBuilder.getShowLog()) {
                                            ToastUtils.showMsgShort(
                                                CloudBuilder.getApp(),
                                                R.string.refresh
                                            )
                                        }
                                        if (info != null) {
                                            DuoDataConfig.setUserOid(info.OrderId)
                                            DuoDataConfig.mOrder = info
                                            _dataCallback.invoke(info)
                                        } else {
                                            //无数据
                                            _emptyCallback.invoke("$DUO_SUCCESS_EMPTY 设备状态")
                                        }
                                    }

                                    override fun onFail(
                                        error: DdyOrderErrorConstants?,
                                        msg: String?
                                    ) {
                                        if (msg != null) {
                                            _emptyCallback.invoke("$DUO_ERROR_REQUEST $msg")
                                        } else {
                                            _emptyCallback.invoke("$DUO_ERROR_REQUEST 设备状态")
                                        }
                                        logShow(
                                            mTAG,
                                            "getOrderDetail onFail== error=$error msg=$msg"
                                        )
                                    }
                                })
                        }
                    })
                }, { _emptyCallback.invoke(it) })
            }
        }, { _emptyCallback.invoke("无网络") })
    }

    //安全校验
    private fun getOrderTimeSafeLimit(safeRes: NtpUtil.SafeDifResponse) {
        if (DuoDataConfig.mLimit == null) {
            safeRes.onTimeSafe()
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            var dif = DuoDataConfig.mNtpDif
            if (dif == null) {
                //刷新网络时间
                mNtpUtil.initTimeDif {
                    DuoDataConfig.setNtpDif(it)
                    dif = it
                    logShow(mTAG, "刷新网络时差 $dif")
                }
            }
            if (dif != null) {
                //时间差计算真实时间
                try {
                    val realTime = System.currentTimeMillis() + (dif ?: 0L)
                    if (DuoDataConfig.mLimit!! - (realTime / 1000) <= 0) {
                        //超时
                        logShow(mTAG, "超时 当前真实时间=$realTime")
                        if (CloudBuilder.getShowLog()) {
                            ToastUtils.showMsgShort(CloudBuilder.getApp(), "超时")
                        }
                        //显示弹窗
                        withContext(Dispatchers.Main) {
                            val mDialog = CloudBuilder.getActMedia()?.let {
                                DialogProgressHelper().getViewInfoSure(
                                    it, it.getString(R.string.end_use), true
                                )
                            }
                            delay(3000)
                            //结束使用
                            if ((DuoDataConfig.mQuitData != null) && (DuoDataConfig.mType != null) && CloudBuilder.getIsFree()) {
                                withContext(Dispatchers.IO) {
                                    ApiQuitHelper().apiRecoveryDevice(
                                        0,
                                        DuoDataConfig.mType,
                                        DuoDataConfig.mQuitData,
                                        object : CloudHttpUtils.StrResponse {
                                            override fun onSuccess(str: String?) {
                                                mDialog?.dismiss()
                                                safeRes.onTimeout()
                                            }

                                            override fun onFailed(status: Int, msg: String) {
                                                mDialog?.dismiss()
                                                safeRes.onTimeout()
                                            }
                                        })
                                }
                                return@withContext
                            }
                            //不释放，直接退出
                            logShow(mTAG, "结束使用 $DuoDataConfig")
                            mDialog?.dismiss()
                            safeRes.onTimeout()
                        }
                        return@launch
                    }
                    logShow(mTAG, "未超时 当前真实时间=$realTime")
                } catch (e: Exception) {
                    e.printStackTrace()
                    logShow(mTAG, "时间校验出错 $dif")
                    safeRes.onTimeSafe()
                }
            } else {
                logShow(mTAG, "刷新网络时差失败 $dif")
            }
            safeRes.onTimeSafe()
        }
        logShow(mTAG, "校验 时间戳=${DuoDataConfig.mLimit}")
    }

    private fun finishAllCloudAct() {
        FilePickManager.resetAll()
        CloudBuilder.finishAll()
        if (CloudBuilder.getIsFree()) {
            CloudBuilder.getQuitSureCallback()
        } else {
            CloudBuilder.getTimeOutCallback()
        }
    }

    /**
     * 请求订单重启（类似：手机重启）
     * code == 1 返回成功onSuccess(info)；// info可能为空
     * code != 1 返回失败onFail(0,msg).
     *
     *  [重启设备](http://192.168.2.40:8081/Help/Api/POST-HWYOrder-Reboot)
     */
    fun getOrderReboot(mListener: OnStrResponse) {
        getNet({
            DdyOrderHelper.getInstance().requestOrderReboot(
                DuoDataConfig.getUser().OrderId,
                DuoDataConfig.getUser().UCID,
                "",
                "",
                object : DdyOrderContract.Callback {
                    override fun onSuccess(info: Any?) {
                        // 后台成功返回值 {"Code":1,"Msg":"等待重启中"}
                        // onSuccess，info=null, 这里直接处理 code=1 的业务
                        mListener.onSuccess("$DUO_SUCCESS_REQUEST 等待重启中...")
                        logShow(mTAG, "requestOrderReboot onSuccess== $info")
                    }

                    override fun onFail(
                        error: DdyOrderErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 设备重启")
                        }
                        logShow(mTAG, "requestOrderReboot onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed("无网络") })
    }

    /**
     * 请求订单重置（类似：手机恢复出厂设置）
     * code == 1 返回成功onSuccess(info)；// info可能为空
     * code != 1 返回失败onFail(0,msg).
     *
     *  [重置设备](http://192.168.2.40:8081/Help/Api/POST-HWYOrder-Reset)
     */
    fun getOrderReset(mListener: OnStrResponse) {
        getNet({
            DdyOrderHelper.getInstance().requestOrderReset(
                DuoDataConfig.getUser().OrderId,
                DuoDataConfig.getUser().UCID,
                "",
                "",
                object : DdyOrderContract.Callback {
                    override fun onSuccess(info: Any?) {
                        // 后台成功返回值 {"Code":1,"Msg":"等待重置中"}
                        // onSuccess，info=null, 这里直接处理 code=1 的业务
                        mListener.onSuccess("$DUO_SUCCESS_REQUEST 等待重置中...")
                        logShow(mTAG, "requestOrderReset onSuccess== $info")
                    }

                    override fun onFail(
                        error: DdyOrderErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 设备重启")
                        }
                        logShow(mTAG, "requestOrderReset onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed("无网络") })
    }

    /**
     * 订单管理相关。一般安卓等客户端，不进行相关订单重启、重置类操作，都由后端提供。
     * {@link DdyOrderHelper}
     * {@link DdyOrderErrorConstants} 错误回调时，相关错误具体枚举值
     *
     * 切换root
     */
    fun getOrderRoot(
        isRoot: Boolean,
        mListener: OnStrResponse
    ) {
        getNet({
            DdyOrderHelper.getInstance().requestOrderRoot(
                DuoDataConfig.getUser().OrderId,
                DuoDataConfig.getUser().UCID,
                "",
                "",
                isRoot,
                object : DdyOrderContract.Callback {
                    override fun onSuccess(info: Any?) {
                        mListener.onSuccess("$DUO_SUCCESS_REQUEST 切换成功,正在准备重启...")
                        logShow(mTAG, "requestOrderRoot onSuccess== $info")
                    }

                    override fun onFail(
                        error: DdyOrderErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 切换ROOT")
                        }
                        logShow(mTAG, "requestOrderRoot onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed("无网络") })
    }

    /**
     * 命令、操作类
     * {@link DdyDeviceCommandHelper} 常规命令入口
     * {@link DdyDeviceExCommandHelper} 扩展服务命令入口
     * 第一个参数ddyUserInfo{@link com.cyjh.ddy.media.bean.DdyUserInfo}来自于 {@link DdyOrderHelper}的requestOrderDetail接口。它因使用Token请求，从而具有时效性。
     * <p>
     * {@link DdyDeviceCommandContract.IApp} 应用相关接口说明
     * {@link DdyDeviceCommandContract.IBusiness} 扩展服务相关接口说明（需要商务层面开通功能后 ，才能使用）
     *
     * 上传文件
     */
    fun getFilePush(
        localPath: String,
        remotePath: String,
        data: DuoObData,
        mResponse: OnBoolResponse
    ) {
        getOrderInfo({
            val ob = ObsCert()
            ob.ak = data.AK
            ob.sk = data.SK
            ob.bucketName = data.BucketName
            ob.endPoint = data.EndPoint
            ob.securityToken = data.SecurityToken
            DdyDeviceCommandHelper.getInstance().pushFile(it, localPath, remotePath, ob,
                object : DdyDeviceCommandContract.Callback<Int?> {
                    override fun success(i: Int?) {
                        if (i != null) {
                            if (i == 1) {
                                mResponse.onSuccess(true)
                            } else {
                                mResponse.onSuccess(false)
                            }
                        } else {
                            mResponse.onFailed("$DUO_SUCCESS_EMPTY 推送文件")
                        }
                        logShow(mTAG, "pushFile onSuccess== $i")
                    }

                    override fun failuer(
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mResponse.onFailed("$DUO_ERROR_REQUEST 推送文件")
                        }
                        logShow(
                            mTAG,
                            "pushFile onFail== error=$error msg=$msg\nlocalPath=$localPath remotePath=$remotePath\nob=$ob"
                        )
                    }
                })
        }, { mResponse.onFailed(str = it) })
    }

    fun getFilePush(
        localPath: String,
        remotePath: String,
        mResponse: OnBoolResponse
    ) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance().pushFile(it, localPath, remotePath,
                object : DdyDeviceCommandContract.Callback<Int?> {
                    override fun success(i: Int?) {
                        if (i != null) {
                            if (i == 1) {
                                mResponse.onSuccess(true)
                            } else {
                                mResponse.onSuccess(false)
                            }
                        } else {
                            mResponse.onFailed("$DUO_SUCCESS_EMPTY 直接推送文件")
                        }
                        logShow(mTAG, "pushFiles onSuccess== $i")
                    }

                    override fun failuer(
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mResponse.onFailed("$DUO_ERROR_REQUEST 直接推送文件")
                        }
                        logShow(
                            mTAG,
                            "pushFiles onFail== error=$error msg=$msg\nlocalPath=$localPath remotePath=$remotePath"
                        )
                    }
                })
        }, { mResponse.onFailed(str = it) })
    }

    /**
     * 拉取文件
     * */
    private fun getFilePull(
        localPath: String,
        remotePath: String,
        data: DuoObData,
        mResponse: OnBoolResponse
    ) {
        getOrderInfo({
            val ob = ObsCert()
            ob.ak = data.AK
            ob.sk = data.SK
            ob.bucketName = data.BucketName
            ob.endPoint = data.EndPoint
            ob.securityToken = data.SecurityToken
            try {
                DdyDeviceCommandHelper.getInstance().pullFile(
                    DuoDataConfig.mOrder,
                    remotePath,
                    localPath,
                    ob,
                    object : DdyDeviceCommandContract.Callback<Int?> {
                        override fun success(i: Int?) {
                            if (i != null) {
                                if (i == 1) {
                                    mResponse.onSuccess(true)
                                } else {
                                    mResponse.onSuccess(false)
                                }
                            } else {
                                mResponse.onFailed("$DUO_SUCCESS_EMPTY 推送文件")
                            }
                            logShow(mTAG, "pullFile onSuccess== $i")
                        }

                        override fun failuer(
                            error: DdyDeviceErrorConstants?,
                            msg: String?
                        ) {
                            if (msg != null) {
                                mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                            } else {
                                mResponse.onFailed("$DUO_ERROR_REQUEST 拉取文件")
                            }
                            logShow(
                                mTAG,
                                "pullFile onFail== error=$error msg=$msg\nlocalPath=$localPath remotePath=$remotePath"
                            )
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { mResponse.onFailed(str = it) })
    }

    /**
     * 截图命令
     */
    fun getScreenShot(
        ver: Boolean?,
        mListener: OnStrResponse,
        mAct: BasePermissionActivity,
        mediaDialogHelper: MediaDialogHelper,
        permissionApplyHelper: PermissionApplyHelper
    ) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance().screencap(
                it, 720, 1280,
                object : DdyDeviceCommandContract.ScreenCap.IView {
                    override fun updateScreenCap(
                        orderid: Long,
                        imageBytes: ByteArray?
                    ) {
                        if (imageBytes != null) {
                            mediaDialogHelper.getViewBg(
                                mAct,
                                imageBytes, ver,
                                mListener,
                                permissionApplyHelper
                            )
                        } else {
                            mListener.onFailed("$DUO_SUCCESS_EMPTY 云机截屏")
                        }
                        logShow(
                            mTAG,
                            "screencap onSuccess== orderid=$orderid imageBytes=${imageBytes?.size}"
                        )
                    }

                    override fun updateScreenCapFailure(
                        orderid: Long,
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 云机截屏")
                        }
                        logShow(mTAG, "screencap onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 判断root状态
     */
    fun getRootState(mResponse: OnBoolResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .queryRootState(it, object : DdyDeviceCommandContract.Callback<Boolean> {
                    override fun success(bool: Boolean?) {
                        if (bool != null) {
                            mResponse.onSuccess(bool)
                        } else {
                            mResponse.onFailed("$DUO_SUCCESS_EMPTY 切换状态")
                        }
                        logShow(mTAG, "queryRootState onSuccess bool=$bool")
                    }

                    override fun failuer(
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mResponse.onFailed("$DUO_ERROR_REQUEST 切换状态")
                        }
                        logShow(mTAG, "queryRootState onFail== error=$error msg=$msg")
                    }
                })
        }, { mResponse.onFailed(str = it) })
    }

    /**
     * 摇一摇命令
     * */
    fun getMediaShake(mListener: OnStrResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .shake(it, object : DdyDeviceCommandContract.Callback<String?> {
                    override fun success(s: String?) {
                        if (s != null) {
                            mListener.onSuccess("摇一摇成功")
                        } else {
                            mListener.onSuccess("$DUO_SUCCESS_REQUEST 摇一摇")
                        }
                        logShow(mTAG, "shake onSuccess s=$s")
                    }

                    override fun failuer(
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 摇一摇")
                        }
                        logShow(mTAG, "shake onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 云机调用扫一扫
     * */
    fun getMediaScan(mListener: OnStrResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .scan(it, object : DdyDeviceCommandContract.Callback<String?> {
                    override fun success(s: String?) {
                        if (s != null) {
                            mListener.onSuccess("识别跳转")
                        } else {
                            mListener.onSuccess("$DUO_SUCCESS_REQUEST 扫一扫")
                        }
                        logShow(mTAG, "scan onSuccess s=$s")
                    }

                    override fun failuer(
                        error: DdyDeviceErrorConstants?,
                        msg: String?
                    ) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 扫一扫")
                        }
                        logShow(mTAG, "scan onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 云机二维码扫描
     * */
    fun getMediaCode(mListener: OnStrResponse, qrContent: String) {
        getOrderInfo({
            //展示手机的图片，识别选中图片的二维码上传二维码信息，无法识别直接上传文件
            DdyDeviceCommandHelper.getInstance()
                .qrcode(it, qrContent, object : DdyDeviceCommandContract.Callback<String> {
                    override fun success(s: String?) {
                        if (s != null) {
                            mListener.onSuccess("识别码")
                        } else {
                            mListener.onSuccess("$DUO_SUCCESS_REQUEST 二维码识别")
                        }
                        logShow(mTAG, "qrcode onSuccess s=$s")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 二维码识别")
                        }
                        logShow(mTAG, "qrcode onFail== error=$error msg=$msg")
                        //DdyDeviceCommandHelper.getInstance().photograph();
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 展示手机的图片和视频调用此方法上传
     * */
    fun getMediaPhoto(
        localPath: String,
        remotePath: String,
        obs: ObsCert?,
        mListener: OnStrResponse
    ) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .photograph(it, localPath, remotePath, obs, object :
                    DdyDeviceCommandContract.Callback<String> {
                    override fun success(s: String?) {
                        if (s != null) {
                            mListener.onSuccess(s)
                        } else {
                            mListener.onSuccess("$DUO_SUCCESS_REQUEST 上传媒体文件")
                        }
                        logShow(mTAG, "photograph onSuccess s=$s")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 上传媒体文件")
                        }
                        logShow(mTAG, "photograph onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 云手机内部执行：am start -a android.intent.action.VIEW -d http://www.baidu.com
     * */
    fun getActionStart(action: String?, data: String?, mListener: OnStrResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance().amStartApp(
                it, action, data,
                object : DdyDeviceCommandContract.Callback<String> {
                    override fun success(s: String?) {
                        if (s != null) {
                            mListener.onSuccess("识别成功")
                        } else {
                            mListener.onSuccess("$DUO_SUCCESS_REQUEST 跳转...")
                        }
                        logShow(mTAG, "amStartApp onSuccess s=$s")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST 跳转")
                        }
                        logShow(mTAG, "amStartApp onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 获取安装应用
     * */
    fun getAppsInfo() {
        DdyDeviceCommandHelper.getInstance()
            .getAppsInfo(
                DuoDataConfig.mOrder,
                object : DdyDeviceCommandContract.Callback<List<AppInfo>> {
                    override fun success(appInfos: List<AppInfo>) {
                        var outName = ""
                        for (appInfo: AppInfo in appInfos) {
                            outName += appInfo.name + ";'"
                        }
                        logShow(mTAG, "getAppsInfo onSuccess outName=$outName")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        logShow(mTAG, "getAppsInfo onFail== error=$error msg=$msg")
                    }
                })
    }

    /**
     * 获取安装进度
     * */
    fun getAppInstallState(pkgName: String, mResponse: OnStrResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .getInstallState(it, pkgName,
                    object : DdyDeviceCommandContract.Callback<String> {
                        override fun success(str: String?) {
                            if (str != null) {
                                mResponse.onSuccess(str)
                            } else {
                                mResponse.onFailed("$DUO_SUCCESS_EMPTY 安装中")
                            }
                            logShow(mTAG, "getInstallState str=$str")
                        }

                        override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                            if (msg != null) {
                                mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                            } else {
                                mResponse.onFailed("$DUO_ERROR_REQUEST 安装中")
                            }
                            logShow(mTAG, "getInstallState onFail== error=$error msg=$msg")
                        }
                    })
        }, { mResponse.onFailed(str = it) })
    }

    /**
     * 启动应用
     * */
    fun getAppStart(pkg: String, act: String, mListener: OnStrResponse) {
        getOrderInfo({
            val strExtras: Map<String, String> = HashMap()
            DdyDeviceCommandHelper.getInstance()
                .runApp(it, pkg, act, strExtras, object : DdyDeviceCommandContract.Callback<Int> {
                    override fun success(i: Int?) {
                        mListener.onSuccess("okk")
                        logShow(mTAG, "runApp onSuccess i=$i")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        mListener.onFailed("noo")
                        logShow(mTAG, "runApp onFail== error=$error msg=$msg")
                    }
                })
        }, { mListener.onFailed(str = it) })
    }

    fun getAppsClear(pkgs: ArrayList<String>, mResponse: OnStrResponse) {
        getOrderInfo({
            DdyDeviceCommandHelper.getInstance()
                .clearApps(it, pkgs,
                    object : DdyDeviceCommandContract.Callback<String> {
                        override fun success(str: String?) {
                            if (str != null) {
                                mResponse.onSuccess(str)
                            } else {
                                mResponse.onFailed(DUO_SUCCESS_EMPTY)
                            }
                            logShow(mTAG, "clearApps str=$str")
                        }

                        override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                            if (msg != null) {
                                mResponse.onFailed("$DUO_ERROR_REQUEST $msg")
                            } else {
                                mResponse.onFailed(DUO_ERROR_REQUEST)
                            }
                            logShow(mTAG, "clearApps onFail== error=$error msg=$msg")
                        }
                    })
        }, { mResponse.onFailed(str = it) })
    }

    fun getRunAppLogin(
        pkg: String,
        act: String,
        userToken: String,
        savePath: ArrayList<String>,
        saveMode: Int,
        mListener: OnStrResponse
    ) {
        getOrderInfo({
            val strExtras: Map<String, String> = HashMap()
            DdyDeviceCommandHelper.getInstance()
                .runAppSave(
                    it, pkg, act, strExtras,
                    userToken, savePath, saveMode,
                    object : DdyDeviceCommandContract.Callback<Int> {
                        override fun success(i: Int?) {
                            mListener.onSuccess("okk")
                            logShow(mTAG, "runAppSave onSuccess i=$i")
                        }

                        override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                            mListener.onFailed("noo")
                            logShow(mTAG, "runAppSave onFail== error=$error msg=$msg")
                        }
                    })
        }, { mListener.onFailed(str = it) })
    }

    /**
     * 安装应用
     * */
    fun getInstallApp(
        url: String?,
        pkgName: String,
        actName: String,
        runAfter: Boolean,
        mResponse: OnBoolResponse
    ) {
        getOrderInfo({
            val strExtras: Map<String, String> = HashMap()
            DdyDeviceCommandHelper.getInstance().installApp(
                it, url, pkgName, actName, runAfter, strExtras,
                object : DdyDeviceCommandContract.Callback<Int> {
                    override fun success(i: Int?) {
                        if (i != null) {
                            mResponse.onSuccess(i == 1)
                        } else {
                            mResponse.onFailed("$DUO_SUCCESS_EMPTY 安装")
                        }
                        logShow(mTAG, "installApp onSuccess i=$i")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        if (msg != null) {
                            mResponse.onFailed(msg)
                        } else {
                            mResponse.onFailed("失败")
                        }
                        logShow(mTAG, "installApp onFail== error=$error msg=$msg")
                    }
                })
        }, { mResponse.onFailed(str = it) })
    }

    //粘贴真机剪切板内容
    fun getClip(mListener: OnStrResponse, cxt: Context) {
        val clip: ClipboardManager =
            cxt.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clip.hasPrimaryClip()) {
            val item = clip.primaryClip?.getItemAt(0)?.text
            if (!item.isNullOrEmpty()) {
                DdyDeviceCommandHelper.getInstance()
                    .setClipBoard(
                        DuoDataConfig.mOrder,
                        item.toString(),
                        object : DdyDeviceCommandContract.Callback<Int> {
                            override fun success(i: Int?) {
                                if (i != null) {
                                    mListener.onSuccess("已获取 复制内容")
                                } else {
                                    mListener.onSuccess("$DUO_SUCCESS_REQUEST 粘贴板")
                                }
                                logShow(mTAG, "setClipBoard onSuccess Int=$i")
                            }

                            override fun failuer(
                                error: DdyDeviceErrorConstants?,
                                msg: String?
                            ) {
                                if (msg != null) {
                                    mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                                } else {
                                    mListener.onFailed("$DUO_ERROR_REQUEST 粘贴板")
                                }
                                logShow(mTAG, "setClipBoard onFail== error=$error msg=$msg")
                            }
                        })
            } else {
                mListener.onFailed("内容为空")
            }
        } else {
            mListener.onFailed("当前无内容")
        }
    }

    fun getScreenUrl(orderInfo: DdyOrderInfo, mListener: OnStrResponse) {
        DdyDeviceCommandHelper.getInstance()
            .screenCapUrl(orderInfo, 720,
                1280, false, object : DdyDeviceCommandContract.Callback<String> {
                    override fun success(str: String?) {
                        if (str != null) {
                            mListener.onSuccess(str)
                        } else {
                            mListener.onFailed("$DUO_SUCCESS_REQUEST help")
                        }
                        logShow(info = "screenCapUrl=$str")
                    }

                    override fun failuer(error: DdyDeviceErrorConstants?, msg: String?) {
                        if (msg != null) {
                            mListener.onFailed("$DUO_ERROR_REQUEST $msg")
                        } else {
                            mListener.onFailed("$DUO_ERROR_REQUEST help")
                        }
                        logShow(mTAG, "screenCapUrl onFail== error=$error msg=$msg")
                    }
                })
    }

    fun getTest() {
        /*        DdyDeviceCommandHelper.getInstance().amBroadcast()
                DdyDeviceCommandHelper.getInstance().amStartService()
                DdyDeviceCommandHelper.getInstance().addNotifyPackages()
                DdyDeviceCommandHelper.getInstance().installSysEnvironment()
                DdyDeviceCommandHelper.getInstance().screenshot()*/

    }
}