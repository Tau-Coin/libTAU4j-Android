package io.taucoin.torrent.publishing.ui;

import android.content.Context;
import android.content.Intent;

import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.taucoin.torrent.publishing.core.utils.FixMemLeak;

public abstract class BaseFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener {
    private BaseActivity baseActivity;
    private String className = getClass().getSimpleName();

    @Override
    public void onRefresh() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(getActivity() != null && getActivity() instanceof BaseActivity){
            baseActivity = (BaseActivity) getActivity();
        }
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
