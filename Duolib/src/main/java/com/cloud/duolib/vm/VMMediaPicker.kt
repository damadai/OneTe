package com.cloud.duolib.vm

import android.app.Application
import android.database.ContentObserver
import android.database.Cursor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloud.duolib.bean.duo.BeanFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class VMMediaPicker(application: Application) : BaseViewModel(application) {
    private val _lvMediaData = MutableLiveData<ArrayList<BeanFile>>()
    val lvMediaData: LiveData<ArrayList<BeanFile>>
        get() = _lvMediaData

    private val _lvDataChanged = MutableLiveData<Boolean>()
    val lvDataChanged: LiveData<Boolean>
        get() = _lvDataChanged

    private var contentObserver: ContentObserver? = null

    private fun registerContentObserver() {
        if (contentObserver == null) {
            contentObserver = getApplication<Application>().contentResolver.registerObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ) {
                _lvDataChanged.value = true
            }
        }
    }

    fun getPhotoDirs() {
        launchDataLoad {
            val calendar = Calendar.getInstance()
            queryImages(
                null,
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                null,
                null,
                true
            ).let { dirs ->
                _lvMediaData.postValue(dirs)
                registerContentObserver()
            }
        }
    }

    @WorkerThread
    suspend fun queryImages(
        bucketId: String?,
        mediaType: Int,
        imageFileSize: Int? = Int.MAX_VALUE,
        videoFileSize: Int? = Int.MAX_VALUE,
        isShowGif: Boolean
    ): ArrayList<BeanFile> {
        var data = ArrayList<BeanFile>()
        withContext(Dispatchers.IO) {
            val projection = null
            val sortOrder = MediaStore.Images.Media._ID + " DESC"
            val selectionArgs = mutableListOf<String>()

            var selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)

            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

                if (videoFileSize != null) {
                    selection += " AND ${MediaStore.Video.Media.SIZE}<=?"
                    selectionArgs.add("${videoFileSize * 1024 * 1024}")
                }
            }

            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE && imageFileSize != null) {
                selection += " AND ${MediaStore.Images.Media.SIZE}<=?"
                selectionArgs.add("${imageFileSize * 1024 * 1024}")
            }

            if (!isShowGif) {
                selection += " AND " + MediaStore.Images.Media.MIME_TYPE + "!='" + MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension("gif") + "'"
            }

            if (bucketId != null)
                selection += " AND " + MediaStore.Images.Media.BUCKET_ID + "='" + bucketId + "'"

            val cursor = getApplication<Application>().contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs.toTypedArray(),
                sortOrder
            )

            if (cursor != null) {
                data = getPhotoDirectories(cursor)
                cursor.close()
            }
        }
        return data
    }

    @WorkerThread
    private fun getPhotoDirectories(data: Cursor): ArrayList<BeanFile> {
        val directories = ArrayList<BeanFile>()
        while (data.moveToNext()) {
            var name =
                "${data.getString(data.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))}_" + data.getString(
                    data.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
                )
            val path = data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            val type = path.substring(path.lastIndexOf(".") + 1)
            if (!name.endsWith(type)) {
                name += ".$type"
            }
            val photoDirectory = BeanFile(
                name, path,
                data.getLong(data.getColumnIndex(MediaStore.Files.FileColumns.SIZE)),
                getMediaType(type)
            )
            if (!directories.contains(photoDirectory)) {
                directories.add(photoDirectory)
            } else {
                directories[directories.indexOf(photoDirectory)] = photoDirectory
            }
        }
        return directories
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}