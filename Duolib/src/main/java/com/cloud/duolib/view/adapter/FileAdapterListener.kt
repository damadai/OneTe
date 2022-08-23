package com.cloud.duolib.view.adapter

import com.cloud.duolib.bean.duo.BeanFile

interface FileAdapterListener {
    fun onFoldClicked(beanFile: BeanFile, pos: Int)
    fun onFileClicked(outSize:Boolean)
}