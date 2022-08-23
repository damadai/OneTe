package com.cloud.duolib.http;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by T on 2018/6/28
 */

public class RxService {
    private RxService() {
        //construct
    }

    public static <T> T createApi(Class<T> clazz,String host) {
        return new Retrofit.Builder()
                .baseUrl(host)
                .client(ClientFactory.INSTANCE.getHttpClient())  // 不打印 网路请求
                //.client(ClientFactory.INSTANCE.getOkHttpClient())//todo打印 网路请求
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(new NullOnEmptyConverterFactory()) // 解决返回数据为空错误
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(clazz);
    }
}