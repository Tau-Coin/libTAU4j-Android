package io.taucbd.news.publishing.ui.community;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.TxQueueAndStatus;
import io.taucbd.news.publishing.databinding.ItemTxQueueBinding;
import io.taucbd.news.publishing.ui.transaction.TxUtils;

/**
 * 交易队列的Adapter
 */
public class QueueListAdapter extends ListAdapter<TxQueueAndStatus, QueueListAdapter.ViewHolder> {
    private ClickListener listener;
    private static boolean isTopProcessing = false; // 顶部是否是处理中

    public QueueListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    public void submitList(@Nullable List<TxQueueAndStatus> list, boolean forceRefresh) {
        this.submitList(list);
        diffCallback.setForceRefresh(forceRefresh);
        if (forceRefresh) {
            this.notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTxQueueBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_tx_queue,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueListAdapter.ViewHolder holder, int position) {
        TxQueueAndStatus tx = getItem(position);
        holder.bindTransaction(tx, position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemTxQueueBinding binding;
        private ClickListener listener;

        ViewHolder(ItemTxQueueBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        /**
         * 绑定交易数据
         */
        void bindTransaction(TxQueueAndStatus tx, int pos) {
            if (null == tx) {
                return;
            }
            Resources resources = binding.getRoot().getResources();
            if (pos == 0) {
                isTopProcessing = true;
            }
            int progressText = isTopProcessing ? R.string.tx_result_status_processing :
                    R.string.tx_result_status_waiting;
            int progressColor = isTopProcessing ? R.color.color_yellow : R.color.color_black;

            binding.tvProgress.setText(resources.getString(progressText));
            binding.tvProgress.setTextColor(resources.getColor(progressColor));
            binding.tvContent.setText(TxUtils.createSpanTxQueue(tx, true));
            binding.ivDelete.setVisibility(View.GONE);
            binding.ivDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(tx);
                }
            });
            binding.ivEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(tx);
                }
            });

            binding.tvError.setVisibility(View.GONE);
        }
    }

    public interface ClickListener {
        void onDeleteClicked(TxQueueAndStatus tx);
        void onEditClicked(TxQueueAndStatus tx);
    }

    private static class ItemCallback extends DiffUtil.ItemCallback<TxQueueAndStatus> {

        private boolean forceRefresh = false;

        void setForceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
        }

        @Override
        public boolean areItemsTheSame(@NonNull TxQueueAndStatus oldItem, @NonNull TxQueueAndStatus newItem) {
            return !forceRefresh && oldItem.equals(newItem) &&
                    oldItem.status == newItem.status &&
                    oldItem.amount == newItem.amount &&
                    oldItem.fee == newItem.fee &&
                    oldItem.nonce == newItem.nonce &&
                    Arrays.equals(oldItem.content, newItem.content);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TxQueueAndStatus oldItem, @NonNull TxQueueAndStatus newItem) {
            return oldItem.equals(newItem);
        }
    };

    private static final ItemCallback diffCallback = new ItemCallback();
}
