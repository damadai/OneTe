package com.cloud.duolib.ui.duo

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFileFragment
import com.cloud.duolib.base.BasePermissionActivity
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.*
import com.cloud.duolib.view.adapter.FileAdapterListener
import com.cloud.duolib.view.adapter.FileListAdapter
import com.cloud.duolib.vm.VMFilePicker
import kotlinx.android.synthetic.main.duo_fragment_select_apk.*

class PickFileFragment : BaseFileFragment(), FileAdapterListener {
    private var mLastDataList: ArrayList<BeanFile>? = null
    private var fileListAdapter: FileListAdapter? = null
    private var viewModel: VMFilePicker? = null

    private var mStart = false
    private var mDopAppName: String? = null
    private var clickSearch = false

    companion object {
        const val FRA_APP_DOP = "FRA_APP_DOP"
    }

    override fun getLayoutId() = com.cloud.duolib.R.layout.duo_fragment_select_apk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getApp(this@PickFileFragment)?.let {
            viewModel = ViewModelProvider(
                this@PickFileFragment,
                ViewModelProvider.AndroidViewModelFactory(it)
            ).get(VMFilePicker::class.java)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        mDopAppName = arguments?.getString(FRA_APP_DOP)
        viewModel?.lvDocData?.observe(viewLifecycleOwner) { data ->
            updateList(data)
        }
        ivSearch_file?.setOnClickListener {
            if ((this.parentFragment as? FilePickerFragment)?.getProgress() == true) return@setOnClickListener
            getClick {
                startSearch()
            }
        }
        etSearch_file?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch()
            }
            false
        }
        ivCancel_file?.setOnClickListener {
            if ((this.parentFragment as? FilePickerFragment)?.getProgress() == true) return@setOnClickListener
            getClick {
                etSearch_file?.text?.clear()
                ivCancel_file?.isVisible = false
                startVm(null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mStart) {
            startVm(null)
            mStart = true
        }
    }

    override fun onPause() {
        super.onPause()
        getAct(this@PickFileFragment) { act ->
            (act as? BasePermissionActivity)?.hideSoftKeyBoard()
        }
    }

    override fun onFoldClicked(beanFile: BeanFile, pos: Int) {
    }

    override fun onFileClicked(outSize: Boolean) {
        if (outSize) {
            getToast(this@PickFileFragment, R.string.select_size_out)
        } else {
            (parentFragment as? FilePickerFragment)?.onChildSelected()
        }
    }

    override fun resetSelectFile() {
        mLastDataList?.let {
            fileListAdapter?.setData(it, FilePickManager.selectFiles)
            onFileClicked(false)
        }
    }

    private fun startSearch() {
        val input = etSearch_file?.text.toString()
        if (input.isNotEmpty() && input.isNotBlank()) {
            startVm(input)
        } else {
            getToast(this@PickFileFragment, R.string.empty_enter)
        }
    }

    private fun startVm(filter: String?) {
        (this.parentFragment as? FilePickerFragment)?.setProgress(true)
        if (!filter.isNullOrEmpty()) {
            clickSearch = true
            viewModel?.getDocList(filter, true)
        } else {
            clickSearch = false
            getCxt(this@PickFileFragment, {
                viewModel?.getDopList(it, mDopAppName)
            }, null)
        }
    }

    private fun updateList(dirs: ArrayList<BeanFile>?) {
        view?.let {
            (this.parentFragment as? FilePickerFragment)?.setProgress(false)
            getCxt(this@PickFileFragment, { cxt ->
                if (!dirs.isNullOrEmpty()) {
                    fileListAdapter = recyclerview_file?.adapter as? FileListAdapter
                    if (fileListAdapter == null) {
                        //首次加载通过路径获取数据
                        for (item in dirs) {
                            PkgInfoUtils().getPathData(cxt.packageManager, item.path)
                        }
                        fileListAdapter = FileListAdapter(
                            cxt, dirs,
                            FilePickManager.selectFiles,
                            FilePickManager.selectAppMap,
                            this, 1, null
                        )
                        recyclerview_file?.adapter = fileListAdapter
                    } else {
                        fileListAdapter?.setData(
                            dirs,
                            FilePickManager.selectFiles
                        )
                    }
                    mLastDataList = dirs
                    onFileClicked(false)
                    getAct(this@PickFileFragment) { act ->
                        (act as? BasePermissionActivity)?.hideSoftKeyBoard()
                    }
                    //点击搜索,有结果
                    if (clickSearch) {
                        ivCancel_file?.isVisible = true
                    }
                } else {
                    //点击搜索,无结果
                    if (clickSearch) {
                        getToast(this@PickFileFragment, R.string.empty_parse)
                    }
                }
            }, null)
        }
    }
}