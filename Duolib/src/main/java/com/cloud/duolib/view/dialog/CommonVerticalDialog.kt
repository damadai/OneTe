package com.cloud.duolib.view.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.cloud.capture.DisplayUtil
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R

class CommonVerticalDialog(context: Context, private val cancelAble: Boolean) :
    Dialog(context, R.style.Theme_Corner_Dialog) {
    private var mView: View? = null
    private var mConfirmListener: View.OnClickListener? = null
    private var mCancelListener: View.OnClickListener? = null

    init {
        mView = LayoutInflater.from(context).inflate(R.layout.duo_dialog_vertical, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(cancelAble)
        setContentView(mView!!)
        mView?.findViewById<TextView>(R.id.tvDeny_ver)?.setOnClickListener {
            dismiss()
            mCancelListener?.onClick(it)
        }
        mView?.findViewById<Button>(R.id.btSure_ver)?.setOnClickListener {
            dismiss()
            mConfirmListener?.onClick(it)
        }
    }

    fun setMsg(strTop: String?, strTitle: String?, strTip: String?) {
        mView?.let {
            if (!strTop.isNullOrEmpty()) {
                mView?.findViewById<TextView>(R.id.tvTop_ver)?.text = strTop
            } else {
                mView?.findViewById<TextView>(R.id.tvTop_ver)?.isVisible = false
            }
            if (!strTitle.isNullOrEmpty()) {
                mView?.findViewById<TextView>(R.id.tvTitle_ver)?.text = strTitle
            } else {
                mView?.findViewById<TextView>(R.id.tvTitle_ver)?.isVisible = false
            }
            if (!strTip.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvTip_ver)?.text = strTip
            } else {
                it.findViewById<TextView>(R.id.tvTip_ver)?.isVisible = false
            }
        }
    }

    fun setBtn(strOk: String?, strNo: String?) {
        mView?.let {
            if (!strOk.isNullOrEmpty()) {
                it.findViewById<Button>(R.id.btSure_ver)?.text = strOk
            }
            if (!strNo.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvDeny_ver)?.text = strNo
            }
        }
    }

    fun setImg(@DrawableRes logoRes: Int) {
        mView?.findViewById<ImageView>(R.id.ivLoad_ver)?.setImageResource(logoRes)
    }

    fun setImg(act: Activity, imageBytes: ByteArray, ver: Boolean?) {
        mView?.findViewById<ImageView>(R.id.ivLoad_ver)?.let { iv ->
            //取控件当前的布局参数
            val params = iv.layoutParams
            if (ver==true){
                params?.width = DisplayUtil.dip2px(act, 180f)
                params?.height = DisplayUtil.dip2px(act, 320f)
            }else{
                params?.width = DisplayUtil.dip2px(act, 240f)
                params?.height = DisplayUtil.dip2px(act, 135f)
            }
            iv.layoutParams = params
            //加载图片
            Glide.with(act)
                .load(imageBytes)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(20 /*圆角*/)))
                .into(iv)
        }
    }

    fun hideImg() {
        mView?.findViewById<ImageView>(R.id.ivLoad_ver)?.isVisible = false
    }

    fun setContentNative(act: Activity) {
        mView?.findViewById<FrameLayout>(R.id.flContent_ver)?.let { fl ->
            fl.removeAllViews()
            CloudBuilder.getNativeCallBack(fl, act)
        }
    }

    fun setConfirmListener(listener: View.OnClickListener) {
        this.mConfirmListener = listener
    }

    fun setCancelListener(listener: View.OnClickListener?) {
        if (listener != null) {
            this.mCancelListener = listener
        }
    }
}