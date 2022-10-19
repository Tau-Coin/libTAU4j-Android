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
package io.taucoin.tauapp.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import io.taucoin.tauapp.publishing.R;

/**
 * 主要针对自定义确认对话框（底部有button的弹框，防止底部不显示）
 */
public class ConfirmDialog extends Dialog {

    public ConfirmDialog(Context context) {
        super(context);
    }

    public ConfirmDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context context;
        private boolean isCanCancel = true;
        private View contentView;
        private View warpView;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setContentView(View view) {
            this.contentView = view;
            return this;
        }

        public Builder setWarpView(View view) {
            this.warpView = view;
            return this;
        }

        public ConfirmDialog create() {
            final ConfirmDialog dialog = new ConfirmDialog(context, R.style.CommonDialog);
            dialog.addContentView(contentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            resetDialogWidthAndHeight();
            dialog.setCanceledOnTouchOutside(isCanCancel);
            return dialog;
        }

        private void resetDialogWidthAndHeight() {
            View layout = contentView;
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            LayoutParams layoutParams = layout.getLayoutParams();
            layoutParams.width = (int) (display.getWidth() * 0.85);
            layout.setLayoutParams(layoutParams);

            int rootMaxHeight = (int) (display.getHeight() * 0.8);
            int warpMaxHeight = (int) (display.getHeight() * 0.45);
            final boolean[] isFirst = {true};
            layout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (isFirst[0]) {
                    isFirst[0] = false;
                    int layoutHeight = layout.getHeight();
                    if (layoutHeight > rootMaxHeight) {
                        LayoutParams warpParams = warpView.getLayoutParams();
                        warpParams.height = warpMaxHeight;
                        warpView.setLayoutParams(warpParams);
                    }
                }
            });
        }
    }

    public void closeDialog(){
        dismiss();
    }

    public interface ClickListener {
        void proceed();
        void close();
    }
}