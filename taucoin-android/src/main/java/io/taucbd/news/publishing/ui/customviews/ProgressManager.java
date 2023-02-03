/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucbd.news.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.fragment.app.FragmentActivity;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.ui.BaseActivity;

/**
 * Description: Progress Manager
 */
public class ProgressManager {
    private static final Logger logger = LoggerFactory.getLogger("ProgressManager");
    private Dialog mDialog;

    public static ProgressManager newInstance() {
        return new ProgressManager();
    }

    private void showProgressDialog(BaseActivity activity){
        showProgressDialog(activity, true, null);
    }

    public void showProgressDialog(FragmentActivity activity){
        BaseActivity baseActivity = (BaseActivity) activity;
        showProgressDialog(baseActivity);
    }

    public void showProgressDialog(FragmentActivity activity, boolean isCanCancel, CharSequence text){
        BaseActivity baseActivity = (BaseActivity) activity;
        showProgressDialog(baseActivity, isCanCancel, text);
    }

    private void showProgressDialog(BaseActivity activity, boolean isCanCancel, CharSequence text){
        try {
            closeProgressDialog();
            logger.info("showProgressDialog");
            if(activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) != null){
                Dialog progress = new Dialog(activity, R.style.dialog_translucent);
                progress.requestWindowFeature(Window.FEATURE_NO_TITLE);
                progress.setCanceledOnTouchOutside(isCanCancel);
                progress.setCancelable(isCanCancel);
                mDialog = progress;
                if(!activity.isFinishing() || activity.isImmersive()){
                    progress.show();
                    progress.setContentView(R.layout.dialog_waiting);
                    // 背景透明
                    Window window = progress.getWindow();
                    if (window != null) {
                        TextView textView = window.findViewById(R.id.tv_text);
                        if (textView != null && text != null) {
                            textView.setText(text);
                        }
                        window.setBackgroundDrawable(new ColorDrawable(0));
                        window.setDimAmount(0f);
                        WindowManager.LayoutParams layout = window.getAttributes();
                        layout.alpha = 0.7f;
                        window.setAttributes(layout);
                    }
                } else {
                    closeProgressDialog();
                }
                mDialog.setOnCancelListener(ProgressManager::closeProgressDialog);
            }
        }catch (Exception ex){
            logger.error("showProgressDialog is error", ex);
        }
    }

    public void closeProgressDialog(){
        logger.info("showProgressDialog");
        if (isShowing()) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    private static void closeProgressDialog(DialogInterface dialog){
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private boolean isShowing(){
        return mDialog != null;
    }
}