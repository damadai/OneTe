package com.cloud.duolib.base;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.cloud.duolib.CloudBuilder;
import com.cloud.duolib.model.helper.MediaDialogHelper;
import com.cloud.duolib.model.util.CxtUtilsKt;
import com.cloud.duolib.model.util.PermissionUtils;
import com.cloud.duolib.model.util.PreferenceUtil;
import com.cloud.duolib.model.util.OsTimeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by T on 2021/3/20 13:35.
 */

public abstract class BasePermissionActivity extends AppCompatActivity {
    private InputMethodManager imm;
    protected BasePermissionActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBeforeView();
        setContentView(getLayoutId());
        initView(savedInstanceState);
        activity = this;
        CxtUtilsKt.logShow("777771", getClass().getSimpleName() + " onCreate");
        CloudBuilder.INSTANCE.add(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CxtUtilsKt.logShow("777771", getClass().getSimpleName() + " onDestroy");
        CloudBuilder.INSTANCE.remove(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CxtUtilsKt.logShow("777771", getClass().getSimpleName() + " onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        CxtUtilsKt.logShow("777771", getClass().getSimpleName() + " onPause");
    }

    /**
     * 关联layout
     **/
    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void initView(Bundle saveInstanceState);

    protected abstract void initBeforeView();

    public void hideSoftKeyBoard() {
        View localView = getCurrentFocus();
        if (this.imm == null) {
            this.imm = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
        }
        if ((localView != null) && (this.imm != null)) {
            this.imm.hideSoftInputFromWindow(localView.getWindowToken(), 2);
        }
    }

    private OnPermissionRequestListener mOnPermissionRequestListener;

    /**
     * 检查权限
     *
     * @param permissions                 权限集合
     * @param pkgName                     包名
     * @param imgResource                 开启权限弹窗图标
     * @param title                       弹窗标题
     * @param content                     开启权限弹窗内容
     * @param onPermissionRequestListener 回调监听
     */
    public void checkPermissions(MediaDialogHelper mediaDialogHelper, List<String> permissions, String pkgName, @NonNull int imgResource, String title, String content, BasePermissionActivity.OnPermissionRequestListener onPermissionRequestListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions.isEmpty()) {
                showPermissionDenyDialog(mediaDialogHelper, pkgName, title);
                return;
            }
            Set<String> lackedPermission = new HashSet<>();
            for (String permission : permissions) {
                if (!(activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)) {
                    lackedPermission.add(permission);
                }
            }
            if (lackedPermission.isEmpty()) {
                onPermissionRequestListener.onPermissionGranted();
                return;
            }
            if (!PermissionUtils.canApplyPermission()) {
                showPermissionDenyDialog(mediaDialogHelper, pkgName, title);
                return;
            }
            mOnPermissionRequestListener = onPermissionRequestListener;
            // 请求所缺少的权限，在onRequestPermissionsResult中再看是否获得权限，如果获得权限就可以调用SDK，否则不要调用SDK。
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            showOpenPermissionDialog(mediaDialogHelper, requestPermissions, imgResource, title, content);
        } else {
            onPermissionRequestListener.onPermissionGranted();
        }
    }

    private void showPermissionDenyDialog(MediaDialogHelper mediaDialogHelper, String pkgName, String title) {
        mediaDialogHelper.getViewPermission(activity, title, null, "您已拒绝此权限，如果继续使用请去设置开启",
                view -> PermissionUtils.gotoPermission(activity, pkgName), null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showOpenPermissionDialog(MediaDialogHelper mediaDialogHelper, String[] requestPermissions, int imgResource, String title, String info) {
        mediaDialogHelper.getViewPermission(activity, title, imgResource, info,
                view -> requestPermissions(requestPermissions, 4321),
                view -> {
                    if (mOnPermissionRequestListener != null) {
                        mOnPermissionRequestListener.onPermissionDenied(false);
                        mOnPermissionRequestListener = null;
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4321 && permissions.length > 0 && grantResults.length > 0) {
            boolean permissionGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Long nowDate = OsTimeUtils.getCurrentMillisecond();
                    PreferenceUtil.putLong(CloudBuilder.INSTANCE.getPermissionKey(), nowDate);
                    permissionGranted = false;
                }
            }
            if (mOnPermissionRequestListener != null) {
                if (permissionGranted) {
                    mOnPermissionRequestListener.onPermissionGranted();
                } else {
                    mOnPermissionRequestListener.onPermissionDenied(true);
                }
            }
            mOnPermissionRequestListener = null;
        }
    }

    public interface OnPermissionRequestListener {
        void onPermissionDenied(boolean showToast);

        void onPermissionGranted();
    }
}