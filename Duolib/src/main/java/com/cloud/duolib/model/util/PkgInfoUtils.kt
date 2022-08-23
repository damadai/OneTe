package com.cloud.duolib.model.util

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.cloud.duolib.bean.duo.BeanApp
import com.cloud.duolib.model.manager.FilePickManager
import java.io.File

class PkgInfoUtils {
    //路径获取数据
    fun getPathData(pm: PackageManager, path: String): BeanApp {
        var app = FilePickManager.selectAppMap[path]
        if (app == null) {
            app = reSetSize(getLocalPackageInfo(pm, path))
            FilePickManager.selectAppMap[path] = app
        }
        return app
    }

    //信息获取数据
    fun getInfoData(pm: PackageManager, item: PackageInfo): BeanApp {
        val app = reSetSize(getAppInfoDetail(pm, item))
        FilePickManager.selectAppMap[app.path] = app
        return app
    }

    private fun reSetSize(apk: BeanApp): BeanApp {
        val file = File(apk.path)
        if (file.isFile && !file.isHidden && file.exists()) {
            apk.size = file.length()
        }
        return apk
    }

    /**
     * 通过APK地址获取此APP的包名和版本等信息
     * 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等 并且可以直接调用
     * */
    private fun getLocalPackageInfo(pm: PackageManager, filePath: String): BeanApp {
        val info = pm.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES)
        var data = BeanApp("", filePath, null, null, null, null)
        if (info != null) {
            try {
                val appInfo = info.applicationInfo
                appInfo.sourceDir = filePath
                appInfo.publicSourceDir = filePath
                val pkgName = appInfo.packageName
                data = BeanApp(
                    pkgName,
                    filePath,
                    0L,
                    pm.getApplicationLabel(appInfo).toString(),
                    null,
                    pm.getApplicationIcon(appInfo) ?: appInfo.loadIcon(pm)//appInfo.icon//
                )
                val packageInfo = pm.getPackageInfo(pkgName, 0)
                if (packageInfo != null) {
                    // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
                    val resolveIntent = Intent(Intent.ACTION_MAIN, null)
                    resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    resolveIntent.setPackage(packageInfo.packageName)
                    // 通过getPackageManager()的queryIntentActivities方法遍历
                    val resolveInfo: ResolveInfo =
                        pm.queryIntentActivities(resolveIntent, 0).iterator().next()
                    val pName: String = resolveInfo.activityInfo.packageName
                    // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
                    if (pName == pkgName) {
                        data.actName = resolveInfo.activityInfo.name
                    }
                } else {
                    logShow(info = "$pkgName packageInfo null actName")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return data
    }

    fun getAppInfoDetail(pm: PackageManager, info: PackageInfo?): BeanApp {
        var data = BeanApp("", "", null, null, null, null)
        if (info != null) {
            try {
                val appInfo = info.applicationInfo
                val pkgName = appInfo.packageName
                data = BeanApp(
                    pkgName,
                    appInfo.sourceDir,
                    0L,
                    pm.getApplicationLabel(appInfo).toString(),
                    null,
                    pm.getApplicationIcon(appInfo) ?: appInfo.loadIcon(pm)//appInfo.icon//
                )
                val packageInfo = pm.getPackageInfo(pkgName, 0)
                if (packageInfo != null) {
                    // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
                    val resolveIntent = Intent(Intent.ACTION_MAIN, null)
                    resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    resolveIntent.setPackage(packageInfo.packageName)
                    // 通过getPackageManager()的queryIntentActivities方法遍历
                    val resolveInfo: ResolveInfo =
                        pm.queryIntentActivities(resolveIntent, 0).iterator().next()
                    val pName: String = resolveInfo.activityInfo.packageName
                    // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packageName.mainActivityName]
                    if (pName == pkgName) {
                        data.actName = resolveInfo.activityInfo.name
                    }
                } else {
                    logShow(info = "$pkgName null actName")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return data
    }
}