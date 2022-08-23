package com.cloud.duolib.model.util

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cloud.duolib.CloudBuilder
import com.cloud.duolib.R
import com.cloud.duolib.http.NetUtil

fun getApp(fra: Fragment): Application? {
    if (fra.activity != null) {
        if (fra.activity?.isFinishing == false && fra.activity?.isDestroyed == false) {
            return fra.activity?.application
        }
    }
    return CloudBuilder.getApp()
}

fun getAct(fra: Fragment, cxtOk: (Activity) -> Unit) {
    if (fra.isAdded) {
        val act = fra.activity
        if (act != null) {
            if (!act.isFinishing && !act.isDestroyed) {
                cxtOk.invoke(act)
                return
            }
        }
    }
}

fun getCxt(act: Activity?, cxtOk: (Context) -> Unit, no: (() -> Unit)?) {
    if (act?.isFinishing == false && !act.isDestroyed) {
        cxtOk.invoke(act)
        return
    }
    no?.invoke()
}

fun getCxt(fra: Fragment, cxtOk: (Context) -> Unit, no: (() -> Unit)?) {
    if (fra.isAdded) {
        if (fra.context != null) {
            cxtOk.invoke(fra.requireContext())
            return
        }
    }
    no?.invoke()
}

fun getNet(net: () -> Unit, no: () -> Unit) {
    if (NetUtil.hasInternet()) {
        net.invoke()
    } else {
        no.invoke()
    }
}

fun getClick(click: () -> Unit) {
    if (!NoFastClickUtils.isFastClick(1000)) {
        click.invoke()
    }
}

fun getLongClick(click: () -> Unit) {
    if (!NoFastClickUtils.isFastClick(2000)) {
        click.invoke()
    }
}

fun getToast(fra: Fragment, msg: String?) {
    getCxt(fra, {
        Toast.makeText(it, msg ?: "", Toast.LENGTH_SHORT).show()
    }, {
        ToastUtils.showMsgShort(fra.context,msg)
    })
}

fun getToast(fra: Fragment, @StringRes msg: Int) {
    getCxt(fra, {
        Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
    }, {
        ToastUtils.showMsgShort(fra.context,msg)
    })
}

fun getToastLong(fra: Fragment, msg: String?) {
    getCxt(fra, {
        Toast.makeText(it, msg ?: "", Toast.LENGTH_LONG).show()
    }, {
        ToastUtils.showMsgLong(fra.context,msg)
    })
}

fun getToast(act: Activity?, msg: String?) {
    getCxt(act, {
        Toast.makeText(it, msg ?: "", Toast.LENGTH_SHORT).show()
    }, {
        ToastUtils.showMsgLong(act,msg)
    })
}

fun getToast(act: Activity?, @StringRes msg: Int?) {
    getCxt(act, {
        Toast.makeText(it, it.getString(msg ?: R.string.cxt_load), Toast.LENGTH_SHORT)
            .show()
    }, {
        ToastUtils.showMsgShort(act,msg ?: R.string.cxt_load)
    })
}

fun getCxtToast(cxt: Context?, @StringRes msg: Int?) {
    if (cxt != null) {
        Toast.makeText(cxt, cxt.getString(msg ?: R.string.cxt_load), Toast.LENGTH_SHORT)
            .show()
    } else {
        ToastUtils.showMsgShort(cxt,msg ?: R.string.cxt_load)
    }
}

fun getToastTest(fra: Fragment, msg: String?) {
    if (CloudBuilder.getShowLog()) {
        getCxt(fra, {
            Toast.makeText(it, msg ?: "", Toast.LENGTH_SHORT).show()
        }, {
            ToastUtils.showMsgShort(fra.context,msg)
        })
    }
}

fun getToastTest(act: Activity, msg: String?) {
    if (CloudBuilder.getShowLog()) {
        getCxt(act, {
            Toast.makeText(it, msg ?: "", Toast.LENGTH_SHORT).show()
        }, {
            ToastUtils.showMsgShort(act,msg)
        })
    }
}

//private val keyLogMap = mapOf(77200 to "成功数据77200", 77400 to "错误信息77400", 77500 to "错误响应77500", 77600 to "错误捕获77600")

fun logShow(tag: String = "77777", info: String) {
    if (CloudBuilder.getShowLog()) {
        Log.e(tag, info)
    }
}

fun getQQGroup(context: Context, key: String): Boolean {
    val intent = Intent()
    intent.data =
        Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
    // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
    // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        // 未安装手Q或安装的版本不支持
        false
    }
}

fun getStrClip(mAct: Activity, str: String) {
    ContextCompat.getSystemService(mAct, ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(null, str))
}