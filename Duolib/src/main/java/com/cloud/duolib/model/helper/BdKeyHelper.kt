package com.cloud.duolib.model.helper

class BdKeyHelper {
    fun transCode(code: Int): String {
        var err = code.toString()
        if (err.startsWith("210")) {
            err = "业务出错"
        } else if (err.startsWith("196")) {
            err = "控制出错"
        } else if (err.startsWith("2000")) {
            err = "连接出错"
        } else if (err.startsWith("100")) {
            err = "加载出错"
        } else if (err.startsWith("300")) {
            err = "后台出错"
        } else if (err.startsWith("1310")) {
            err = "回应出错"
        } else if (err.startsWith("262")) {
            err = "其它错误"
        } else {
            err = "未知错误"
        }
        return err + code.toString()
    }
}