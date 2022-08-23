package com.cloud.duolib.view.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.cloud.duolib.R

/***
 * 2020/12/15 1:18 PM by LY
 * 描述：
 */
class CommonHorizonDialog(context: Context) : Dialog(context, R.style.Theme_Corner_Dialog) {
    private var mView: View? = null
    private var mConfirmListener: DialogInterface.OnClickListener? = null
    private var mCancelListener: DialogInterface.OnClickListener? = null

    init {
        mView = LayoutInflater.from(context).inflate(R.layout.duo_dialog_horizon, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(mView!!)
        mView?.findViewById<TextView>(R.id.tvDeny_hor)?.setOnClickListener {
            if (mCancelListener == null) {
                dismiss()
            } else {
                mCancelListener?.onClick(this, 0)
            }
        }
        mView?.findViewById<TextView>(R.id.tvSure_hor)?.setOnClickListener {
            mConfirmListener?.onClick(this, 1)
        }
    }

    fun setMsg(strTitle: String?, strTip: String?) {
        mView?.let {
            if (!strTitle.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvTitle_hor)?.text = strTitle
            }
            if (!strTip.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvTip_hor)?.text = strTip
            }
        }
    }

    fun setBtn(strOk: String?, strNo: String?) {
        mView?.let {
            if (!strOk.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvSure_hor)?.text = strOk
            }
            if (!strNo.isNullOrEmpty()) {
                it.findViewById<TextView>(R.id.tvDeny_hor)?.text = strNo
            }
        }
    }

    fun setConfirmListener(listener: DialogInterface.OnClickListener) {
        this.mConfirmListener = listener
    }

    fun setCancelListener(listener: DialogInterface.OnClickListener) {
        this.mCancelListener = listener
    }
}