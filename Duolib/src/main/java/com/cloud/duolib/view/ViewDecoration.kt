package com.cloud.duolib.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

fun getItemMargin(vir: Int, hor: Int): ItemDecoration {
    return object : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: State,
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.right = hor
            outRect.left = hor
            outRect.top = vir
            outRect.bottom = vir
        }
    }
}