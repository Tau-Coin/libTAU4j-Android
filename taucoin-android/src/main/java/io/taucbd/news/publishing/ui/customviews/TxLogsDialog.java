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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.TxLogStatus;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.databinding.ItemMsgLogBinding;
import io.taucbd.news.publishing.databinding.MsgLogsBinding;

/**
 * 消息发送历史展示
 */
public class TxLogsDialog extends Dialog {

    private LogsAdapter adapter;
    public TxLogsDialog(Context context) {
        super(context);
    }

    public TxLogsDialog(Context context, int theme) {
        super(context, theme);
    }

    public TxLogsDialog(Context context, int theme, LogsAdapter adapter) {
        this(context, theme);
        this.adapter = adapter;
    }

    public static class Builder {
        private Context context;
        private boolean isCanCancel = true;
        private boolean isResend = false;
        private MsgLogsListener msgLogsListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setResend(boolean resend) {
            this.isResend = resend;
            return this;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setMsgLogsListener(MsgLogsListener msgLogsListener) {
            this.msgLogsListener = msgLogsListener;
            return this;
        }

        public TxLogsDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            MsgLogsBinding binding = DataBindingUtil.inflate(inflater, R.layout.msg_logs,
                    null, false);
            LogsAdapter adapter = new LogsAdapter(msgLogsListener, isResend);
            final TxLogsDialog msgLogsDialog = new TxLogsDialog(context, R.style.CommonDialog, adapter);
            binding.ivClose.setOnClickListener(v -> {
                if (msgLogsDialog.isShowing()) {
                    msgLogsDialog.closeDialog();
                }
                if (msgLogsListener != null) {
                    msgLogsListener.onCancel();
                }
            });
            binding.recyclerView.setAdapter(adapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            binding.recyclerView.setLayoutManager(layoutManager);

            List<TxLogStatus> list = Arrays.asList(TxLogStatus.values());;
            adapter.submitList(list);

            View layout = binding.getRoot();
            msgLogsDialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            msgLogsDialog.setCanceledOnTouchOutside(isCanCancel);
            msgLogsDialog.setOnCancelListener(dialog -> {
                if (msgLogsListener != null) {
                    msgLogsListener.onCancel();
                }
            });
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            LayoutParams layoutParams = layout.getLayoutParams();
            layoutParams.width = (int) (display.getWidth() * 0.85);
            layout.setLayoutParams(layoutParams);
            return msgLogsDialog;
        }
    }

    private static class LogsAdapter extends ListAdapter<TxLogStatus, LogsAdapter.ViewHolder> {
        private MsgLogsListener listener;
        private List<TxLog> logs = new ArrayList<>();
        private boolean isResend = false;
        LogsAdapter(MsgLogsListener listener, boolean isResend) {
            super(diffCallback);
            this.listener = listener;
            this.isResend = isResend;
        }

        private void setLogsData(List<TxLog> logs) {
            if (logs != null) {
                this.logs.clear();
                this.logs.addAll(logs);
                notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemMsgLogBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_msg_log,
                    parent,
                    false);
            return new ViewHolder(binding, listener, logs, isResend);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int count = getCurrentList().size();
            holder.bind(holder, getItem(count - 1 - position), position, count);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private ItemMsgLogBinding binding;
            private MsgLogsListener listener;
            private List<TxLog> logs;
            private Context context;
            private int blackColor;
            private int grayColor;
            private boolean isResend = false;
            ViewHolder(ItemMsgLogBinding binding, MsgLogsListener listener, List<TxLog> logs, boolean isResend) {
                super(binding.getRoot());
                this.binding = binding;
                this.listener = listener;
                this.logs = logs;
                this.isResend = isResend;
                this.context = binding.getRoot().getContext();
                blackColor = context.getResources().getColor(R.color.color_black);
                grayColor = context.getResources().getColor(R.color.gray_dark);
            }

            public void bind(ViewHolder holder, TxLogStatus status, int pos, int size) {
                if (null == binding || null == holder || null == status) {
                    return;
                }
                binding.tvStatus.setText(status.getStatusInfo());
                binding.timeLineBottom.setVisibility(pos == size - 1 ? View.GONE : View.VISIBLE);

                int latestStatus = TxLogStatus.SENT.getStatus();
                if (logs.size() > 0) {
                    latestStatus = logs.get(0).status;
                }
                boolean isShowResend = isResend && latestStatus == status.getStatus();
                int timePointRes;
                if (status == TxLogStatus.ARRIVED_SWARM) {
                    isShowResend = false;
                    timePointRes = R.mipmap.icon_msg_swarm;
                } else {
                    timePointRes = R.mipmap.icon_msg_waitting;
                }
                TxLog currentLog = null;
                for (int i = 0; i < logs.size(); i++) {
                    TxLog log = logs.get(i);
                    if (log.status == status.getStatus()) {
                        currentLog = log;
                        break;
                    }
                }
                binding.timePoint.setImageResource(timePointRes);

                if (currentLog != null) {
                    String time = DateUtil.format(currentLog.timestamp, DateUtil.pattern9);
                    binding.tvTime.setText(time);
                    binding.tvTime.setVisibility(View.VISIBLE);
                    binding.tvTime.setTextColor(blackColor);
                    binding.tvStatus.setTextColor(blackColor);

                    binding.timePoint.setColorFilter(null);
                } else {
                    binding.tvTime.setVisibility(View.GONE);
                    binding.tvTime.setTextColor(grayColor);
                    binding.tvStatus.setTextColor(grayColor);

                    ColorMatrix cm = new ColorMatrix();
                    cm.setSaturation(0); // 设置饱和度
                    ColorMatrixColorFilter grayColorFilter = new ColorMatrixColorFilter(cm);
                    binding.timePoint.setColorFilter(grayColorFilter);
                }

                binding.tvResend.setVisibility(isShowResend ? View.VISIBLE : View.GONE);
                if (isShowResend) {
                    binding.tvResend.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onRetry();
                        }
                    });
                }
            }
        }

        private static final DiffUtil.ItemCallback<TxLogStatus> diffCallback = new DiffUtil.ItemCallback<TxLogStatus>() {
            @Override
            public boolean areContentsTheSame(@NonNull TxLogStatus oldItem, @NonNull TxLogStatus newItem) {
                return false;
            }

            @Override
            public boolean areItemsTheSame(@NonNull TxLogStatus oldItem, @NonNull TxLogStatus newItem) {
                return oldItem.getStatus() == newItem.getStatus();
            }
        };
    }

    public interface MsgLogsListener {
        void onRetry();
        void onCancel();
    }

    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }

    public void submitList(List<TxLog> logs) {
        if (adapter != null) {
            adapter.setLogsData(logs);
        }
    }
}