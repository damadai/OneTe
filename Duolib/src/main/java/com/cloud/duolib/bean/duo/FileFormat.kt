package com.cloud.duolib.bean.duo

import com.cloud.duolib.R
import java.text.DecimalFormat

enum class FileType {
    FOLD, APK, AUDIO, TXT, IMG, VIDEO, GIF, UNKNOWN
}

//上传文件状态
enum class PushStatusType {
    WAIT, START, UPLOAD, INSTALL,
    SPEED_UPLOAD, SPEED_INSTALL,
    OPEN, VIEW,
    FAIL_OBS, FAIL_UPLOAD, FAIL_INSTALL, FAIL_STATE
}

fun getStateFormatStatus(status: PushStatusType): String {
    return when (status) {
        PushStatusType.WAIT -> "等待"
        PushStatusType.START -> "正在开始"
        PushStatusType.UPLOAD -> "上传中"
        PushStatusType.SPEED_UPLOAD -> "加速中"
        PushStatusType.INSTALL, PushStatusType.SPEED_INSTALL -> "安装中"
        PushStatusType.OPEN -> "打开"
        else -> "查看"//"上传失败", "传输失败", "安装失败", "查询失败"
    }
}

fun getProgressFormatStatus(status: PushStatusType): Double {
    return when (status) {
        PushStatusType.WAIT -> 0.0
        PushStatusType.START -> 10.0
        PushStatusType.UPLOAD -> 50.0
        PushStatusType.SPEED_UPLOAD -> 70.0
        PushStatusType.INSTALL, PushStatusType.SPEED_INSTALL -> 99.0
        else -> 100.0
    }
}

//下载中、安装中、未安装、已安装
fun getFinishState(key: String) = keyInstallStateMap[key]
private val keyInstallStateMap = mapOf(
    "4" to PushStatusType.SPEED_UPLOAD,
    "5" to PushStatusType.SPEED_INSTALL,
    "6" to PushStatusType.FAIL_STATE,
    "7" to PushStatusType.OPEN
)

//开始、上传、安装、加速
fun getUnFinishState(key: PushStatusType) = keyUnFinishMap.contains(key)
private val keyUnFinishMap = listOf(
    PushStatusType.START,
    PushStatusType.UPLOAD,
    PushStatusType.SPEED_UPLOAD,
    PushStatusType.INSTALL,
    PushStatusType.SPEED_INSTALL
)

//加速
fun getSpeedState(key: PushStatusType) = keySpeedMap.contains(key)
private val keySpeedMap = listOf(
    PushStatusType.SPEED_UPLOAD,
    PushStatusType.SPEED_INSTALL
)

internal interface FileSize {
    companion object {
        const val NB: Long = 0
        const val B: Long = 1024
        const val KB = B * 1024
        const val MB = KB * 1024
        const val GB = MB * 1024
        const val TB = GB * 1024
    }
}

internal interface SizeName {
    companion object {
        const val B = "B"
        const val KB = "KB"
        const val MB = "MB"
        const val GB = "GB"
        const val TB = "TB"
    }
}

fun getFileFormatSize(length: Long): String {
    val format = DecimalFormat("#.00")
    return when {
        length == FileSize.NB -> {
            ""
        }
        length < FileSize.B -> {
            length.toString() + SizeName.B
        }
        length < FileSize.KB -> {
            format.format(length * 1.0 / FileSize.B) + SizeName.KB
        }
        length < FileSize.MB -> {
            format.format(length * 1.0 / FileSize.KB) + SizeName.MB
        }
        length < FileSize.GB -> {
            format.format(length * 1.0 / FileSize.MB) + SizeName.GB
        }
        else -> {
            format.format(length * 1.0 / FileSize.GB) + SizeName.TB
        }
    }
}

fun getSpeedFormatSize(length: Long?): Long {
    return when {
        (length == null) || (length < FileSize.KB) -> {
            2
        }
        length < FileSize.MB -> {
            //大于50M则延时
            if ((length / FileSize.KB) > 50) 5 else 3
        }
        else -> {
            10
        }
    }
}

fun getAddFormatSize(length: Long?): Double {
    return when {
        (length == null) || (length < FileSize.KB) -> {
            5.0
        }
        length < FileSize.MB -> {
            when (length / FileSize.KB) {
                in 0..100 -> {
                    1.0
                }
                in 100..150 -> {
                    0.5
                }
                else -> {
                    //大于150M
                    0.25
                }
            }
        }
        length < FileSize.GB -> {
            //大于1.5G
            if ((length / FileSize.MB) > 1.5) 0.1 else 0.2
        }
        else -> {
            0.05
        }
    }
}

fun getFileFormatRes(type: FileType): Int {
    return when (type) {
        FileType.APK -> {
            R.mipmap.ic_apk_210
        }
        FileType.FOLD -> {
            R.mipmap.ic_fold_120
        }
        FileType.AUDIO -> {
            R.mipmap.ic_sound_120
        }
        FileType.TXT -> {
            R.mipmap.ic_document_120
        }
        else -> {
            R.mipmap.ic_device_210
        }
    }
}