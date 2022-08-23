package com.cloud.duolib.model.helper

import android.content.Context
import android.content.Intent
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.model.util.logShow
import com.cyjh.ddy.thirdlib.lib_hwobs.UploadApkInfo
import com.cyjh.ddysdk.ddyobs.ObsContract
import com.cyjh.ddysdk.ddyobs.ObsFileHelper

class PushObsHelper(private val cxt: Context) {
    private var startPos = 0
    private var failSize = 0
    private var startList: ArrayList<BeanFile>? = null

    //正在执行的id
    private var uploadingId: Long = 0

    //上传成功的信息
    private var uploadApkInfo: UploadApkInfo? = null

    //上传成功未同步的id
    private var fileId: String? = null

    //成功后需要同步的id
    private val fileIds = ArrayList<String>()

    private fun startNext(fail: Boolean, ucid: String) {
        startPos++
        if (fail) failSize++
        try {
            //当前为最后一个
            if (startPos == (startList?.size ?: 1)) {
                //发送全部上传完毕广播
                val info = if (failSize != 0) {
                    "${failSize}个失败，${startPos - failSize}个文件上传成功\n即将为您打开安装APP列表"
                } else {
                    "${startPos}个文件全部上传成功\n即将为您打开安装APP列表"
                }
                sendProgress(false, info)
            } else {
                //未遍历完全部，继续
                sendProgress(true, "正在上传...\n第${startPos}个文件")
                startList?.get(startPos)?.let { upload(it, ucid) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logShow(info = "startNext=$e")
        }
    }

    fun sendProgress(start: Boolean, info: String) {
        var intent = Intent(BROADCAST_FILE_UPLOAD_START)
        if (!start) {
            /*全部完成需要上传管理
            * "com.cyjh.filemanager","com.cyjh.filemanager.activity.filemanager.FileActivity"
            * */
            intent = Intent(BROADCAST_FILE_UPLOAD_END)
        }
        intent.putExtra(RECEIVER_FILE_UPLOAD, info)
        intent.setPackage(cxt.packageName)
        cxt.sendBroadcast(intent)
    }

    fun upload(item: BeanFile, ucid: String) {
        uploadingId = ObsFileHelper.getInstance()
            .uploadFile(item.path, ucid, object : ObsContract.UploadCallback<UploadApkInfo> {
                override fun onSuccess(apkInfo: UploadApkInfo) {
                    //上传成功后，还要同步，同步完成才是真的上传成功，定时查询同步状态
                    fileId = apkInfo.fileId
                    fileIds.add(apkInfo.fileId)
                    uploadApkInfo = apkInfo
                    //startSyncInfoTimer(ucid)
                    startNext(false, ucid)
                }

                override fun onFail(taskID: Long, i: Int, s: String) {
                    uploadingId = 0
                    logShow(info = "第${startPos}个文件上传失败，$i=$s")
                    startNext(true, ucid)
                }

                override fun onCancel(taskID: Long) {
                    uploadingId = 0
                    startNext(true, ucid)
                }
            })
    }

    companion object {
        const val BROADCAST_FILE_UPLOAD_START = "com.cloud.duolib.RECEIVER_UPLOAD_START"
        const val BROADCAST_FILE_UPLOAD_END = "com.cloud.duolib.RECEIVER_UPLOAD_END"
        const val RECEIVER_FILE_UPLOAD = "RECEIVER_FILE_UPLOAD"//obsHelper方式
    }
}