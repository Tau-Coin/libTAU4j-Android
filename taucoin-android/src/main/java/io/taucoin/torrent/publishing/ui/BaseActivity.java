package io.taucoin.torrent.publishing.ui;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.ui.customviews.ProgressManager;

public abstract class BaseActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    private ProgressManager progressManager = null;
    protected Point point = new Point();

    private boolean isFullScreen = true;
    public void setIsFullScreen(boolean isFullScreen){
        this.isFullScreen = isFullScreen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(isFullScreen){
            setTheme(Utils.getAppTheme(getApplicationContext()));
        }
//        if(isFullScreen || Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
//            ActivityUtil.setRequestedOrientation(this);
//        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        closeProgressDialog();
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if(progressManager != null){
            progressManager.closeProgressDialog();
        }
        this.finish();
    }

    public void showProgressDialog(){
        showProgressDialog(true);
    }

    public void showProgressDialog(boolean isCanCancel){
        progressManager = ProgressManager.newInstance();
        progressManager.showProgressDialog(this, isCanCancel);
    }

    public void closeProgressDialog() {
        if(progressManager != null){
            progressManager.closeProgressDialog();
        }
    }

    /**
     * Activity或Fragment视图被销毁回调
     * APP分屏操作，Activity重新加载，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getViewModelStore().clear();
    }

    /**
     * 实现用户自己定义字体大小
     */
    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(this);
        float fontScaleSize = settingsRepo.getFloatValue(Constants.PREF_KEY_FONT_SCALE_SIZE, 1.0f);
        Configuration configuration = resources.getConfiguration();
        if (fontScaleSize > 0) {
            configuration.fontScale = fontScaleSize;
        } else {
            configuration.fontScale = 1.0f;
        }
        resources.updateConfiguration(configuration, dm);
        return resources;
    }

    /**
     * 刷新所有视图， 保证字体大小修改成功
     */
    protected void refreshAllView() {
    }

    @Override
    public void onRefresh() {

    }

    public Point getPoint() {
        return point;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            point.x = (int) ev.getRawX();
            point.y = (int) ev.getRawY();
        }
        return super.dispatchTouchEvent(ev);
    }
}
