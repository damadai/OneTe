package com.cloud.duolib.model.helper

import android.Manifest
import com.cloud.duolib.R
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.model.OnBaseSucResponse
import com.cloud.duolib.model.util.getToast

class PermissionApplyHelper {
    //录音权限申请
    fun applyPermissionRecord(bAct: BasePermissionActivity, mediaDialogHelper: MediaDialogHelper) {
        val perListener = object : BasePermissionActivity.OnPermissionRequestListener {
            override fun onPermissionGranted() {
                //okk
                getToast(bAct, R.string.get_retry)
            }

            override fun onPermissionDenied(showToast: Boolean) {
                //noo
                if (showToast) {
                    getToast(bAct, R.string.permission_record)
                }
            }
        }
        //权限判断
        bAct.checkPermissions(
            mediaDialogHelper,
            listOf(Manifest.permission.RECORD_AUDIO),
            bAct.packageName,
            R.mipmap.ic_audio_210,
            bAct.resources.getString(R.string.record),
            bAct.resources.getString(R.string.describe_record),
            perListener
        )
    }

    //相机权限申请
    fun applyPermissionCamera(
        bAct: BasePermissionActivity,
        listener: OnBaseSucResponse,
        mediaDialogHelper: MediaDialogHelper
    ) {
        val perListener = object : BasePermissionActivity.OnPermissionRequestListener {
            override fun onPermissionGranted() {
                //okk
                listener.onSuccess()
            }

            override fun onPermissionDenied(showToast: Boolean) {
                //noo
                if (showToast) {
                    getToast(bAct, R.string.permission_camera)
                }
            }
        }
        //权限判断
        bAct.checkPermissions(
            mediaDialogHelper,
            listOf(
                Manifest.permission.CAMERA
            ),
            bAct.packageName,
            R.mipmap.ic_camera_210,
            bAct.resources.getString(R.string.camera),
            bAct.resources.getString(R.string.describe_camera),
            perListener
        )
    }

    //存储权限申请
    fun applyPermissionStorage(
        bAct: BasePermissionActivity,
        listener: OnBaseSucResponse, mediaDialogHelper: MediaDialogHelper, scanApp: Boolean
    ) {
        val perListener = object : BasePermissionActivity.OnPermissionRequestListener {
            override fun onPermissionGranted() {
                //okk
                listener.onSuccess()
            }

            override fun onPermissionDenied(showToast: Boolean) {
                //noo
                if (showToast) {
                    getToast(bAct, R.string.permission_storage)
                }
            }
        }
        //权限判断
        bAct.checkPermissions(
            mediaDialogHelper,
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            bAct.packageName,
            R.mipmap.ic_storage_210,
            if (scanApp)  bAct.resources.getString(R.string.storage_scan) else  bAct.resources.getString(R.string.storage_save),
            if (scanApp) bAct.resources.getString(R.string.describe_storage_scan) else bAct.resources.getString(
                R.string.describe_storage_save
            ),
            perListener
        )
    }
}