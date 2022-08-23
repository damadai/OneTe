package com.cloud.duolib.vm

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloud.duolib.bean.duo.FileType
import kotlinx.coroutines.*

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    private val viewModelJob = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        t.printStackTrace()
    }

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob + exceptionHandler)

    private val _lvError = MutableLiveData<Exception>()
    open val lvError: LiveData<Exception>
        get() = _lvError

    fun launchDataLoad(block: suspend (scope: CoroutineScope) -> Unit): Job {
        return uiScope.launch {
            try {
                block(this)
            } catch (e: Exception) {
                e.printStackTrace()
                handleException(e)
            } finally {
            }
        }
    }

    private fun handleException(error: Exception) {
        error.printStackTrace()
        if (error !is CancellationException) {
            _lvError.value = error
        }
    }

    public override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                observer(selfChange)
            }
        }
        registerContentObserver(uri, true, contentObserver)
        return contentObserver
    }

    fun getFileType(name: String): FileType {
        return when (name.substring(name.lastIndexOf(".") + 1)) {
            "apk", "aab" -> FileType.APK
            "mp3", "wav" -> FileType.AUDIO
            "txt", "log", "doc", "docx" -> FileType.TXT
            "jpg", "png", "jpeg" -> FileType.IMG
            "mp4", "avi", "wmv" -> FileType.VIDEO
            "gif" -> FileType.GIF
            else -> FileType.UNKNOWN
        }
    }

    fun getMediaType(type: String): FileType {
        return if (type == "gif") FileType.GIF else FileType.IMG
    }
}