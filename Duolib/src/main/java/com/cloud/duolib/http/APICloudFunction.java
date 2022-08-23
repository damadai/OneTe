package com.cloud.duolib.http;

import com.cloud.duolib.bean.BaseInfo;
import com.cloud.duolib.bean.duo.DuoHelpData;
import com.cloud.duolib.bean.fre.CloudHostData;
import com.cloud.duolib.bean.fre.CloudListData;
import com.cloud.duolib.bean.fre.CloudListNewData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface APICloudFunction {
    /**
     * FREE
     */
    //产品选择页
    @GET("phone/api/choiceAll4/")
    Observable<BaseInfo<ArrayList<CloudListData>>> apiChoiceNew(@QueryMap HashMap<String, String> params);

    //产品选择页
    @GET("phone/api/phoneChoice2/")
    Observable<BaseInfo<CloudListNewData>> apiChoiceNewSpec(@QueryMap HashMap<String, String> params);

    //机房选择页
    @GET("phone/api/serverChoice/")
    Observable<BaseInfo<ArrayList<CloudHostData>>> apiChoiceNewHost(@QueryMap HashMap<String, String> params);

    //申请云手机排队
    @FormUrlEncoded
    @POST("phone/api/queueUp/")
    Observable<BaseInfo<String>> apiQueue(@FieldMap Map<String, String> params);

    //退出排队
    @FormUrlEncoded
    @POST("phone/api/queueOut/")
    Observable<BaseInfo<String>> apiQuit(@FieldMap Map<String, String> params);

    //获取云手机
    @FormUrlEncoded
    @POST("phone/api/getPhone/")
    Observable<BaseInfo<String>> apiGet(@FieldMap Map<String, String> params);

    //回收云手机
    @FormUrlEncoded
    @POST("phone/api/recyclePhone/")
    Observable<BaseInfo<String>> apiRecycle(@FieldMap Map<String, String> params);

    //帮助
    @GET("phone/api/guide/")
    Observable<BaseInfo<ArrayList<DuoHelpData>>> apiGuideList(@QueryMap HashMap<String, String> params);

    /**
     * FEE
     */
    //帮助
    @GET("phone/api/help/")
    Observable<BaseInfo<ArrayList<DuoHelpData>>> apiHelpList();

    /**
     * OTHER
     */

    //安装标记次数
    @FormUrlEncoded
    @POST("phone/api/upload/")
    Observable<BaseInfo<String>> apiUpload(@FieldMap Map<String, String> params);

    //安装成功标记
    @FormUrlEncoded
    @POST("phone/api/upload_success/")
    Observable<BaseInfo<String>> apiUploadSuc(@FieldMap Map<String, String> params);

    //安装存储
    @FormUrlEncoded
    @POST("phone/api/obs/")
    Observable<Response<BaseInfo<String>>> apiUploadObs(@FieldMap Map<String, String> params);

    //错误
    @FormUrlEncoded
    @POST("phone/api/errorPhone/")
    Observable<BaseInfo<String>> apiReportError(@FieldMap Map<String, String> params);

    //错误
    @FormUrlEncoded
    @POST("phone/api/payError/")
    Observable<BaseInfo<String>> apiErrorReport(@FieldMap Map<String, String> params);
}