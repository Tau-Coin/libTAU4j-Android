package io.taucoin.tauapp.publishing.ui.splash;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.BuildConfig;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.log.LogConfigurator;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ObservableUtil;
import io.taucoin.tauapp.publishing.core.utils.PermissionUtils;
import io.taucoin.tauapp.publishing.databinding.ActivitySplashBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.crash.CrashViewModel;
import io.taucoin.tauapp.publishing.ui.customviews.permission.EasyPermissions;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;

public class SplashActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("SplashActivity");
    private volatile boolean isAsk = false;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private CrashViewModel crashViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.info("SplashActivity.onCreate");
        if (!this.isTaskRoot()) {
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    logger.info("SplashActivity immediate finish");
                    finish();
                    ActivityUtil.moveTaskToFront();
                }
            }
        } else {
            ActivityUtil.fullScreenAll(this);
            logger.info("SplashActivity show");
            // Open for the first time
            ActivitySplashBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
            binding.appTitle.setText(BuildConfig.SPLASH_APP_NAME);

            ViewModelProvider provider = new ViewModelProvider(this);
            crashViewModel = provider.get(CrashViewModel.class);

            // 每次APP重新启动如果有新版本更新需要提示用户
            RepositoryHelper.getSettingsRepository(this).setNeedPromptUser(true);

            logger.info("SplashActivity onCreate");

            requestWriteLogPermissions();

            // delay 3 seconds jump
            disposables.add(Observable.timer(1500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(aLong -> splashJump()));
        }
    }

    private synchronized void splashJump() {
        if (!isAsk) {
            crashViewModel.getUploadResult().observe(this, result -> {
                closeProgressDialog();
                splashJumpDirect();
            });
            crashViewModel.uploadDumpFile(this, false);
        }
        isAsk = false;
    }

    private void splashJumpDirect() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        logger.info("Jump to MainActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
        disposables.clear();
    }

    /**
     * Shielded return key
     * */
    @Override
    public void onBackPressed() {

    }

    private void requestWriteLogPermissions() {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        Log.e("LoggerFactory", "requestWriteLogPermissions");
        if(!EasyPermissions.hasPermissions(this, permission)){
            isAsk = true;
            Log.e("LoggerFactory", "requestPermissions");
            EasyPermissions.requestPermissions(this,
                    this.getString(R.string.permission_tip_upgrade_denied),
                    permissionCallbacks,
                    PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
        }
    }

    private synchronized void handlePermissionsCallBack() {
        if(!isAsk){
            splashJump();
        }
        isAsk = false;
    }

    private EasyPermissions.PermissionCallbacks permissionCallbacks = new EasyPermissions.PermissionCallbacks(){

        @Override
        public void onPermissionsGranted(int requestCode, List<String> granted) {

        }

        @Override
        public void onPermissionsDenied(int requestCode, List<String> denied) {
            handlePermissionsCallBack();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_STORAGE:
                handlePermissionsCallBack();
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.e("LoggerFactory", "onRequestPermissionsResult");
                        LogConfigurator.configure();
                    }
                }
                break;
            default:
                break;
        }
    }
}