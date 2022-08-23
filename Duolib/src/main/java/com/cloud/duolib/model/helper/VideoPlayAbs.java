package com.cloud.duolib.model.helper;

public abstract class VideoPlayAbs {

    public abstract void startPermissionCamera();

    public abstract void startPermissionAudio();

    public abstract void errorOnSetParams();

    public abstract void errorOnStartPlay();

    public abstract void errorOnDisconnected(int errCode);
}
