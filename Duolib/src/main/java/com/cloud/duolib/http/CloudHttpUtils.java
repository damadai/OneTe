package com.cloud.duolib.http;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.cloud.duolib.CloudBuilder;
import com.cloud.duolib.bean.BaseInfo;
import com.cloud.duolib.bean.duo.DuoHelpData;
import com.cloud.duolib.bean.fre.CloudHostData;
import com.cloud.duolib.bean.fre.CloudLineData;
import com.cloud.duolib.bean.fre.CloudListData;
import com.cloud.duolib.bean.fre.CloudListNewData;
import com.cloud.duolib.bean.fre.CloudNodeData;
import com.cloud.duolib.bean.duo.DuoObData;
import com.cloud.duolib.model.util.CxtUtilsKt;
import com.cloud.duolib.model.util.EncodeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Response;

public class CloudHttpUtils {
    private static final String UM_DUO_REQUEST_ERROR = "云机出错-接口请求";

    private static CloudHttpUtils instance;

    public static CloudHttpUtils getInstance() {
        if (instance == null) {
            instance = new CloudHttpUtils();
        }
        return instance;
    }

    /**
     * 获取列表
     */
    public void getPriceRx(String pkg, int co, String app, ListResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiChoiceNew(getParamsServer(pkg, co, app))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<ArrayList<CloudListData>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<ArrayList<CloudListData>> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            response.onSuccess(baseInfo.getData());
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求列表:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getNewPriceRx(String pkg, int co, String app, ListNewResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiChoiceNewSpec(getParamsServer(pkg, co, app))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<CloudListNewData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<CloudListNewData> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            response.onSuccess(baseInfo.getData());
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求列表:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getNewHostRx(String pkg, int co, HostResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiChoiceNewHost(getParamsServer(pkg, co, null))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<ArrayList<CloudHostData>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<ArrayList<CloudHostData>> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            response.onSuccess(baseInfo.getData());
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "机房列表:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 排队
     */
    public void getLineRx(String pkg, int co, String token, Integer type, String key, String iv, LineResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiQueue(getParamsLine(pkg, co, token, type, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            String data = baseInfo.getData();
                            if (data != null) {
                                String str = encodeParamData(data, key, iv);
                                if (!TextUtils.isEmpty(str)) {
                                    CloudLineData result = new Gson().fromJson(str, new TypeToken<CloudLineData>() {
                                    }.getType());
                                    response.onSuccess(result);
                                } else {
                                    response.onSuccess(null);
                                }
                            } else {
                                response.onSuccess(null);
                            }
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求排队:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 退队
     */
    public void getExitRx(String pkg, int co, String token, int type, String key, String iv, StrResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiQuit(getParamsLine(pkg, co, token, type, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            String data = baseInfo.getData();
                            if (data != null) {
                                String str = encodeParamData(data, key, iv);
                                if (!TextUtils.isEmpty(str)) {
                                    response.onSuccess(str);
                                } else {
                                    response.onSuccess(data);
                                }
                            } else {
                                response.onSuccess(null);
                            }
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求退队:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 获取
     */
    public void getNodeRx(Context cxt, String pkg, int co, String token, Integer type, String appn, String ch, int alert, int refresh, Integer rId, String key, String iv, NodeResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiGet(getParamsGet(cxt, pkg, co, token, type, appn, ch, alert, refresh, rId, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            String data = baseInfo.getData();
                            if (data != null) {
                                String str = encodeParamData(data, key, iv);
                                if (!TextUtils.isEmpty(str)) {
                                    CloudNodeData result = new Gson().fromJson(str, new TypeToken<CloudNodeData>() {
                                    }.getType());
                                    response.onSuccess(result);
                                } else {
                                    response.onSuccess(null);
                                }
                            } else {
                                response.onSuccess(null);
                            }
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求云机:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 回收
     */
    public void getRecoverRx(String pkg, int co, String token, Integer type, int repair, String key, String iv, StrResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiRecycle(getParamsRecycle(pkg, co, token, type, repair, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            String data = baseInfo.getData();
                            if (data != null) {
                                String str = encodeParamData(data, key, iv);
                                if (!TextUtils.isEmpty(str)) {
                                    response.onSuccess(str);
                                } else {
                                    response.onSuccess(data);
                                }
                            } else {
                                response.onSuccess(null);
                            }
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "请求回收:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public interface BaseResponse {
        void onFailed(int status, String str);
    }

    public interface StrResponse extends BaseResponse {
        void onSuccess(String str);
    }

    public interface ListResponse extends BaseResponse {
        void onSuccess(ArrayList<CloudListData> data);
    }

    public interface ListNewResponse extends BaseResponse {
        void onSuccess(CloudListNewData data);
    }

    public interface HostResponse extends BaseResponse {
        void onSuccess(ArrayList<CloudHostData> data);
    }

    public interface LineResponse extends BaseResponse {
        void onSuccess(CloudLineData data);
    }

    public interface NodeResponse extends BaseResponse {
        void onSuccess(CloudNodeData data);
    }

    public interface ObsResponse extends BaseResponse {
        void onSuccess(DuoObData data);
    }

    public interface HelpResponse extends BaseResponse {
        void onSuccess(ArrayList<DuoHelpData> data);
    }

    private HashMap<String, String> getParamsServer(String pkg, int co, String appn) {
        HashMap<String, String> params = getBaseParam(pkg, co);
        if (!TextUtils.isEmpty(appn)) {
            CxtUtilsKt.logShow("77777", "getParamsServer " + appn);
            params.put("appn", appn);
        }
        return params;
    }

    private Map<String, String> getParamsLine(String pkg, int co, String token, int type, String mAesKey, String mAesIv) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, Integer> outParam = new HashMap<>();
        outParam.put("types", type);
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    private Map<String, String> getParamsGet(Context cxt, String pkg, int co, String token, int type, String appn, String ch, int alert, int refresh, Integer rId, String mAesKey, String mAesIv) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, Object> outParam = new HashMap<>();
        outParam.put("types", type);
        if (!TextUtils.isEmpty(appn)) {
            outParam.put("appn", appn);
        }
        outParam.put("ch", ch);
        outParam.put("alert", alert);
        outParam.put("refresh", refresh);
        outParam.put("uuid", getUUID(cxt));
        if (rId != null) {
            outParam.put("roomId", rId);
        }
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    private Map<String, String> getParamsRecycle(String pkg, int co, String token, int type, int repair, String mAesKey, String mAesIv) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, Object> outParam = new HashMap<>();
        outParam.put("types", type);
        outParam.put("repair", repair);
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    /**
     * base params
     */
    private Map<String, String> getBaseParam(String pkg, int co, String token) {
        Map<String, String> params = getBaseParam(pkg, co);
        params.put("to", token);
        return params;
    }

    private HashMap<String, String> getBaseParam(String pkg, int co) {
        HashMap<String, String> params = new HashMap<>();
        params.put("pkg", pkg);
        params.put("co", String.valueOf(co));
        params.put("os", "1");
        return params;
    }

    private <T> ObservableTransformer<T, T> setThread() {
        return upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * OTHERS
     */
    //上传第一步
    public void getUploadRx(String pkg, int co, String token, String md, String oid, String apk, String key, String iv, StrResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiUpload(paramsUpload(pkg, co, token, md, oid, apk, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            response.onSuccess(baseInfo.getData());
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "加密网路环境判断:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    //上传第二步
    public void getUploadSucRx(String pkg, int co, String token, String md, String oid, String path, String key, String iv, StrResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiUploadSuc(paramsUploadSuc(pkg, co, token, md, oid, path, key, iv))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull BaseInfo<String> baseInfo) {
                        if (baseInfo.getStatus() == 200) {
                            response.onSuccess(baseInfo.getMsg());
                        } else {
                            response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "加密网路环境判断:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    //获取桶
    public void getUploadObsRx(String pkg, int co, String token, String key, String iv, ObsResponse response) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiUploadObs(getBaseParam(pkg, co, token))
                .compose(setThread())
                .safeSubscribe(new Observer<Response<BaseInfo<String>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Response<BaseInfo<String>> baseInfoResponse) {
                        BaseInfo<String> baseInfo = baseInfoResponse.body();
                        if (baseInfo != null) {
                            if (baseInfo.getStatus() == 200) {
                                String data = baseInfo.getData();
                                if (data != null) {
                                    String str = encodeParamData(data, key, iv);
                                    if (!TextUtils.isEmpty(str)) {
                                        DuoObData result = new Gson().fromJson(str, new TypeToken<DuoObData>() {
                                        }.getType());
                                        result.setSafeTime(baseInfoResponse.headers().get("Date"));
                                        response.onSuccess(result);
                                    } else {
                                        response.onSuccess(null);
                                    }
                                } else {
                                    response.onSuccess(null);
                                }
                            } else {
                                response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                            }
                        } else {
                            response.onFailed(555, "落空");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        response.onFailed(555, UM_DUO_REQUEST_ERROR);
                        CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "加密桶:" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    //获取帮助
    public void getHelpRx(String pkg, int co, HelpResponse response) {
        if (CloudBuilder.INSTANCE.getUiStyle() == 2) {
            RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                    .apiGuideList(getBaseParam(pkg, co))
                    .compose(setThread())
                    .safeSubscribe(new Observer<BaseInfo<ArrayList<DuoHelpData>>>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull BaseInfo<ArrayList<DuoHelpData>> baseInfo) {
                            if (baseInfo.getStatus() == 200) {
                                response.onSuccess(baseInfo.getData());
                            } else {
                                response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            e.printStackTrace();
                            response.onFailed(555, UM_DUO_REQUEST_ERROR);
                            CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "获取帮助:" + e);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                    .apiHelpList()
                    .compose(setThread())
                    .safeSubscribe(new Observer<BaseInfo<ArrayList<DuoHelpData>>>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull BaseInfo<ArrayList<DuoHelpData>> baseInfo) {
                            if (baseInfo.getStatus() == 200) {
                                response.onSuccess(baseInfo.getData());
                            } else {
                                response.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            e.printStackTrace();
                            response.onFailed(555, UM_DUO_REQUEST_ERROR);
                            CloudBuilder.INSTANCE.getUMCallBack(UM_DUO_REQUEST_ERROR, "获取帮助:" + e);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    //上报错误
    public void getErrorRx(String pkg, int co, String token, String mAesKey, String mAesIv, int type, int fix, String cause, String phoneId, String ch, String model, String android, String expire, BaseResponse baseResponse) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiReportError(paramsReport(pkg, co, token, mAesKey, mAesIv, type, fix, cause, phoneId, ch, model, android, expire))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull BaseInfo baseInfo) {
                        baseResponse.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        baseResponse.onFailed(555, UM_DUO_REQUEST_ERROR);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    //yx上报错误
    public void getErrorReport(String pkg, int co, String token, String mAesKey, String mAesIv, int type, int fix, String cause, String phoneId, String ch, String model, String android, String expire, BaseResponse baseResponse) {
        RxService.createApi(APICloudFunction.class, CloudBuilder.INSTANCE.getMHost())
                .apiErrorReport(paramsReport(pkg, co, token, mAesKey, mAesIv, type, fix, cause, phoneId, ch, model, android, expire))
                .compose(setThread())
                .safeSubscribe(new Observer<BaseInfo>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull BaseInfo baseInfo) {
                        baseResponse.onFailed(baseInfo.getStatus(), baseInfo.getMsg());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        baseResponse.onFailed(555, UM_DUO_REQUEST_ERROR);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private Map<String, String> paramsUpload(String pkg, int co, String token, String md, String oid, String apk, String mAesKey, String mAesIv) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, String> outParam = new HashMap<>();
        outParam.put("md5", md);
        outParam.put("OrderId", oid);
        outParam.put("apk", apk);
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    private Map<String, String> paramsUploadSuc(String pkg, int co, String token, String md, String oid, String path, String mAesKey, String mAesIv) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, String> outParam = new HashMap<>();
        outParam.put("md5", md);
        outParam.put("OrderId", oid);
        outParam.put("path", path);
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    private Map<String, String> paramsReport(String pkg, int co, String token, String mAesKey, String mAesIv, int type, int fix, String cause, String phoneId, String ch, String model, String android, String expire) {
        Map<String, String> params = getBaseParam(pkg, co, token);
        //1
        HashMap<String, Object> outParam = new HashMap<>();
        outParam.put("type", type);
        outParam.put("fixType", fix);
        outParam.put("cause", cause);
        outParam.put("phoneId", phoneId);
        outParam.put("ch", ch);
        outParam.put("model", model);
        outParam.put("android", android);
        outParam.put("expire", expire);
        JSONObject jsonObject = new JSONObject(outParam);

        String data = jsonObject.toString();
        String encrypData = new String(EncodeUtils.encryptAES2Base64(data.getBytes(), mAesKey.getBytes(), "AES/CBC/PKCS5Padding", mAesIv.getBytes()), StandardCharsets.UTF_8);
        params.put("data", encrypData);

        return params;
    }

    /**
     * 数据解密解压
     */
    private String encodeParamData(String data, String AES_KEY, String AES_IV) {
        String resultData = null;
        try {
            byte[] decryptBase64AES = EncodeUtils.decryptBase64AES(
                    data.getBytes(),
                    AES_KEY.getBytes(),
                    "AES/CBC/PKCS5Padding",
                    AES_IV.getBytes()
            );
            resultData = new String(decryptBase64AES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultData;
    }

    /**
     * 获取uuid，此方法在部分手机上（例如小米11 android 11）可能获取不到，
     * 商用时获取uuid谨慎使用，此处仅用于演示
     *
     * @param context
     * @return
     */
    private String getUUID(Context context) {
        String uuid = null;
        try {
            uuid = Settings.System.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(uuid)) {
            uuid = "mciDemoDefaultUuid";
        }
        return uuid;
    }
}
