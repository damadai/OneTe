package com.cloud.duolib.model.helper

import android.text.TextUtils
import android.view.KeyEvent
import android.view.ViewGroup
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.model.OnBaseSucResponse
import com.cloud.duolib.model.util.*
import com.cyjh.ddy.media.bean.DdyUserInfo
import com.cyjh.ddy.media.media.ActionCode
import com.cyjh.ddy.media.media.listener.IHwySDKListener
import com.cyjh.ddysdk.device.base.constants.DdyDeviceErrorConstants
import com.cyjh.ddysdk.device.camera.DdyDeviceCameraWebrtcHelper
import com.cyjh.ddysdk.device.command.DdyDeviceCommandHelper
import com.cyjh.ddysdk.device.media.DdyDeviceMediaContract
import com.cyjh.ddysdk.device.media.DdyDeviceMediaHelper
import com.cyjh.ddysdk.order.base.bean.NoticeSyncInfo
import com.cyjh.ddysdk.order.base.bean.OrderInfoRespone
import com.cyjh.ddysdk.order.base.constants.DdyOrderErrorConstants

/**
 * 流媒体相关
 * [DdyDeviceMediaHelper]
 * [IHwySDKListener]  媒体库的回调函数
 * [ActionCode]   媒体库的关键事件回调枚举值1
 * [DdyDeviceErrorConstants] 回调错误时，具体错误枚举值
 */
class MediaResumeHelper(private val mTag: String) {
    //媒体流交互
    private var dMediaHelper: DdyDeviceMediaHelper? = null

    fun initMediaResume(
        mAct: BasePermissionActivity,
        contentView: ViewGroup,
        ddyUserInfo: DdyUserInfo,
        mCommandHelper: DuoCommandHelper,
        mediaConfigHelper: MediaConfigHelper,
        mediaDialogHelper: MediaDialogHelper,
        permissionApplyHelper: PermissionApplyHelper,
        _reInitKeyCallback: () -> Unit,
        _photoChangeCallback: () -> Unit
    ) {
        dMediaHelper = DdyDeviceMediaHelper(mAct)
        // 是否开启语音功能
        dMediaHelper?.switchVoice(true)
        // 步骤一：设置用户信息
        initHwyManager(
            contentView,
            dMediaHelper,
            ddyUserInfo,
            mediaConfigHelper,
            _reInitKeyCallback
        )
        //该步骤非必要。具体看商务合作是否有相关业务。
        initBusinessProcess(
            mCommandHelper,
            mAct,
            dMediaHelper,
            mediaDialogHelper,
            mediaConfigHelper,
            _photoChangeCallback,
            permissionApplyHelper
        )
        // 步骤二：设置基本连接信息
        startPlay(ddyUserInfo, dMediaHelper, _reInitKeyCallback)
    }

    fun stopMediaCamera() {
        try {
            dMediaHelper?.uninit()
            DdyDeviceCameraWebrtcHelper.getInstance().stop()
        } catch (e: Exception) {
            e.printStackTrace()
            CloudBuilder.getUMCallBack(UM_DUO_MEDIA_PAUSE_FAIL, "e=$e")
        }
    }

    fun destroyMediaCommand() {
        dMediaHelper = null
        DdyDeviceCommandHelper.getInstance().uninit()
    }

    fun getKeyBack(oid: Long) {
        dMediaHelper?.doKeyEvent(oid, KeyEvent.KEYCODE_BACK)
    }

    fun getKeyHome(oid: Long) {
        dMediaHelper?.doKeyEvent(oid, KeyEvent.KEYCODE_HOME)
    }

    fun getKeySwitch(oid: Long) {
        dMediaHelper?.doKeyEvent(oid, KeyEvent.KEYCODE_APP_SWITCH)
    }

    private fun initHwyManager(
        contentView: ViewGroup,
        ddyDeviceMediaHelper: DdyDeviceMediaHelper?,
        ddyUserInfo: DdyUserInfo,
        mediaConfigHelper: MediaConfigHelper,
        _reInitKeyCallback: () -> Unit
    ) {
        ddyDeviceMediaHelper?.init(ddyUserInfo, "", "", "", object : IHwySDKListener {
            /**
             * action回调
             * @param code 事件码  [ActionCode]
             * @param value
             */
            override fun actionCodeCallback(code: Int, value: String) {
                // 出错了，需要退出可视化
                when {
                    ActionCode.isErrExitAction(code) -> {
                        logShow(mTag, "ErrExit出错了: $code=$value")
                        _reInitKeyCallback.invoke()
                        CloudBuilder.getUMCallBack(UM_DUO_MEDIA_FAIL, "$code=$value")
                    }
                    ActionCode.isMediaRefuseAction(code) -> {
                        logShow(mTag, "MediaRefuse出错了: $code=$value")
                    }
                    ActionCode.isCtrlRefuseAction(code) -> {
                        logShow(mTag, "CtrlRefuse出错了: $code=$value")
                    }
                }
            }

            /**
             * 显示帧率fps
             * @param fps
             */
            override fun upFps(fps: String) {
                //Log.i(mTag, "upFps() fps:$fps")
            }

            /**
             * 显示ping值
             * @param pingRtt
             */
            override fun upPing(pingRtt: String) {
                //Log.i(mTag, "upPing() pingRtt:$pingRtt")
                if (mediaConfigHelper.isCamera) {
                    val pingShow = DuoKeyHelper.KeyPingShow(pingRtt)
                    val pingString =
                        pingShow.avg!!.substring(0, pingShow.avg!!.indexOf("."))
                    logShow(mTag, "playMedia upPing() pingRtt:$pingRtt")
                    //DdyDeviceCameraWebrtcHelper.getInstance().upMediaPing(Integer.valueOf(pingString))
                }
            }

            /**
             * 手机内部切换视频时通知外部
             * @param rotate 2 横屏； 1 竖屏
             */
            override fun autoRotateScreen(rotate: Int) {
                logShow(mTag, "autoRotateScreen() value:${if (rotate == 1) "竖屏" else "横屏"}")
            }

            override fun upClipboard(s: String) {
                //云机有复制粘贴板
                logShow(mTag, "upClipboard() value:$s")
            }

            override fun upNoticeSyncInfo(noticeSyncInfo: NoticeSyncInfo) {
                logShow(
                    mTag,
                    "upNoticeSyncInfo() noticeSyncInfo:$noticeSyncInfo"
                )
            }

            override fun upTraffic(l: Long, l1: Long) {}
            override fun upFrameTime(l: Long) {}
        }, contentView, false)?.let { res ->
            logShow(mTag, "initHwyManager=$res")
            if (!res) {
                //界面初始化错误
                _reInitKeyCallback.invoke()
                logShow(mTag, "初始化initHwyManager失败=$res")
            }
        }
    }

    private fun initBusinessProcess(
        mCommandHelper: DuoCommandHelper,
        mAct: BasePermissionActivity,
        ddyDeviceMediaHelper: DdyDeviceMediaHelper?,
        mediaDialogHelper: MediaDialogHelper,
        mediaConfigHelper: MediaConfigHelper,
        _photoChangeCallback: () -> Unit,
        permissionApplyHelper: PermissionApplyHelper
    ) {
        /**
         * 这块仅有集成我们现有的扩展服务（如一键新机，定位，摇一摇等服务的情况下，才需要使用）时，才需要。否则直接忽略。
         * PS:该服务商务开通后，可配置生效，进行对接。
         */
        //当云手机中的某服务，需要购买时，会请求该接口。
        //http://sdk.ddyun.com/Help/Api/POST-api-OrderExtServices ，渠道可通过该接口，指定某订单具有某服务。
        ddyDeviceMediaHelper?.addProcessRequest("toDDYBuy") { s ->
            getToast(mAct, FAIL_CLOUD_DUO_UN_SUPPORT)
            logShow(mTag, "toDDYBuy:$s")
        }

        //当云手机里请求拨打电话时
        ddyDeviceMediaHelper?.addProcessRequest("phone") { s ->
            getToast(mAct, FAIL_CLOUD_DUO_UN_SUPPORT)
            logShow(mTag, "phone:$s")
        }

        //当云手机里请求发送短信时
        ddyDeviceMediaHelper?.addProcessRequest("message") { s ->
            getToast(mAct, FAIL_CLOUD_DUO_UN_SUPPORT)
            logShow(mTag, "message:$s")
        }

        //当云手机里触发使用相机时
        ddyDeviceMediaHelper?.addProcessRequest("scan") { s ->
            //如果没有对应扫一扫的环境，则需要先准备好其环境后，才能继续下一步
            //渠道这边一般情况是：该扫一扫环境均已预置。如果是要兼容之前的云手机，才需要考虑这块流程。
            //下一步：外部通知云手机开启选择界面
            logShow(mTag, "scan:$s")
            mCommandHelper.getMediaScan(mediaConfigHelper.mDuoResponse(mAct))
        }

        //当云手机里选择二维码图片上传
        ddyDeviceMediaHelper?.addProcessRequest("openCodeImg") { s ->
            logShow(mTag, "openCodeImg:$s")
            mCommandHelper.getMediaCode(mediaConfigHelper.mDuoResponse(mAct), s)
        }

        //当云手机里选择媒体文件上传
        ddyDeviceMediaHelper?.addProcessRequest("photograph") { s ->
            logShow(mTag, "photograph:$s")
            //DuoManager.getMediaPhoto("", "", null, mDuoResponse)
            _photoChangeCallback.invoke()
        }

        //视频流相机
        ddyDeviceMediaHelper?.addProcessRequest("openCamera") { s ->
            mediaConfigHelper.mJson = s
            permissionApplyHelper.applyPermissionCamera(
                mAct,
                object : OnBaseSucResponse {
                    override fun onSuccess() {
                        mCommandHelper.getOrderInfo({data->
                            DdyDeviceCameraWebrtcHelper.getInstance()
                                .start(data as? OrderInfoRespone, mAct,mediaConfigHelper.mJson)
                        }, {})
                    }
                }, mediaDialogHelper
            )
            mediaConfigHelper.isCamera = true
        }
        //视频流关闭相机
        ddyDeviceMediaHelper?.addProcessRequest("closeCamera") { s ->
            logShow(mTag, "closeCamera:$s")
            DdyDeviceCameraWebrtcHelper.getInstance().stop()
            mediaConfigHelper.isCamera = false
        }
        ddyDeviceMediaHelper?.addProcessRequest("changeCamera") {s->
            logShow(mTag, "changeCamera:$s")
            DdyDeviceCameraWebrtcHelper.getInstance().change()
        }
        //当云手机里请求摇一摇
        ddyDeviceMediaHelper?.addProcessRequest("shake") { s ->
            logShow(mTag, "shake:$s")
            //让云手机去响应摇一摇，调用：
            mCommandHelper.getMediaShake(mediaConfigHelper.mDuoResponse(mAct)) //也可以主动调用
        }
        //当云手机里请求切换Root/非Root
        ddyDeviceMediaHelper?.addProcessRequest("nroot") { s ->
            //让云手机去切换Root、非Root，调用：
            logShow(mTag, "nroot:$s")
            getToast(mAct, FAIL_CLOUD_DUO_UN_SUPPORT)
            //也可以主动调用
        }
        /*
         * 收到权限消息处理录音权限 或 应用预先给录音权限，根据需要选择是否处理start和close消息
         */
        ddyDeviceMediaHelper?.addProcessRequest("voiceEvent") { data ->
/*            *//*if (TextUtils.equals("start", data)) {// 按下说话，开始录音的消息

                } else if (TextUtils.equals("close", data)) {// 手指抬起，停止录音的消息

                } else */
            logShow(mTag, "voiceEvent:$data")
            if (TextUtils.equals("permission", data)) { // 处理录音权限
                permissionApplyHelper.applyPermissionRecord(mAct, mediaDialogHelper)
            }
        }
        ddyDeviceMediaHelper?.addProcessRequest("test") { data ->
            logShow(mTag, "addProcessRequest test=$data")
            getToast(mAct, "addProcessRequest test=$data")
        }
    }

    private fun startPlay(
        mData: DdyUserInfo,
        ddyDeviceMediaHelper: DdyDeviceMediaHelper?,
        _reInitKeyCallback: () -> Unit
    ) {
        // 步骤三 ：设置码率，然后playMedia;第二个参数是H265的码率。如果该服务未申请开通，可空。
        ddyDeviceMediaHelper?.setPullStreamRate("1500", "1500")
        // 步骤四：增加断网重连
        ddyDeviceMediaHelper?.setReConnect(true)
        //start
        ddyDeviceMediaHelper?.playMedia(
            mData.OrderId,
            mData.UCID,
            "",// 步骤二：设置基本连接信息;默认可空。（云游戏渠道有监控设备状态回调的话，可以填写该参数。回调时会回发该参数）
            object : DdyDeviceMediaContract.Callback {
                override fun success(obj: Any) {
                    logShow(mTag, "playMedia onSuccess obj=${obj}")
                }

                /**
                 * @param errcode
                 * @param msg 当出现DDE_DOE_ERROR时，具体错误可参考[DdyOrderErrorConstants]的定义
                 * 这里一般是网络错误，或填写参数错误，或Token失效等错误
                 */
                override fun failuer(
                    errcode: DdyDeviceErrorConstants,
                    msg: String
                ) {
                    val info = "$errcode=$msg"
                    logShow(mTag, info)
                    //网络不稳定?
                    _reInitKeyCallback.invoke()
                }
            })
    }
}