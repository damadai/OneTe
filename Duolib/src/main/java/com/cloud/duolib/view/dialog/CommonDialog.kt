package com.cloud.duolib.view.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.cloud.duolib.R

class CommonDialog(context: Context) : Dialog(context, R.style.Theme_Full_Dialog) {
    private var mView: View? = null
    private var mConfirmListener: View.OnClickListener? = null
    private var mCancelListener: View.OnClickListener? = null

    init {
        mView = LayoutInflater.from(context).inflate(R.layout.duo_view_media_guide, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        val window = this.window
        window?.decorView?.setPadding(0, 0, 0, 0)
        val point = Point()
        window?.windowManager?.defaultDisplay?.getSize(point)
        val attributes = window?.attributes
        attributes?.width = point.x
        attributes?.height = point.y
        window?.attributes = attributes
        setContentView(mView!!)
        mView?.findViewById<TextView>(R.id.tvPass_guide)?.setOnClickListener {
            dismiss()
            mCancelListener?.onClick(it)
        }
        mView?.findViewById<TextView>(R.id.tvOk_guide)?.setOnClickListener {
            dismiss()
            mConfirmListener?.onClick(it)
        }
    }

    fun setConfirmListener(listener: View.OnClickListener) {
        this.mConfirmListener = listener
    }

    fun setCancelListener(listener: View.OnClickListener) {
        this.mCancelListener = listener
    }
}