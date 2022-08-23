package com.cloud.duolib.bean.duo

import kotlinx.android.parcel.Parcelize

@Parcelize
class BeanFile @JvmOverloads constructor(
    override var name: String,
    override var path: String,
    val size: Long = 0L,
    var type: FileType = FileType.UNKNOWN,
    var status: PushStatusType = PushStatusType.WAIT,
    var progress: Double = 0.0
) : BaseFile(name, path)