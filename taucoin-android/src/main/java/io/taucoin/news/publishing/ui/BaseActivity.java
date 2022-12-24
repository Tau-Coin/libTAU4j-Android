package io.taucoin.news.publishing.ui;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.news.publishing.core.storage.RepositoryHelper;
import io.taucoin.news.publishing.core.utils.Utils;
import io.taucoin.news.publishing.ui.crash.CrashViewModel;
import io.taucoin.news.publishing.ui.customviews.ProgressManager;

public abstract class BaseActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    protected static Logger logger = LoggerFactory.getLogger("BaseActivity");
    private ProgressManager progressManager = null;
    protected Point point = new Point();

    private boolean isFullScreen = true;
    private String className = getClass().getSimpleName();
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

        if (savedInstanceState != null) {
            // 上传Crash文件
            ViewModelProvider provider = new ViewModelProvider(this);
            CrashViewModel crashViewModel = provider.get(CrashViewModel.class);
            crashViewModel.uploadDumpFile(this, false);
        }
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

    public void showProgressDialog(CharSequence text){
        showProgressDialog(true, text);
    }

    public void showProgressDialog(boolean isCanCancel){
        showProgressDialog(isCanCancel, null);
    }

    public void showProgressDialog(boolean isCanCancel, CharSequence text){
        progressManager = ProgressManager.newInstance();
        progressManager.showProgressDialog(this, isCanCancel, text);
    }

    public void closeProgressDialog() {
        if(progressManager != null){
            progressManager.closeProgressDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.debug("{} onStart", className);
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.debug("{} onResume", className);
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.debug("{} onPause", className);
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeProgressDialog();
        logger.debug("{} onStop", className);
    }

    /**
     * Activity或Fragment视图被销毁回调
     * APP分屏操作，Activity重新加载，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getViewModelStore().clear();
        logger.debug("{} onDestroy", className);
    }

    /**
     * 实现用户自己定义字体大小
     */
    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(this);
        String fontKey = resources.getString(R.string.pref_key_font_scale_size);
        float fontScaleSize = settingsRepo.getFloatValue(fontKey, Constants.DEFAULT_FONT_SCALE_SIZE);
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
//            TauDaemon.getInstance(getApplicationContext()).newActionEvent(DozeEvent.TOUCH_EVENT);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        TauDaemon.getInstance(getApplicationContext()).newActionEvent(DozeEvent.KEY_DOWN);
        return super.onKeyDown(keyCode, event);
    }
}
