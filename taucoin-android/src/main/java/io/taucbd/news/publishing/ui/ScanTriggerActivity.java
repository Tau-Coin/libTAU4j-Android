package io.taucbd.news.publishing.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.PermissionUtils;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.constant.SeedQRContent;
import io.taucbd.news.publishing.ui.customviews.permission.EasyPermissions;
import io.taucbd.news.publishing.ui.qrcode.ScanQRCodeActivity;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 触发扫码的页面需要
 */
public abstract class ScanTriggerActivity extends BaseActivity {

    protected static final int SCAN_CODE = 0X100;
    private User userTemp;
    private boolean isExit = false;
    private UserViewModel viewModel;
    private boolean scanKeyOnly = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void openScanQRActivityAndExit() {
        this.isExit = true;
        requestCameraPermissions();
    }

    protected void openScanQRActivity(User user) {
        this.userTemp = user;
        requestCameraPermissions();
    }

    protected void openScanQRActivity() {
        requestCameraPermissions();
    }

    public void openScanQRActivityForResult() {
        this.scanKeyOnly = true;
        requestCameraPermissions();
    }

    public void openScanQRActivity(UserViewModel viewModel) {
        this.scanKeyOnly = true;
        this.viewModel = viewModel;
        requestCameraPermissions();
    }

    private void directOpenScanQRActivity() {
        if (isExit) {
            onBackPressed();
            isExit = false;
        }
        Intent intent = new Intent();
        if (userTemp != null) {
            intent.putExtra(IntentExtra.BEAN, userTemp);
        }
        intent.putExtra(IntentExtra.SCAN_KEY_ONLY, scanKeyOnly);
        ActivityUtil.startActivityForResult(intent,this, ScanQRCodeActivity.class, SCAN_CODE);

    }
    /**
     * 请求摄像头权限
     */
    private void requestCameraPermissions() {
        String permission = Manifest.permission.CAMERA;
        if(!EasyPermissions.hasPermissions(this, permission)){
            EasyPermissions.requestPermissions(this,
                    this.getString(R.string.permission_tip_camera_denied),
                    PermissionUtils.REQUEST_PERMISSIONS_CAMERA, permission);
        } else {
            directOpenScanQRActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_CAMERA:
                if (grantResults.length > 0) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtils.checkUserBanPermission(this, (dialog, which) -> { },
                                permissions[0], R.string.permission_tip_camera_never_ask_again);
                    } else {
                        directOpenScanQRActivity();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SCAN_CODE) {
            if (data != null && viewModel != null) {
                String scanResult = data.getStringExtra(IntentExtra.DATA);
                SeedQRContent content = new Gson().fromJson(scanResult, SeedQRContent.class);
                viewModel.importSeed(content.getSeed(), content.getNickName());
            }
        }
    }
}
