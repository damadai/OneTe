package com.cloud.duolib.bean;

/**
 * Created by T on 2018/6/28
 */

public class BaseInfo<T> {
    private int status;
    private int code;
    private String msg;
    private T data;
    private T Data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.Data = data;
    }
}
