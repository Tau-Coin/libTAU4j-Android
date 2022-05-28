package io.taucoin.torrent.publishing.ui.splash;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.log.LogConfigurator;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.PermissionUtils;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.permission.EasyPermissions;
import io.taucoin.torrent.publishing.ui.download.DownloadViewModel;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

public class SplashActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("SplashActivity");
    private volatile boolean isAsk = false;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AlertDialog mDialog;
    private int dumpTimes = 5;
    private DownloadViewModel downloadViewModel;

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
            setContentView(R.layout.activity_splash);
            ViewModelProvider provider = new ViewModelProvider(this);
            downloadViewModel = provider.get(DownloadViewModel.class);

            // 每次APP重新启动如果有新版本更新需要提示用户
            RepositoryHelper.getSettingsRepository(this).setNeedPromptUser(true);

            logger.info("SplashActivity onCreate");

            requestWriteLogPermissions();

            // delay 3 seconds jump
            disposables.add(Observable.timer(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(aLong -> splashJump()));
        }
    }

    private synchronized void splashJump() {
        if (!isAsk) {
            checkDumpFile();
        }
        isAsk = false;
    }

    private void checkDumpFile() {
        logger.info("checkDumpFile start");
        File dir = new File(FileUtil.getDumpfileDir());
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        String timestampKey = getString(R.string.pref_key_dump_file_timestamp);
        String timesKey = getString(R.string.pref_key_dump_times);
        long timestamp = settingsRepo.getLongValue(timestampKey, 0);
        int times = settingsRepo.getIntValue(timesKey, 0);
        boolean isHaveDump = false;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                isHaveDump = true;
                File latestFile = null;
                for (File file : files) {
                    if (null == latestFile) {
                        latestFile = file;
                    }
                    if (file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }
                long currentTime = DateUtil.getMillisTime();
                int days = DateUtil.compareDay(timestamp, currentTime);
                if (days >= 1) {
                    times = 1;
                    settingsRepo.setIntValue(timesKey, times);
                    settingsRepo.setLongValue(timestampKey, currentTime);
                } else {
                    times += 1;
                }
                settingsRepo.setIntValue(timesKey, times);
                logger.info("days::{}, times::{}", days, times);
                logger.info("dump file::{}, timestamp::{}", latestFile.getAbsolutePath(),
                        DateUtil.format(latestFile.lastModified(), DateUtil.pattern6));
                showDumpDialog(latestFile, times);
            }
        }
        logger.info("checkDumpFile end");
        if (!isHaveDump) {
            splashJumpDirect();
        }
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
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
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

    private void showDumpDialog(File file, int times) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
        boolean manyTimes = times > dumpTimes;
        // 最后通过构造函数将样式传进去
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setCancelable(false)
            .setMessage(manyTimes ? R.string.dump_many_times : R.string.dump_report)
            .setNegativeButton(manyTimes ? R.string.common_report_exit : R.string.common_report, null)
            .setPositiveButton(R.string.common_proceed, null);

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        setMessageStyle(mDialog);

        downloadViewModel.getUploadResult().observe(this, result -> {
            closeProgressDialog();
            if (result.isSuccess()) {
                FileUtil.deleteFile(file.getParentFile());
                ToastUtils.showShortToast(R.string.dump_uploading_successfully);
            } else {
                ToastUtils.showShortToast(R.string.dump_uploading_failure);
            }
            if (manyTimes) {
                this.finish();
                int pid = android.os.Process.myPid();
                logger.info("exit::{}", pid);
                android.os.Process.killProcess(pid);
                System.exit(0);
            } else {
                splashJumpDirect();
            }
        });

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mDialog.cancel();
            showProgressDialog(getString(R.string.dump_uploading));
            downloadViewModel.dumpFileUpload(file);
        });

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            mDialog.cancel();
            FileUtil.deleteFile(file.getParentFile());
            splashJumpDirect();
        });
    }

    private void setMessageStyle(AlertDialog mDialog) {
        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(mDialog);
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            mMessageView.setLineSpacing(getResources().getDimensionPixelSize(R.dimen.widget_size_5), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}