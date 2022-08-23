/*
 * Copyright © 2020 XiaMen BaFenYi Network Technology Co., Ltd. All rights reserved.
 * 版权：厦门八分仪网络科技有限公司版权所有（C）2020
 * 作者：Administrator
 * 创建日期：2020年4月6日
 */

package com.cloud.duolib.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cloud.duolib.model.util.CxtUtilsKt;

/**
 * Created by T on 2021/3/20 13:35.
 */

public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(savedInstanceState);
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onViewCreated");
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void initView(Bundle savedInstanceState);

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onHiddenChanged " + hidden);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onDestroy");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onPause");
    }

    @Override
    public void onStart() {
        super.onStart();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        CxtUtilsKt.logShow("777772", getClass().getSimpleName() + " onStop");
    }
}
