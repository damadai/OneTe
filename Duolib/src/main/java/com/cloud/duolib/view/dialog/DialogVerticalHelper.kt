package com.cloud.duolib.view.dialog

import android.app.Activity
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.view.View
import com.cloud.duolib.R
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.model.OnBaseSucResponse
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.helper.MediaDialogHelper
import com.cloud.duolib.model.helper.PermissionApplyHelper
import com.cloud.duolib.model.util.OsTimeUtils
import com.cloud.duolib.model.util.getQQGroup
import com.cloud.duolib.model.util.getStrClip
import com.cloud.duolib.model.util.getToast

class DialogVerticalHelper {
    //弹窗确认（root切换,点击退出
    fun getViewClickSure(
        mAct: Activity,
        sureListener: View.OnClickListener
    ): CommonVerticalDialog? {
        var mDialog: CommonVerticalDialog? = null
        try {
            //创建弹窗
            mDialog = CommonVerticalDialog(mAct, true)
            //隐藏图标
            mDialog.hideImg()
            //显示标题
            mDialog.setMsg(mAct.getString(R.string.quit_sure), null, null)
            //显示信息流
            mDialog.setContentNative(mAct)
            //显示按钮
            mDialog.setConfirmListener {
                mDialog.dismiss()
                sureListener.onClick(it)
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
        return mDialog
    }

    //弹窗确认（权限请求
    fun getViewPermission(
        mAct: Activity,
        titleInfo: String,
        logoRes: Int?,
        contentInfo: String,
        sureListener: View.OnClickListener,
        denyListener: View.OnClickListener?
    ) {
        try {
            //创建弹窗
            val mDialog = CommonVerticalDialog(mAct, false)
            //是否为拒绝后跳转
            var sureBtnInfo = mAct.getString(R.string.permission_open_set)
            var denyBtnInfo = mAct.getString(R.string.still_refuse)
            if (logoRes != null) {
                mDialog.setImg(logoRes)
                sureBtnInfo = mAct.getString(R.string.agree)
                denyBtnInfo = mAct.getString(R.string.refuse)
            } else {
                mDialog.hideImg()
            }
            //设置标题
            mDialog.setMsg(null, titleInfo, contentInfo)
            //设置按钮
            mDialog.setBtn(sureBtnInfo, denyBtnInfo)
            mDialog.setConfirmListener(sureListener)
            mDialog.setCancelListener(denyListener)
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }

    //弹窗确认（权限截图
    fun getViewBg(
        mAct: BasePermissionActivity,
        imageBytes: ByteArray,
        ver: Boolean?,
        mListener: OnStrResponse,
        mediaDialogHelper: MediaDialogHelper,
        permissionApplyHelper: PermissionApplyHelper
    ) {
        try {
            //创建弹窗
            val mDialog = CommonVerticalDialog(mAct, true)
            //背景图片
            mDialog.setImg(mAct, imageBytes, ver)
            //显示标题
            mDialog.setMsg(mAct.getString(R.string.viewScreenShot), null, null)
            //显示按钮
            mDialog.setBtn(mAct.getString(R.string.save), null)
            mDialog.setConfirmListener {
                permissionApplyHelper.applyPermissionStorage(
                    mAct,
                    object : OnBaseSucResponse {
                        override fun onSuccess() {
                            //okk
                            val file = MediaStore.Images.Media.insertImage(
                                mAct.contentResolver,
                                BitmapFactory.decodeByteArray(
                                    imageBytes,
                                    0,
                                    imageBytes.size
                                ),
                                OsTimeUtils.getCurrentTime2(), mAct.getString(R.string.shot_des)
                            )
                            if (!file.isNullOrEmpty()) {
                                mListener.onSuccess(mAct.getString(R.string.save_dir))
                                //隐藏自身
                                if (!mAct.isDestroyed) {
                                    mDialog.dismiss()
                                }
                            } else {
                                mListener.onFailed(mAct.getString(R.string.save_fail))
                            }
                        }
                    }, mediaDialogHelper, false
                )
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }

    //弹窗确认（跳转客服
    fun getViewFeedback(mAct: Activity, number: String?, group: String?) {
        try {
            //创建弹窗
            val mDialog = CommonVerticalDialog(mAct, true)
            mDialog.setMsg(
                mAct.getString(R.string.helperCenter),
                null,
                mAct.getString(R.string.otherQueGroup, number)
            )
            mDialog.hideImg()
            mDialog.setBtn(mAct.getString(R.string.clickAdd), null)
            mDialog.setConfirmListener {
                if (!group.isNullOrEmpty()) {
                    if (getQQGroup(mAct, group)) {
                        return@setConfirmListener
                    }
                }
                if (!number.isNullOrEmpty()) {
                    getStrClip(mAct, number)
                    getToast(mAct, mAct.getString(R.string.clipGroupSuc, number))
                } else {
                    getToast(mAct, R.string.tellEmil)
                }
            }
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }

    //弹窗确认（跳转排队
    fun getViewFeedLine(
        mAct: Activity,
        titleInfo: String,
        sureBtn: String,
        listener: View.OnClickListener
    ) {
        try {
            //创建弹窗
            val mDialog = CommonVerticalDialog(mAct, false)
            mDialog.setMsg("云手机异常", null, titleInfo)
            mDialog.hideImg()
            mDialog.setBtn(sureBtn, null)
            mDialog.setConfirmListener(listener)
            mDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            getToast(mAct, R.string.cxt_load)
        }
    }
}