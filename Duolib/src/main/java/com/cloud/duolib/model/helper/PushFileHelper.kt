package com.cloud.duolib.model.helper

import android.content.Context
import android.content.Intent
import com.cloud.duolib.bean.duo.PushStatusType
import com.cloud.duolib.model.util.BROADCAST_FILE_PUSH_END
import com.cloud.duolib.model.util.BROADCAST_FILE_PUSH_START
import com.cloud.duolib.model.util.RECEIVER_FILE_PUSH_FILE_PATH
import com.cloud.duolib.model.util.RECEIVER_FILE_PUSH_FILE_STATUS

/**
 * 发送广播
 * */
class PushFileHelper {
    //状态广播
    fun sendStatusRefresh(mCxt: Context, status: PushStatusType, path: String) {
        val intent = Intent(BROADCAST_FILE_PUSH_START)
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_STATUS, status)
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_PATH, path)
        intent.setPackage(mCxt.packageName)
        mCxt.sendBroadcast(intent)
    }

    //状态广播
    fun sendStatusRefresh(mCxt: Context, status: String, path: String) {
        val intent = Intent(BROADCAST_FILE_PUSH_START)
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_STATUS, status)
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_PATH, path)
        intent.setPackage(mCxt.packageName)
        mCxt.sendBroadcast(intent)
    }

    //结束广播
    fun sendStatusEnd(mCxt: Context, endMsg: String, openLast: String?) {
        val intent = Intent(BROADCAST_FILE_PUSH_END)
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_STATUS, endMsg)
        //是否存在仅非应用文件
        intent.putExtra(RECEIVER_FILE_PUSH_FILE_PATH, openLast)
        intent.setPackage(mCxt.packageName)
        mCxt.sendBroadcast(intent)
    }
}