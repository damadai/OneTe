package com.cloud.duolib.model.abs;

import android.app.Activity;

import com.cloud.duolib.bean.InitCloudData;

public abstract class PreFraBaseAbs {
    public abstract InitCloudData getApiRequestData();

    //刷新界面布局
    public abstract void startRefreshView(boolean autoNext);

    public abstract void startNextFra(Activity activity, boolean autoNext);
}