package com.cloud.duolib

interface ResultRewardOkCallBack {
    /**
     * 关闭视频回调
     * 验证奖励是否有效
     */
    fun onCloseRewardVerify(showSuccess: Boolean)

    /**
     *  错误视频回调
     */
    fun onErrorRewardVerify()

    /**
     *  开始视频回调
     */
    fun onShowRewardVideo()
}

interface ResultNewTokenCallBack {
    /**
     * 刷新token后传入新值
     */
    fun onSetNewToken(newToken: String)
}

interface ResultRefreshCallBack {
    /**
     * 刷新获取后传入新值
     */
    fun onSetNewUrl(url: String?)
}