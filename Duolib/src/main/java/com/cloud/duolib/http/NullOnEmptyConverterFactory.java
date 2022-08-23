package com.cloud.duolib.http;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by T on 2018/6/28
 * 解决
 * 但当接口请求的数据为空，并不是返回理论意义上的空，null或者[]（数据集合空），
 * 而是返回没有响应体body，只有响应头header，content-length为0的Response
 *
 * NullOnEmptyConverterFactory必需在GsonConverterFactory之前addConverterFactory
 */

public class NullOnEmptyConverterFactory extends Converter.Factory {

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        final Converter<ResponseBody, ?> delegate = retrofit.nextResponseBodyConverter(this, type, annotations);
        return new Converter<ResponseBody,Object>() {
            @Override
            public Object convert(ResponseBody body) throws IOException {
                if (body.contentLength() == 0) return null;
                return delegate.convert(body);
            }
        };
    }
}