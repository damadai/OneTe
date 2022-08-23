package com.cloud.duolib.model.abs;

import com.cloud.duolib.bean.InitCloudData;

public abstract class PreActAbs {
    public abstract InitCloudData getApiRequestData();

    //刷新验证
    public abstract void startSetNewToken(String token);

    public abstract void startClickBack();

    public abstract void startQuitLine(Integer type);

    public abstract void startSetTitle(String title);

    //跳转排队
    public abstract void startFraLine(Integer type, String name, Integer rid);

    //跳转机房
    public abstract void startFraHost(Integer type, String name, String btn, boolean autoNext);

    //跳转选择
    public abstract void startFraServer(Integer type);

    //达到间隔允许
    public abstract boolean startCheckShowReward();

    //开始展示激励
    public abstract void startShowReward(boolean first, boolean nextHost, Integer type, String name, Integer rid, String btn);

    //激励确认
    public abstract void startDlgReward(boolean nextHost, Integer type, String name, Integer rid, String btn);
}