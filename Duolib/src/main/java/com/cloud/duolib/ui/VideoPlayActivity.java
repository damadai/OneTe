package com.cloud.duolib.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.baidu.armvm.api.SdkView;
import com.cloud.duolib.CloudBuilder;
import com.cloud.duolib.R;
import com.cloud.duolib.ResultRefreshCallBack;
import com.cloud.duolib.base.BasePermissionActivity;
import com.cloud.duolib.bean.InitCloudData;
import com.cloud.duolib.bean.fre.CloudNodeData;
import com.cloud.duolib.http.CloudHttpUtils;
import com.cloud.duolib.model.helper.ApiQuitHelper;
import com.cloud.duolib.model.helper.BdKeyHelper;
import com.cloud.duolib.model.helper.MediaDialogHelper;
import com.cloud.duolib.model.helper.PermissionApplyHelper;
import com.cloud.duolib.model.helper.VideoPlayAbs;
import com.cloud.duolib.model.util.CxtUtilsKt;
import com.cloud.duolib.view.StatusBarUtilsKt;
import com.mci.base.MCIKeyEvent;
import com.mci.commonplaysdk.ASdkCallback;
import com.mci.commonplaysdk.PlayMCISdkManager;
import com.mci.commonplaysdk.PlaySdkCallbackInterface;

import org.json.JSONException;
import org.json.JSONObject;

public class VideoPlayActivity extends BasePermissionActivity implements PlaySdkCallbackInterface, View.OnClickListener {
    private static final String TAG = VideoPlayActivity.class.getSimpleName();

    private PlayMCISdkManager mPlayMCISdkManager;

    private SdkView mSdkView; // video渲染用view

    private VideoPlayAbs videoPlayAbs;
    private ASdkCallback aSdkCallback;

    private PermissionApplyHelper permissionApplyHelper;

    private InitCloudData mInitCloudData;
    private Integer mType;
    private Integer mRoom;
    private String mExTime;

    private ApiQuitHelper apiQuitHelper;
    private int mRefreshTry = 0;
    private boolean mRefreshAble = true;

    private static final String VIDEO_VIEW_TIME = "VIDEO_VIEW_TIME";
    private static final String VIDEO_VIEW_SERVER_TYPE = "VIDEO_VIEW_SERVER_TYPE";
    private static final String VIDEO_VIEW_INIT_DATA = "VIDEO_VIEW_INIT_DATA";
    private static final String VIDEO_DATA_CONTENT = "VIDEO_DATA_CONTENT";
    private static final String VIDEO_DATA_RID = "VIDEO_DATA_RID";

    public static void newInstance(Activity act, String time, Integer type, InitCloudData data, String content, Integer rId) {
        if (CloudBuilder.INSTANCE.getActTop() instanceof VideoPlayActivity) return;
        Intent intent = new Intent(act, VideoPlayActivity.class);
        intent.putExtra(VIDEO_VIEW_TIME, time);
        intent.putExtra(VIDEO_VIEW_SERVER_TYPE, type);
        intent.putExtra(VIDEO_VIEW_INIT_DATA, data);
        intent.putExtra(VIDEO_DATA_CONTENT, content);
        intent.putExtra(VIDEO_DATA_RID, rId);
        if (CloudBuilder.INSTANCE.getActExist(VideoPlayActivity.class)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        act.startActivity(intent);
        if (act instanceof PreDataActivity) {
            act.finish();
        }
    }


    /**
     * 从connect接口返回的参数解析出package name
     *
     * @param contents
     * @return
     */
    private String parsePckName(String contents) {
        String pckName = null;
        if (!TextUtils.isEmpty(contents)) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(contents);
                if (jsonObject.has("resultInfo")) {
                    JSONObject resultInfo = jsonObject.getJSONObject("resultInfo");
                    if (resultInfo != null
                            && resultInfo.has("packname")) {
                        pckName = resultInfo.getString("packname");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return pckName;
    }

    /**
     * start game
     */
    private void startPlay(String content) {
        // groupId、packageName、padCode三个参数都是无效值时，会自动分配实列
        final int apiLevel = 2;
        final int useSSL = 0;

        // sdk使用软解还是硬解
        mPlayMCISdkManager = new PlayMCISdkManager(VideoPlayActivity.this, false);
        String pckName = parsePckName(content);
        // 3、set game parameters
        if (mPlayMCISdkManager.setParams(content, pckName, apiLevel, useSSL,
                mSdkView, VideoPlayActivity.this) != 0) {
            if (videoPlayAbs != null) {
                videoPlayAbs.errorOnSetParams();
            }
        }
        // 云端实列编号
        String mPadCode = mPlayMCISdkManager.getPadCode();
        // 4、start game
        if (mPlayMCISdkManager.start() != 0) {
            if (videoPlayAbs != null) {
                videoPlayAbs.errorOnStartPlay();
            }
        }

        //可交互音视频
        mPlayMCISdkManager.setUseSdkCollectVideo(true);
        mPlayMCISdkManager.setUseSdkCollectAudio(true);

        //监听回调
        mPlayMCISdkManager.setASdkCallback(aSdkCallback);
    }

    private void initDialog(Intent bundle) {
        //init dialog
        mInitCloudData = bundle.getParcelableExtra(VIDEO_VIEW_INIT_DATA);
        mType = bundle.getIntExtra(VIDEO_VIEW_SERVER_TYPE, 0);
        mRoom = bundle.getIntExtra(VIDEO_DATA_RID, 0);
        mExTime = bundle.getStringExtra(VIDEO_VIEW_TIME);
        new MediaDialogHelper().getInitFloatTime(
                this, mExTime, mType, mInitCloudData
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 6、resume game
        if (mPlayMCISdkManager != null) {
            mPlayMCISdkManager.resume();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.duo_activity_video_play;
    }

    @Override
    protected void initView(Bundle saveInstanceState) {
        mSdkView = findViewById(R.id.sdk_view);
        StatusBarUtilsKt.setNavigationBarTranslucent(VideoPlayActivity.this, true, null);

        //响应
        initAbs();

        //点按监听
        initListener();

        //倒计时
        initDialog(getIntent());

        // start game
        startPlay(getIntent().getStringExtra(VIDEO_DATA_CONTENT));
    }

    @Override
    protected void initBeforeView() {
        // 1、initialization environment
        PlayMCISdkManager.init(getApplication(), null, PlayMCISdkManager.LOG_DEBUG, CloudBuilder.INSTANCE.getShowLog(), null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 5、pasue game, 应用彻底不可见时暂停云端推流
        if (mPlayMCISdkManager != null) {
            mPlayMCISdkManager.pause();
        }
    }

    @Override
    public void onBackPressed() {
        this.findViewById(R.id.btBack_media).callOnClick();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop game and release resources
        if (mPlayMCISdkManager != null) {
            // 7、stop game
            mPlayMCISdkManager.stop();
            // 8、release resources
            mPlayMCISdkManager.release();
        }
    }

    @Override
    public void onReconnecting(int i) {
        CxtUtilsKt.logShow(TAG, "onDisconnected i = " + i);
    }

    @Override
    public void onConnected() {
        CxtUtilsKt.logShow(TAG, "onConnected");
    }

    @Override
    public void onDisconnected(final int i) {
        CxtUtilsKt.logShow(TAG, "onDisconnected i = " + i);
        if (videoPlayAbs != null) {
            videoPlayAbs.errorOnDisconnected(i);
        }
    }

    @Override
    public void onScreenRotation(int i) {
        CxtUtilsKt.logShow(TAG, "onScreenRotation i = " + i);
    }

    @Override
    public void onSensorInput(int i, int i1) {
        CxtUtilsKt.logShow(TAG, "onSensorInput i = " + i + ", i1 = " + i1);
    }

    @Override
    public void onPlayInfo(String s) {
        CxtUtilsKt.logShow(TAG, "onPlayInfo s = " + s);
    }

    @Override
    public void onRenderedFirstFrame(int i, int i1) {
        CxtUtilsKt.logShow(TAG, "onRenderedFirstFrame i = " + i + ", i1 = " + i1);
    }

    @Override
    public void onVideoSizeChanged(int i, int i1) {
        CxtUtilsKt.logShow(TAG, "onVideoSizeChanged i = " + i + ", i1 = " + i1);
    }

    @Override
    public void onTransparentMsg(int i, int i1, int i2, String s, String s1) {

    }

    @Override
    public void onTransparentMsg(int i, String s, String s1) {

    }

    @Override
    public void onTransparentMsgFail(int i, String s, String s1) {
        CxtUtilsKt.logShow(TAG, "onTransparentMsgFail i = " + i + ", s = " + s + ", s1 = " + s1);
    }

    @Override
    public void onControlVideo(int i, int i1) {

    }

    @Override
    public void onGameScreenshots(String s, byte[] bytes) {
        CxtUtilsKt.logShow(TAG, "onGameScreenshots s = " + s);
    }

    private PermissionApplyHelper getPermissionApplyHelper() {
        if (permissionApplyHelper == null) {
            permissionApplyHelper = new PermissionApplyHelper();
        }
        return permissionApplyHelper;
    }

    private void startError(Integer code) {
        runOnUiThread(() -> {
            if (CloudBuilder.INSTANCE.getIsFree()) {
                String error = new BdKeyHelper().transCode(code);
                new MediaDialogHelper().getViewFeedLine(VideoPlayActivity.this, error, mRefreshTry >= 3 ? "重新获取" : "刷新", view -> {
                    getApiQuitHelper().apiReportError(mInitCloudData.getApp_pkg(), mInitCloudData.getApp_token(), mInitCloudData.getApp_key(), mInitCloudData.getApp_iv(), mInitCloudData.getApp_co(),
                            error, code, 11, mPlayMCISdkManager.getPadCode(), mExTime, mInitCloudData.getApp_channel(),
                            (status, str) ->
                            {
                                if (mRefreshTry >= 3) {
                                    //超过3次
                                    startReGain();
                                } else if (mRefreshAble) {
                                    startRefresh();
                                }
                            });
                });
            } else {
                new MediaDialogHelper().getViewFeedback(VideoPlayActivity.this, mInitCloudData.getQq_number(), mInitCloudData.getQq_group());
            }
        });
    }

    private void startRefresh() {
        mRefreshAble = false;
        mRefreshTry++;
        if (CloudBuilder.INSTANCE.getIsFree()) {
            CloudHttpUtils.getInstance().getNodeRx(VideoPlayActivity.this,
                    mInitCloudData.getApp_pkg(), mInitCloudData.getApp_co(), mInitCloudData.getApp_token(),
                    mType, mInitCloudData.getAppn().getPkgName(), mInitCloudData.getApp_channel(), mInitCloudData.getAlert(),
                    1, mRoom, mInitCloudData.getApp_key(), mInitCloudData.getApp_iv(), new CloudHttpUtils.NodeResponse() {
                        @Override
                        public void onSuccess(CloudNodeData data) {
                            if (data != null && !TextUtils.isEmpty(data.getConnect())) {
                                startPlay(data.getConnect());
                            } else {
                                CxtUtilsKt.getToast(VideoPlayActivity.this, "刷新失败");
                            }
                            mRefreshAble = true;
                        }

                        @Override
                        public void onFailed(int status, String str) {
                            CxtUtilsKt.getToast(VideoPlayActivity.this, "刷新失败" + status + "，请退出重新获取");
                            mRefreshAble = true;
                        }
                    });
        } else {
            CloudBuilder.INSTANCE.getRefreshCallBack(new ResultRefreshCallBack() {
                @Override
                public void onSetNewUrl(String url) {
                    mRefreshAble = true;
                    if (!TextUtils.isEmpty(url)){
                        startPlay(url);
                        return;
                    }
                    CxtUtilsKt.getToast(VideoPlayActivity.this, "刷新失败");
                }
            });
        }
    }

    private void startReGain() {
        getApiQuitHelper().apiRecoveryDevice(1, mType, mInitCloudData, new CloudHttpUtils.StrResponse() {
            @Override
            public void onSuccess(String str) {
                PreDataActivity.Companion.newInstance(VideoPlayActivity.this, mInitCloudData, mType,0);
            }

            @Override
            public void onFailed(int status, String str) {
                PreDataActivity.Companion.newInstance(VideoPlayActivity.this, mInitCloudData, mType,0);
            }
        });
    }

    private ApiQuitHelper getApiQuitHelper() {
        if (apiQuitHelper == null) {
            apiQuitHelper = new ApiQuitHelper();
        }
        return apiQuitHelper;
    }

    private void initAbs() {
        if (videoPlayAbs == null) {
            videoPlayAbs = new VideoPlayAbs() {
                @Override
                public void startPermissionCamera() {
                    getPermissionApplyHelper().applyPermissionCamera(VideoPlayActivity.this, () -> {
                        if (mPlayMCISdkManager != null) {
                            mPlayMCISdkManager.openCamera();
                        }
                    }, new MediaDialogHelper());
                }

                @Override
                public void startPermissionAudio() {
                    getPermissionApplyHelper().applyPermissionRecord(VideoPlayActivity.this, new MediaDialogHelper());
                }

                @Override
                public void errorOnSetParams() {
                    CxtUtilsKt.logShow(TAG, "errorOnSetParams");
                    startError(20001);
                }

                @Override
                public void errorOnStartPlay() {
                    CxtUtilsKt.logShow(TAG, "errorOnSetParams");
                    startError(20001);
                }

                @Override
                public void errorOnDisconnected(int errCode) {
                    CxtUtilsKt.logShow(TAG, "errorOnDisconnected");
                    startError(errCode);
                }
            };
        }

        //sdk监听
        if (aSdkCallback == null) {
            aSdkCallback = new ASdkCallback() {
                @Override
                public void onSensorInput(int i, int i1, String s) {
                    super.onSensorInput(i, i1, s);
                }

                @Override
                public void onOutputBright(float v) {
                    super.onOutputBright(v);
                }

                @Override
                public void onRequestPermission(String s) {
                    super.onRequestPermission(s);
                    CxtUtilsKt.logShow(TAG, "onRequestPermission " + s);
                    if (s.equals("android.permission.CAMERA")) {
                        if (videoPlayAbs != null) {
                            videoPlayAbs.startPermissionCamera();
                        }
                    } else if (s.equals("android.permission.RECORD_AUDIO")) {
                        //todo find me
/*                        if (videoPlayAbs != null) {
                            videoPlayAbs.startPermissionAudio();
                        }*/
                    }
                }

                @Override
                public void onGameVideo(String s, String s1, int i) {
                    super.onGameVideo(s, s1, i);
                }

                @Override
                public void onCloudAppEvent(int i, int i1) {
                    super.onCloudAppEvent(i, i1);
                }

                @Override
                public void onDecodeVideoType(int i) {
                    super.onDecodeVideoType(i);
                }

                @Override
                public void onStreamingProtocol(int i) {
                    super.onStreamingProtocol(i);
                }

                @Override
                public void onCloudNotify(int i, String s) {
                    super.onCloudNotify(i, s);
                }
            };
        }
    }

    /**
     * 点按监听
     */
    private void initListener() {
        //隐藏开关
        this.findViewById(R.id.ivSet_media).setVisibility(View.GONE);
        this.findViewById(R.id.btAudio_media).setVisibility(View.GONE);
        //菜单按钮
        this.findViewById(R.id.btBack_media).setOnClickListener(this);
        this.findViewById(R.id.btHome_media).setOnClickListener(this);
        this.findViewById(R.id.btMenu_media).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int keyCode = -1;
        int id = view.getId();
        if (id == R.id.btMenu_media) {
            keyCode = MCIKeyEvent.KEYCOED_MENU;
        } else if (id == R.id.btHome_media) {
            keyCode = MCIKeyEvent.KEYCOED_HOME;
        } else if (id == R.id.btBack_media) {
            keyCode = MCIKeyEvent.KEYCOED_BACK;
        }

        if (keyCode > 0 && mPlayMCISdkManager != null) {
            mPlayMCISdkManager.sendKeyEvent(MCIKeyEvent.ACTION_UNDEFINED, keyCode);
        }
    }
}