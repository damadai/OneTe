package com.cloud.duolib.model.helper

import com.cloud.duolib.model.OnBaseSucResponse
import com.cloud.duolib.model.OnBoolResponse
import com.cloud.duolib.model.OnStrResponse
import com.cloud.duolib.model.util.ToastUtils
import com.cloud.duolib.model.util.getCxt
import com.cloud.duolib.model.util.shell.TarCommander
import com.cloud.duolib.ui.duo.DeviceMediaActivity
import java.io.File

class MediaAppDopHelper {
    fun startAppLogin(
        act: DeviceMediaActivity,
        mCommandHelper: DuoCommandHelper?,
        mRebootHelper: MediaRebootHelper, pkgName: String, lauAct: String?,
        localPath: String, remotePath: String, rootControl: Boolean,
        _afterSuc: () -> Unit
    ) {
        val file = File(localPath)
        if (!file.exists()) {
            getCxt(act, {
                ToastUtils.showMsgShort(it, "请先在分身登录")
            }, null)
            return
        }
        getCxt(act, {
            ToastUtils.showMsgShort(it, "检查同步信息..请稍侯")
        }, null)
        //判断环境
        startLogin(
            act,
            mCommandHelper,
            if (rootControl) mRebootHelper else null,
            pkgName, lauAct,
            localPath, remotePath,
            _afterSuc
        )
    }

    private fun startLogin(
        act: DeviceMediaActivity,
        mCommandHelper: DuoCommandHelper?,
        mRebootHelper: MediaRebootHelper?,
        pkgName: String, lauAct: String?,
        localPath: String, remotePath: String,
        _afterSuc: () -> Unit
    ) {
        act.startPermissionStorage(object : OnBaseSucResponse {
            override fun onSuccess() {
                mCommandHelper?.getAppsClear(arrayListOf(pkgName),
                    object : OnStrResponse {
                        override fun onSuccess(str: String) {
                            startLoginFile(
                                act,
                                mCommandHelper,
                                mRebootHelper,
                                pkgName, lauAct,
                                localPath, remotePath,
                                _afterSuc
                            )
                        }

                        override fun onFailed(str: String) {
                            startLoginFile(
                                act,
                                mCommandHelper,
                                mRebootHelper,
                                pkgName, lauAct,
                                localPath, remotePath,
                                _afterSuc
                            )
                        }
                    })
            }
        })
    }

    private fun startLoginFile(
        act: DeviceMediaActivity,
        mCommandHelper: DuoCommandHelper?,
        mRebootHelper: MediaRebootHelper?,
        pkgName: String, lauAct: String?,
        localPath: String, remotePath: String,
        _afterSuc: () -> Unit
    ) {
        mCommandHelper?.getFilePush(
            localPath, remotePath,
            object : OnBoolResponse {
                override fun onSuccess(ok: Boolean) {
                    getCxt(act, {
                        act.runOnUiThread {
                            ToastUtils.showMsgLong(it, "同步" + (if (ok) "成功" else "失败"))
                        }
                        if (ok) {
                            _afterSuc.invoke()
                        }
                    }, null)
                    //清除缓存
                    try {
                        val tarCommander = TarCommander()
                        val result = tarCommander.startDeleteFoldCmd(600000, localPath)
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                    }
                    lauAct?.let {
                        mCommandHelper.getAppStart(pkgName, lauAct, object : OnStrResponse {
                            override fun onSuccess(str: String) {
                                mRebootHelper?.let {
                                    getRootState(mCommandHelper, mRebootHelper)
                                }
                            }

                            override fun onFailed(str: String) {
                                mRebootHelper?.let {
                                    getRootState(mCommandHelper, mRebootHelper)
                                }
                            }
                        })
                        return
                    }
                    mRebootHelper?.let {
                        getRootState(mCommandHelper, mRebootHelper)
                    }
                }

                override fun onFailed(str: String) {
                    getCxt(act, {
                        act.runOnUiThread {
                            ToastUtils.showMsgLong(it, "同步失败")
                        }
                    }, null)
                }
            })
    }

    private fun getRootState(
        mCommandHelper: DuoCommandHelper?,
        mRebootHelper: MediaRebootHelper
    ) {
        mCommandHelper?.getRootState(object : OnBoolResponse {
            override fun onSuccess(ok: Boolean) {
                if (ok) {
                    startRoot(
                        mCommandHelper,
                        mRebootHelper
                    )
                }
            }

            override fun onFailed(str: String) {
                startRoot(
                    mCommandHelper,
                    mRebootHelper
                )
            }
        })
    }

    private fun startRoot(
        mCommandHelper: DuoCommandHelper?,
        mRebootHelper: MediaRebootHelper
    ) {
        mRebootHelper.changeReboot = true
        mCommandHelper?.getOrderRoot(
            false,
            object : OnStrResponse {
                override fun onSuccess(str: String) {}

                override fun onFailed(str: String) {}
            }
        )
    }
}