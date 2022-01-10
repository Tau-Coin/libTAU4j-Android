package io.taucoin.torrent.publishing.ui;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.taucoin.torrent.publishing.core.utils.FixMemLeak;

public abstract class BaseFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener {
    private BaseActivity baseActivity;

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
            baseActivity.showProgressDialog();
        }
    }

    public void closeProgressDialog(){
        if(baseActivity != null){
            baseActivity.closeProgressDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getViewModelStore().clear();
        FixMemLeak.fixOPPOLeak(this);
        FixMemLeak.fixSamSungInputConnectionLeak(baseActivity);
    }

    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {

    }
}
