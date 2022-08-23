package com.cloud.duolib.vm

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.bean.duo.FileType
import com.cloud.duolib.model.helper.DopSortHelper
import com.cloud.duolib.model.manager.FilePickManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VMFilePicker(application: Application) : BaseViewModel(application) {
    private val _apkSec = ".apk"
    private val _apkList = listOf("apk", "apk.1")
    private var _listDopData = ArrayList<BeanFile>()
    private val _lvDocData = MutableLiveData<ArrayList<BeanFile>>()
    val lvDocData: LiveData<ArrayList<BeanFile>>
        get() = _lvDocData

    //检索过滤关键字
    fun getDocList(keyword: String, search: Boolean) {
        launchDataLoad {
            val dirs = ArrayList<BeanFile>()
            if (search) {
                //获取过滤
                dirs.addAll(_listDopData.filter { file ->
                    //应用名称过滤
                    (file.name.contains(keyword)) || (FilePickManager.selectAppMap[file.path]?.apkName?.contains(
                        keyword
                    ) == true)
                })
            } else {
                dirs.addAll(_listDopData)
            }
            _lvDocData.postValue(dirs.also { it.addAll(queryDocs(keyword, search)) })
        }
    }

    //首次检索添加已安装应用
    fun getDopList(cxt: Context, firstApkName: String?) {
        //获取应用
        if (_listDopData.isNullOrEmpty()) {
            launchDataLoad {
                val scan = queryDocs(_apkSec, false)
                //_lvDocData.postValue(scan)
                DopSortHelper().initApkList(cxt, firstApkName)
                    ?.let { list -> _listDopData.addAll(list) }
                _lvDocData.postValue(_listDopData.also { it.addAll(scan) })
            }
        } else {
            getDocList(_apkSec, false)
        }
    }

    //获取安装包
    @WorkerThread
    private suspend fun queryDocs(keyword: String, search: Boolean): ArrayList<BeanFile> {
        var data = ArrayList<BeanFile>()
        withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            if (search) {
                sb.append(MediaStore.Files.FileColumns.TITLE).append(" LIKE '%$keyword%'")
            } else {
                sb.append(MediaStore.Files.FileColumns.DATA).append(" LIKE '%$keyword%'")
            }
            //条件检索
            getFilterCursor(sb.toString())?.let { cursor ->
                data = getDocumentFromCursor(cursor)
                cursor.close()
                //应用检索
                if (search) {
                    getFilterCursor(MediaStore.Files.FileColumns.DATA + " LIKE '%$_apkSec%'")?.let { apkCursor ->
                        //过滤检索
                        data.addAll(getDocumentFromCursor(apkCursor).filter { file ->
                            var flag = true
                            data.forEach {
                                if (file.path == it.path) {
                                    flag = false
                                    return@forEach
                                }
                            }
                            flag
                        }.filter { file ->
                            var flag = false
                            if (file.type == FileType.APK) {
                                //应用名称过滤
                                FilePickManager.selectAppMap[file.path]?.let { app ->
                                    flag = app.apkName?.contains(keyword) == true
                                }
                            }
                            flag
                        })
                        apkCursor.close()
                    }
                }
            }
        }
        return data
    }

    @WorkerThread
    private fun getFilterCursor(selection: String?): Cursor? {
        return getApplication<Application>().contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.TITLE
            ), selection, null, null
        )
    }

    @WorkerThread
    private fun getDocumentFromCursor(
        cursor: Cursor
    ): ArrayList<BeanFile> {
        val documents = ArrayList<BeanFile>()
        while (cursor.moveToNext()) {
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            val file = File(path)
            if (path != null && !file.isDirectory && !file.isHidden && file.exists()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE))
                val document = BeanFile(
                    name,
                    path,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
                )
                document.type = getFileType(path)
                _apkList.forEach { sec ->
                    if (path.endsWith(sec)) {
                        document.name += _apkSec
                        document.type = FileType.APK
                        return@forEach
                    }
                }
                if (!documents.contains(document)) documents.add(document)
            }
        }
        return documents
    }
}