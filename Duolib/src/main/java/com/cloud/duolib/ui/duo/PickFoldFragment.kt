package com.cloud.duolib.ui.duo

import android.os.Bundle
import android.os.Environment
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFileFragment
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.getApp
import com.cloud.duolib.model.util.getClick
import com.cloud.duolib.model.util.getCxt
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.view.adapter.FileAdapterListener
import com.cloud.duolib.view.adapter.FileListAdapter
import com.cloud.duolib.vm.VMFoldPicker
import kotlinx.android.synthetic.main.duo_fragment_select_fold.*
import kotlinx.android.synthetic.main.duo_item_file.*

class PickFoldFragment : BaseFileFragment(), FileAdapterListener {
    private var mLastDataList: ArrayList<BeanFile>? = null
    private var fileListAdapter: FileListAdapter? = null
    private var viewModel: VMFoldPicker? = null

    private val backList = ArrayList<String>()
    private val backMap = HashMap<String, Int>()

    override fun getLayoutId() = com.cloud.duolib.R.layout.duo_fragment_select_fold

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getApp(this@PickFoldFragment)?.let {
            viewModel = ViewModelProvider(
                this@PickFoldFragment,
                ViewModelProvider.AndroidViewModelFactory(it)
            ).get(VMFoldPicker::class.java)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        viewModel?.lvDocData?.observe(viewLifecycleOwner) { data ->
            updateList(data)
        }
        viewModel?.lvDataChanged?.observe(viewLifecycleOwner) {
            viewModel?.getDocs(getEndPath())
        }
        llFile_item?.isVisible = false
        llFile_item?.setOnClickListener {
            if ((this.parentFragment as? FilePickerFragment)?.getProgress() == true) return@setOnClickListener
            getClick {
                clickBackFold()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!backList.contains(rootPath)) {
            addBackList(rootPath, 0)
            (this.parentFragment as? FilePickerFragment)?.setProgress(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backList.clear()
        backMap.clear()
    }

    companion object {
        private val rootPath = Environment.getExternalStorageDirectory().path
    }

    override fun onFileClicked(outSize: Boolean) {
        if (outSize) {
            getToast(this@PickFoldFragment, R.string.select_size_out)
        } else {
            (parentFragment as? FilePickerFragment)?.onChildSelected()
        }
    }

    override fun onFoldClicked(beanFile: BeanFile, pos: Int) {
        //文件夹不为空
        if (beanFile.size != 0L) {
            addBackList(beanFile.path, pos)
            llFile_item?.isVisible = true
        } else {
            getToast(this@PickFoldFragment, R.string.empty)
        }
    }

    override fun resetSelectFile() {
        mLastDataList?.let {
            fileListAdapter?.setData(it, FilePickManager.selectFiles)
            onFileClicked(false)
        }
    }

    private fun clickBackFold() {
        val endPath = getEndPath()
        if (endPath != rootPath) {
            backList.removeLast()
            getEndPath().let {
                viewModel?.getDocs(it)
                tvSize_item?.text = it
                llFile_item?.isVisible = (it != rootPath)
            }
        }
    }

    private fun getEndPath() =
        if (backList.isNullOrEmpty()) rootPath else backList[backList.size - 1]

    private fun addBackList(str: String, pos: Int) {
        backMap[getEndPath()] = pos
        backList.add(str)
        tvSize_item?.text = str
        viewModel?.getDocs(str)
    }

    private fun updateList(dirs: ArrayList<BeanFile>) {
        view?.let {
            getCxt(this@PickFoldFragment, { cxt ->
                if (!dirs.isNullOrEmpty()) {
                    (this.parentFragment as? FilePickerFragment)?.setProgress(false)
                    fileListAdapter = recyclerview_fold?.adapter as? FileListAdapter
                    if (fileListAdapter == null) {
                        fileListAdapter = FileListAdapter(
                            cxt, dirs,
                            FilePickManager.selectFiles,
                            FilePickManager.selectAppMap,
                            this, 2, null
                        )
                        recyclerview_fold?.adapter = fileListAdapter
                    } else {
                        fileListAdapter?.setData(
                            dirs,
                            FilePickManager.selectFiles
                        )
                        backMap[getEndPath()]?.let { pos ->
                            try {
                                recyclerview_fold?.scrollToPosition(pos)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    mLastDataList = dirs
                    onFileClicked(false)
                }
            }, null)
        }
    }
}