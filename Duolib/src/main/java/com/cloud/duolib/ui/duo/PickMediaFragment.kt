package com.cloud.duolib.ui.duo

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.cloud.duolib.R
import com.cloud.duolib.base.BaseFileFragment
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.getApp
import com.cloud.duolib.model.util.getCxt
import com.cloud.duolib.model.util.getToast
import com.cloud.duolib.view.adapter.FileAdapterListener
import com.cloud.duolib.view.adapter.FileListAdapter
import com.cloud.duolib.vm.VMMediaPicker
import kotlinx.android.synthetic.main.duo_fragment_select_media.*
import kotlin.math.abs

class PickMediaFragment : BaseFileFragment(), FileAdapterListener {
    private var mLastDataList: ArrayList<BeanFile>? = null
    private var photoGridAdapter: FileListAdapter? = null
    private var viewModel: VMMediaPicker? = null

    private var mGlideRequestManager: RequestManager? = null
    private var mStart = false

    override fun getLayoutId() = com.cloud.duolib.R.layout.duo_fragment_select_media

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this@PickMediaFragment)
        getApp(this@PickMediaFragment)?.let {
            viewModel = ViewModelProvider(
                this@PickMediaFragment,
                ViewModelProvider.AndroidViewModelFactory(it)
            ).get(VMMediaPicker::class.java)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        initRv()
        viewModel?.lvMediaData?.observe(viewLifecycleOwner) { data ->
            updateList(data)
        }
        viewModel?.lvDataChanged?.observe(viewLifecycleOwner) {
            viewModel?.getPhotoDirs()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mStart) {
            viewModel?.getPhotoDirs()
            (this.parentFragment as? FilePickerFragment)?.setProgress(true)
            mStart = true
        }
    }

    companion object {
        private const val SCROLL_THRESHOLD = 30
    }

    override fun onFoldClicked(beanFile: BeanFile, pos: Int) {

    }

    override fun onFileClicked(outSize: Boolean) {
        if (outSize) {
            getToast(this@PickMediaFragment, R.string.select_size_out)
        } else {
            (parentFragment as? FilePickerFragment)?.onChildSelected()
        }
    }

    override fun resetSelectFile() {
        mLastDataList?.let {
            photoGridAdapter?.setData(it, FilePickManager.selectFiles)
            onFileClicked(false)
        }
    }

    private fun initRv() {
        val layoutManager = StaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        layoutManager.gapStrategy =
            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        rv_media?.let { rv ->
            rv.layoutManager = layoutManager
            rv.itemAnimator = DefaultItemAnimator()
            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (abs(dy) > SCROLL_THRESHOLD) {
                        mGlideRequestManager?.pauseRequests()
                    } else {
                        resumeRequestsIfNotDestroyed()
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        resumeRequestsIfNotDestroyed()
                    }
                }
            })
        }
    }

    private fun updateList(medias: ArrayList<BeanFile>) {
        view?.let {
            getCxt(this@PickMediaFragment, { cxt ->
                if (photoGridAdapter != null) {
                    photoGridAdapter?.setData(medias, FilePickManager.selectFiles)
                } else {
                    photoGridAdapter = FileListAdapter(
                        cxt,
                        medias,
                        FilePickManager.selectFiles,
                        FilePickManager.selectAppMap,
                        this, 3, mGlideRequestManager
                    )
                    (this.parentFragment as? FilePickerFragment)?.setProgress(false)
                    rv_media?.adapter = photoGridAdapter
                }
                mLastDataList = medias
            }, null)
        }
    }

    private fun resumeRequestsIfNotDestroyed() {
        mGlideRequestManager?.resumeRequests()
    }
}