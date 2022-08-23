package com.cloud.duolib.model.manager

import com.cloud.duolib.bean.duo.BeanApp
import com.cloud.duolib.bean.duo.BeanFile

object FilePickManager {
    val selectAppMap = HashMap<String, BeanApp>()

    private var maxCount = -1

    val selectFiles = ArrayList<BeanFile>()

    val currentCount: Int
        get() = selectFiles.size

    val curSize: Long
        get() {
            var size = 0L
            selectFiles.forEach {
                size += it.size
            }
            return size
        }

    fun setMaxCount(count: Int) {
        maxCount = count
    }

    fun getMaxCount(): Int {
        return maxCount
    }

    private fun isContain(path: BeanFile, remove: Boolean): Boolean {
        selectFiles.iterator().let { iterator ->
            while (iterator.hasNext()) {
                if (iterator.next().path == path.path) {
                    if (remove) iterator.remove()
                    return true
                }
            }
        }
        return false
    }

    fun add(file: BeanFile?) {
        if (file != null && shouldAdd() && !isContain(file, false)) {
            selectFiles.add(file)
        }
    }

    fun remove(path: BeanFile) {
        isContain(path, true)
    }

    fun shouldAdd(): Boolean {
        return if (maxCount == -1) true else currentCount < maxCount
    }

    fun reset() {
        selectFiles.clear()
    }

    fun resetAll() {
        maxCount = -1
        selectFiles.clear()
    }
}