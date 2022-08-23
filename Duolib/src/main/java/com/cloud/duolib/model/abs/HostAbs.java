package com.cloud.duolib.model.abs;

public abstract class HostAbs extends PreFraBaseAbs {
    //选中
    public abstract void startSelectRoom(Integer id);

    public abstract Integer getType();

    public abstract String getName();

    public abstract String getBtn();
}