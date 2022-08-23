package com.cloud.duolib.view

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.cloud.capture.DisplayUtil
import com.cloud.duolib.R
import com.lzf.easyfloat.utils.DisplayUtils


class ToolsPopWindow(private val act: Activity, listener: View.OnClickListener) : PopupWindow(
    LayoutInflater.from(act)
        .inflate(R.layout.duo_view_media_tools, null),
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT,
    true
) {

    init {
        //设置各个控件的点击响应
        contentView.findViewById<Button>(R.id.btScan_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btShake_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btShot_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btHelp_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btFix_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btFix2_tools).setOnClickListener(listener)
        contentView.findViewById<Button>(R.id.btFix3_tools).setOnClickListener(listener)
    }

    fun showPop(v: View, ver: Boolean) {
        //以某个控件的x和y的偏移量位置开始显示窗口
        showTopTipsPop(this, v, ver)
        this.update()
    }

    private fun showTopTipsPop(popWin: PopupWindow, anchor: View, ver: Boolean) {
        val popView = popWin.contentView
        popView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        if (ver) {
            popView.findViewById<ImageView>(R.id.ivTri_tools)?.let { iv ->
                iv.clearAnimation()
                iv.invalidate()
                (iv.layoutParams as? RelativeLayout.LayoutParams)?.let {
                    it.addRule(RelativeLayout.BELOW, R.id.btFix_tools)
                    it.addRule(RelativeLayout.ALIGN_END, R.id.btScan_tools)
                    it.removeRule(RelativeLayout.END_OF)
                    it.marginEnd = DisplayUtil.dip2px(act, 24f)
                }
                ObjectAnimator.ofPropertyValuesHolder(
                    iv,
                    PropertyValuesHolder.ofFloat("rotation", 0f),
                    PropertyValuesHolder.ofFloat("translationY", 0f)
                ).start()
                // 右下角 (父尾宽20+父半宽-角尾距24-角半宽9，栏高+父高48-角高18+偏移6)
                popWin.showAtLocation(
                    anchor, Gravity.END or Gravity.BOTTOM,
                    anchor.measuredWidth / 2 - DisplayUtil.dip2px(act, 13f),
                    DisplayUtils.getNavigationBarCurrentHeight(popView.context) + DisplayUtil.dip2px(
                        act,
                        36f
                    )
                )
            }
        } else {
            IntArray(2).let { location ->
                anchor.getLocationOnScreen(location)
                popView.findViewById<ImageView>(R.id.ivTri_tools)?.let { iv ->
                    iv.clearAnimation()
                    iv.invalidate()
                    (iv.layoutParams as? RelativeLayout.LayoutParams)?.let {
                        it.removeRule(RelativeLayout.BELOW)
                        it.removeRule(RelativeLayout.ALIGN_END)
                        it.addRule(RelativeLayout.END_OF, R.id.btScan_tools)
                        it.marginEnd = 0
                        ObjectAnimator.ofPropertyValuesHolder(
                            iv,
                            PropertyValuesHolder.ofFloat("rotation", -90f),
                            PropertyValuesHolder.ofFloat(
                                "translationY",//父尾宽20+父半宽-角半宽9
                                anchor.measuredWidth.toFloat() / 2 + DisplayUtil.dip2px(act, 11f)
                            )
                        ).start()
                    }
                }
                // 右上角 (x-本宽-父宽48+角高18-偏移6)
                popWin.showAtLocation(
                    anchor, Gravity.NO_GRAVITY,
                    location[0] - popView.findViewById<Button>(R.id.btScan_tools).measuredWidth - DisplayUtil.dip2px(
                        act,
                        36f
                    ),
                    0
                )
            }
        }
    }
}