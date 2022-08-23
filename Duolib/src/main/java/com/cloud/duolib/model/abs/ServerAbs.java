package com.cloud.duolib.model.abs;

public abstract class ServerAbs extends PreFraBaseAbs {
    public abstract void startCountDown(boolean cancel, boolean start);

    public abstract void startSelectType(Integer selectType, boolean first, boolean autoNext);

    public abstract String getPayBtnInfo();

    //跳转判断
    public abstract boolean startCheckGotoHost();
}