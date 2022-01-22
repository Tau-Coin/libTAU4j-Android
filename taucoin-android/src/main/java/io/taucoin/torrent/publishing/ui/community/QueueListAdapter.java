package io.taucoin.torrent.publishing.ui.community;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemTxQueueBinding;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;

/**
 * 交易队列的Adapter
 */
public class QueueListAdapter extends ListAdapter<TxQueueAndStatus, QueueListAdapter.ViewHolder> {
    private ClickListener listener;
    private boolean isReadOnly;

    public QueueListAdapter(ClickListener listener, boolean isReadOnly) {
        super(diffCallback);
        this.listener = listener;
        this.isReadOnly = isReadOnly;
    }

    QueueListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
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
        holder.bindTransaction(tx, isReadOnly);
    }

    public void setReadOnly(boolean isReadOnly) {
        if (this.isReadOnly != isReadOnly) {
            this.isReadOnly = isReadOnly;
            notifyDataSetChanged();
        }
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
        void bindTransaction(TxQueueAndStatus tx, boolean isReadOnly) {
            if (null == tx) {
                return;
            }
            int progressText = tx.status == 0 ? R.string.tx_result_status_processing :
                    R.string.tx_result_status_waiting;
            int progressColor = tx.status == 0 ? R.color.color_yellow : R.color.color_black;
            Resources resources = binding.getRoot().getResources();
            binding.tvProgress.setText(resources.getString(progressText));
            binding.tvProgress.setTextColor(resources.getColor(progressColor));
            binding.tvContent.setText(TxUtils.createSpanTxQueue(tx));
            binding.ivEdit.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
            binding.ivEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(tx);
                }
            });
        }
    }

    public interface ClickListener {
        void onEditClicked(TxQueueAndStatus tx);
    }

    private static final DiffUtil.ItemCallback<TxQueueAndStatus> diffCallback = new DiffUtil.ItemCallback<TxQueueAndStatus>() {
        @Override
        public boolean areContentsTheSame(@NonNull TxQueueAndStatus oldItem, @NonNull TxQueueAndStatus newItem) {
            return oldItem.equals(newItem) &&
                    oldItem.status == newItem.status &&
                    oldItem.amount == newItem.amount &&
                    oldItem.fee == newItem.fee &&
                    StringUtil.isEquals(oldItem.memo, newItem.memo);
        }

        @Override
        public boolean areItemsTheSame(@NonNull TxQueueAndStatus oldItem, @NonNull TxQueueAndStatus newItem) {
            return oldItem.equals(newItem);
        }
    };
}
