package com.cloud.duolib.vm

import android.app.Application
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.bean.duo.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VMFoldPicker(application: Application) : BaseViewModel(application) {
    private val _lvDocData = MutableLiveData<ArrayList<BeanFile>>()
    val lvDocData: LiveData<ArrayList<BeanFile>>
        get() = _lvDocData

    fun getDocs(path: String) {
        launchDataLoad {
            val dirs = queryDocs(path)
            _lvDocData.postValue(dirs)
            registerContentObserver()
        }
    }

    private val _lvDataChanged = MutableLiveData<Boolean>()
    val lvDataChanged: LiveData<Boolean>
        get() = _lvDataChanged

    private var contentObserver: ContentObserver? = null
    private fun registerContentObserver() {
        if (contentObserver == null) {
            contentObserver = getApplication<Application>().contentResolver.registerObserver(
                MediaStore.Files.getContentUri("external")
            )
            { _lvDataChanged.value = true }
        }
    }

    @WorkerThread
    suspend fun queryDocs(path: String): ArrayList<BeanFile> {
        val data = ArrayList<BeanFile>()
        withContext(Dispatchers.IO) {
            val list = File(path).listFiles()
            if (!list.isNullOrEmpty()) {
                list.forEach { item ->
                    if (!item.isHidden) {
                        val name = item.name
                        val type: FileType
                        var size = item.length()
                        if (item.isDirectory) {
                            type = FileType.FOLD
                            val files = item.listFiles()
                            size = if (!files.isNullOrEmpty()) {
                                files.size.toLong()
                            } else {
                                0L
                            }
                        } else {
                            type = getFileType(name)
                        }
                        data.add(
                            BeanFile(
                                name,
                                item.path,
                                size,
                                type
                            )
                        )
                    }
                }
            }
        }
        return data
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}