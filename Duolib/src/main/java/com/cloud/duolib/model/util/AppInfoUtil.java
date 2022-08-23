package com.cloud.duolib.model.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class AppInfoUtil {
    /**
     * 检测应用安装列表
     */
    public List<PackageInfo> fetchAppInstallList(Context context) {
        try {
            //1.获取一个包管理器。
            PackageManager pm = context.getPackageManager();
            // 获取手机内所有应用
            List<PackageInfo> packlist = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
            List<PackageInfo> apps = new ArrayList<>();
            for (int i = 0; i < packlist.size(); i++) {
                PackageInfo pak = packlist.get(i);

                // if()里的值如果<=0则为自己装的程序，否则为系统工程自带
                if ((pak.applicationInfo.flags & pak.applicationInfo.FLAG_SYSTEM) <= 0) {
                    // 添加自己已经安装的应用程序
                    //非签名限制应用，则可以进行克隆
                    apps.add(pak);
                }
            }
            return apps;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}