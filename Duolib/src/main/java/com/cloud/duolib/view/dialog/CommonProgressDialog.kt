package com.cloud.duolib.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.cloud.duolib.R
import com.cloud.duolib.view.HorizontalDownloadProgressBar

class CommonProgressDialog(context: Context) : Dialog(context, R.style.Theme_Corner_Dialog) {
    private var mView: View? = null
    private var mCancelListener: View.OnClickListener? = null

    init {
        mView = LayoutInflater.from(context).inflate(R.layout.duo_dialog_progress, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mView!!)
        setCancelable(false)
        mView?.findViewById<ImageView>(R.id.ivClose_pro)?.setOnClickListener(mCancelListener)
        mView?.findViewById<TextView>(R.id.tvDeny_pro)?.setOnClickListener {
            dismiss()
            mCancelListener?.onClick(it)
        }
    }

    fun setMsg(str: String) {
        mView?.findViewById<TextView>(R.id.tvTitle_pro)?.text = str
    }

    fun setCancelListener(listener: View.OnClickListener) {
        this.mCancelListener = listener
    }

    //隐藏控件
    fun hideViews(showClose: Boolean, showQuit: Boolean, showProgress: Boolean) {
        mView?.let {
            it.findViewById<ImageView>(R.id.ivClose_pro)?.visibility =
                if (showClose) View.VISIBLE else View.INVISIBLE
            it.findViewById<TextView>(R.id.tvDeny_pro)?.isVisible = showQuit
            it.findViewById<HorizontalDownloadProgressBar>(R.id.pbLoad_pro)?.isVisible =
                showProgress
        }
    }

    //进度风格
    fun setProgressGreen() {
        mView?.findViewById<HorizontalDownloadProgressBar>(R.id.pbLoad_pro)?.setStyleGreen(context)
    }

    //进度更新
    fun setProgress(infoMsg: String, curProgress: Int) {
        mView?.findViewById<HorizontalDownloadProgressBar>(R.id.pbLoad_pro)
            ?.updateProgress(curProgress, infoMsg)
    }
}