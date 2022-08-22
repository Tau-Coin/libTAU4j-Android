package io.taucoin.torrent.publishing.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.taucoin.torrent.publishing.core.utils.FixMemLeak;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

public abstract class BaseFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener {
    private BaseActivity baseActivity;
    private String className = getClass().getSimpleName();
    private String customTag;

    @Override
    public void onRefresh() {

    }

    public String getCustomTag() {
        return customTag;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            customTag = getArguments().getString(IntentExtra.CUSTOM_TAG, null);
        }
        BaseActivity.logger.debug("{} onViewCreated", className);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(getActivity() != null && getActivity() instanceof BaseActivity){
            baseActivity = (BaseActivity) getActivity();
        }
        BaseActivity.logger.debug("{} onAttach", className);
    }

    public void showProgressDialog(){
        showProgressDialog(true);
    }

    public void showProgressDialog(boolean isCanCancel){
        if(baseActivity != null){
            baseActivity.closeProgressDialog();
            baseActivity.showProgressDialog();
        }
    }

    public void closeProgressDialog(){
        if(baseActivity != null){
            baseActivity.closeProgressDialog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BaseActivity.logger.debug("{} onStart", className);
    }

    @Override
    public void onResume() {
        super.onResume();
        BaseActivity.logger.debug("{} onResume", className);
    }

    @Override
    public void onPause() {
        super.onPause();
        BaseActivity.logger.debug("{} onPause", className);
    }

    @Override
    public void onStop() {
        super.onStop();
        closeProgressDialog();
        BaseActivity.logger.debug("{} onStop", className);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getViewModelStore().clear();
        FixMemLeak.fixOPPOLeak(this);
        FixMemLeak.fixSamSungInputConnectionLeak(baseActivity);
        BaseActivity.logger.debug("{} onDestroy", className);
    }

    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {

    }
}
