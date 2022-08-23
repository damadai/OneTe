package com.cloud.duolib.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CommonRecyclerAdapter : RecyclerView.Adapter<CommonRecyclerAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var mBindView: ((holder: MyViewHolder, data: Any?, position: Int) -> Unit)? = null

    private var mLayout: Int? = null
    private var mDataList: List<*>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return (MyViewHolder(
            LayoutInflater.from(parent.context).inflate(mLayout!!, null, false)
        ))
    }

    override fun getItemCount(): Int {
        return mDataList!!.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        mBindView?.invoke(
            holder, mDataList?.get(position), position
        )
    }

    class Builder {
        private val commonAdapter: CommonRecyclerAdapter =
            CommonRecyclerAdapter()

        fun setData(data: List<*>): Builder {
            commonAdapter.mDataList = data
            return this
        }

        fun setLayout(layout: Int): Builder {
            commonAdapter.mLayout = layout
            return this
        }

        fun bindView(bindView: ((holder: MyViewHolder, data: Any?, position: Int) -> Unit)): Builder {
            commonAdapter.mBindView = bindView
            return this
        }

        fun create(): CommonRecyclerAdapter {
            return commonAdapter
        }
    }
}