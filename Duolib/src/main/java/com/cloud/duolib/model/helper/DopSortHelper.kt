package com.cloud.duolib.model.helper

import android.content.Context
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.bean.duo.BeanFile
import com.cloud.duolib.bean.duo.FileType
import com.cloud.duolib.model.manager.FilePickManager
import com.cloud.duolib.model.util.AppInfoUtil
import com.cloud.duolib.model.util.PkgInfoUtils

class DopSortHelper {
    //获取已安装应用数据
    fun initApkList(cxt: Context, mDopAppName: String?): ArrayList<BeanFile>? {
        //使用信息获取
        AppInfoUtil().fetchAppInstallList(cxt)?.let { info ->
            val oldList = ArrayList<BeanFile>()
            for (item in info) {
                //存有相应地址应用信息
                var app = FilePickManager.selectAppMap[item.applicationInfo.sourceDir]
                if (app == null) {
                    cxt.packageManager?.let { pm -> app = PkgInfoUtils().getInfoData(pm, item) }
                }
                app?.let {
                    if (it.pkgName.isNotEmpty() && it.path.isNotEmpty()) {
                        oldList.add(
                            BeanFile(
                                it.pkgName,
                                it.path,
                                it.size ?: 0,
                                FileType.APK
                            )
                        )
                    }
                }
            }
            //排序包名、路径、大小、类型
            return getDataSort(mDopAppName, oldList)
        }
        return null
    }

    //数据按列表排序
    private fun getDataSort(appn: String?, oldList: ArrayList<BeanFile>): ArrayList<BeanFile> {
        val fileList = ArrayList<BeanFile>()
        //添加首位包名
        if (!appn.isNullOrEmpty()) {
            val ite = oldList.iterator()
            //遍历并删除
            while (ite.hasNext()) {
                val item = ite.next()
                if (appn == item.name) {
                    fileList.add(item)
                    ite.remove()
                    break
                }
            }
        }
        //获取排序表
        return fileList.also { it.addAll(getSortDop(getSortHot(oldList))) }
    }

    //热门排序
    private fun getSortHot(oldList: ArrayList<BeanFile>): ArrayList<BeanFile> {
        val newList = ArrayList<BeanFile>()
        val hot = CloudBuilder.getHotSort()
        if (!hot.isNullOrEmpty()) {
            //热门排序
            hot.forEach {
                for (old in oldList) {
                    //包名相同
                    if (old.name == it) {
                        newList.add(old)
                    }
                }
            }
            //再加入剩余项
            oldList.forEach { old ->
                if (!newList.contains(old)) {
                    newList.add(old)
                }
            }
        } else {
            newList.addAll(oldList)
        }
        return newList
    }

    //已分身排序
    private fun getSortDop(oldList: ArrayList<BeanFile>): ArrayList<BeanFile> {
        val newList = ArrayList<BeanFile>()
        val dop = CloudBuilder.getDopSort()
        if (!dop.isNullOrEmpty()) {
            //分身排序
            oldList.forEach { file ->
                for (item in dop) {
                    //包名相同
                    if (file.name == item.packageName) {
                        newList.add(file)
                    }
                }
            }
            //再加入剩余项
            oldList.forEach { bean ->
                if (!newList.contains(bean)) {
                    newList.add(bean)
                }
            }
        } else {
            newList.addAll(oldList)
        }
        return newList
    }
}