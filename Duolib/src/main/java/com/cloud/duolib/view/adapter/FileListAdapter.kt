package com.cloud.duolib.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.cloud.duolib.R
import com.cloud.duolib.bean.duo.*
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.logShow
import com.cloud.duolib.view.HorizontalDownloadProgressBar
import com.cloud.duolib.view.SmoothCheckBox

class FileListAdapter(
    private val context: Context,
    private var mFilteredList: List<BeanFile>,
    private var selectedPaths: ArrayList<BeanFile>?,
    private var apkMap: Map<String, BeanApp?>?,
    private val mListener: FileAdapterListener,
    private val mShowType: Int,//1APk 2Fold 3Media 4His
    private val mGlide: RequestManager?
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>()/*, Filterable */ {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemView = if (mShowType == 3) {
            LayoutInflater.from(context)
                .inflate(R.layout.duo_item_media, parent, false)
        } else {
            LayoutInflater.from(context)
                .inflate(R.layout.duo_item_file, parent, false)
        }
        return FileViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileBean = mFilteredList[position]
        val fName = fileBean.name
        holder.nameView?.text = fName
        when (fileBean.type) {
            FileType.FOLD -> {
                holder.imageView?.setImageResource(getFileFormatRes(fileBean.type))
            }
            FileType.APK -> {
                if (!fName.endsWith("apk")) {
                    holder.nameView?.text = "$fName.apk"
                }
                var size = getFileFormatSize(fileBean.size)
                holder.imageView?.setImageResource(getFileFormatRes(fileBean.type))
                if (!apkMap.isNullOrEmpty()) {
                    apkMap!![fileBean.path]?.icon?.let { icon ->
                        holder.imageView?.setImageDrawable(icon)
                    }
                    apkMap!![fileBean.path]?.apkName?.let {
                        size += "($it)"
                    }
                }
                holder.sizeView?.text = size
            }
            FileType.IMG, FileType.VIDEO -> {
                holder.imageView?.let { iv ->
                    (mGlide ?: Glide.with(iv)).load(fileBean.path).into(iv)
                }
                holder.nameView?.text = fName
                holder.sizeView?.text = getFileFormatSize(fileBean.size)
            }
            FileType.GIF -> {
                holder.imageView?.let { iv ->
                    (mGlide ?: Glide.with(iv)).asGif().load(fileBean.path).into(iv)
                }
                holder.nameView?.text = fName
                holder.sizeView?.text = getFileFormatSize(fileBean.size)
            }
            else -> {
                holder.imageView?.setImageResource(getFileFormatRes(fileBean.type))
                holder.sizeView?.text = getFileFormatSize(fileBean.size)
            }
        }
        bindListData(holder, fileBean, position)
    }

    private fun isSelected(item: BeanFile, remove: Boolean): Boolean {
        selectedPaths?.iterator()?.let { iterator ->
            while (iterator.hasNext()) {
                if (iterator.next().path == item.path) {
                    if (remove) iterator.remove()
                    return true
                }
            }
        }
        return false
    }

    fun setData(items: ArrayList<BeanFile>, sPaths: ArrayList<BeanFile>?) {
        this.mFilteredList = items
        this.selectedPaths = sPaths
        this.notifyDataSetChanged()
    }

    private fun bindListData(holder: FileViewHolder, beanFile: BeanFile, position: Int) {
        holder.checkBox?.setOnCheckedChangeListener(null)
        if (mShowType != 4) {
            if (beanFile.type == FileType.FOLD) {
                bindFoldData(holder, beanFile, position)
            } else {
                bindFileData(holder, beanFile)
            }
            return
        }
        bindBtnPush(holder, beanFile, position)
        logShow(info = "bindListData=${beanFile.progress}${beanFile.status}")
    }

    //推送类型
    private fun bindBtnPush(holder: FileViewHolder, beanFile: BeanFile, position: Int) {
        if (getSpeedState(beanFile.status)) {
            holder.speedView?.isVisible = true
        }
        holder.pushView?.updateProgress(
            beanFile.progress.toInt(),
            getStateFormatStatus(beanFile.status)
        )
        holder.pushView?.isVisible = true
        holder.pushView?.setOnClickListener {
            mListener.onFoldClicked(beanFile, position)
        }
    }

    //文件类型
    private fun bindFileData(holder: FileViewHolder, beanFile: BeanFile) {
        holder.itemView.setOnClickListener { onItemClicked(holder) }
        holder.checkBox?.setOnClickListener { onItemClicked(holder) }
        isSelected(beanFile, false).let { se ->
            holder.checkBox?.isChecked = se

            holder.itemView.setBackgroundResource(if (se) R.color.gray6 else R.color.white)

            if (mShowType != 3) {
                holder.checkBox?.visibility = if (se) View.VISIBLE else View.GONE
            }
        }

        holder.checkBox?.setOnCheckedChangeListener(object :
            SmoothCheckBox.OnCheckedChangeListener {
            override fun onCheckedChanged(checkBox: SmoothCheckBox, isChecked: Boolean) {
                if (!isSelected(beanFile, true)) {
                    selectedPaths?.add(beanFile)
                    FilePickManager.add(beanFile)
                } else {
                    FilePickManager.remove(beanFile)
                }
                holder.itemView.setBackgroundResource(if (isChecked) R.color.gray6 else R.color.white)
                mListener.onFileClicked(false)
            }
        })
    }

    //文件夹类型
    private fun bindFoldData(holder: FileViewHolder, beanFile: BeanFile, position: Int) {
        holder.sizeView?.text =
            holder.itemView.context.resources.getString(R.string.item, beanFile.size.toString())
        holder.checkBox?.visibility = View.GONE
        holder.itemView.setOnClickListener {
            mListener.onFoldClicked(beanFile, position)
        }
    }

    private fun onItemClicked(holder: FileViewHolder) {
        holder.checkBox?.let { check ->
            if (check.isChecked) {
                check.setChecked(!check.isChecked, true)
                if (mShowType != 3) check.visibility = View.GONE
            } else if (FilePickManager.shouldAdd()) {
                check.setChecked(!check.isChecked, true)
                if (mShowType != 3) check.visibility = View.VISIBLE
            } else {
                mListener.onFileClicked(true)
            }
        }
    }

    override fun getItemCount(): Int {
        return mFilteredList.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val checkBox: SmoothCheckBox? =
            itemView.findViewById(R.id.checkbox_item) ?: itemView.findViewById(R.id.scBox_item)

        internal val imageView: ImageView? =
            itemView.findViewById(R.id.ivType_item) ?: itemView.findViewById(R.id.ivPhoto_item)

        internal val nameView: TextView? = itemView.findViewById(R.id.tvName_item)

        internal val sizeView: TextView? = itemView.findViewById(R.id.tvSize_item)

        internal val speedView: TextView? = itemView.findViewById(R.id.tvSpeed_item)

        internal val pushView: HorizontalDownloadProgressBar? =
            itemView.findViewById(R.id.btPush_item)
    }

/*    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint.toString()
                if (charString.isNotEmpty()) {
                    val filteredList = ArrayList<BeanFile>()
                    for (document in mFilteredList) {
                        if (document.name.toLowerCase().contains(charString)) {
                            filteredList.add(document)
                        }
                    }
                    mFilteredList = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = mFilteredList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                (results?.values as? List<BeanFile>)?.let {
                    mFilteredList = it
                    notifyDataSetChanged()
                }
            }
        }
    }*/
}