package io.taucbd.news.publishing.ui.setting;

import android.os.Bundle;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.ui.BaseActivity;

/**
 * 流量提示页面
 */
@Deprecated
public class TrafficTipsActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("TrafficTipsActivity");
    private AlertDialog mDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);
        logger.info("Show no remaining data tips dialog start");
        showNoRemainingDataTipsDialog();
    }

    /**
     * 显示没有剩余流量提示对话框
     */
    public void showNoRemainingDataTipsDialog() {
        int leftButton = R.string.cancel;
        int rightButton = R.string.common_proceed;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.setting_daily_traffic_limit_used_up)
                .setNegativeButton(leftButton, null)
                .setPositiveButton(rightButton, null)
                .setCancelable(false);

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        setMessageStyle(mDialog);

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mDialog.cancel();
            handleUserSelected(false);
        });

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            mDialog.cancel();
            handleUserSelected(true);
        });
    }

    /**
     * 处理用户流量提示选择
     * @param isProceed 是否继续
     */
    private void handleUserSelected(boolean isProceed) {
//        boolean updateDailyDataLimit = false;
//        if (isProceed) {
//            int pos = NetworkSetting.getMeteredLimitPos();
//            int[] meteredLimits = NetworkSetting.getMeteredLimits();
//            for (int i = 0; i < meteredLimits.length; i++) {
//                if (pos == i && i < meteredLimits.length - 1) {
//                    NetworkSetting.setMeteredLimitPos(i + 1, false);
//                    NetworkSetting.updateMeteredSpeedLimit();
//                    updateDailyDataLimit = true;
//                    break;
//                }
//            }
//        }
//        TauDaemon.getInstance(this).handleUserSelected(updateDailyDataLimit);
        this.finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        handleUserSelected(false);
    }
}