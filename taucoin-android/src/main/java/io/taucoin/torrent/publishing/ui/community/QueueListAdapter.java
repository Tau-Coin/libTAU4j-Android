package io.taucoin.torrent.publishing.ui.community;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libTAU4j.Account;

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
    private Account account;

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
        holder.bindTransaction(tx, isReadOnly, account, position);
    }

    public void setReadOnly(boolean isReadOnly) {
        if (this.isReadOnly != isReadOnly) {
            this.isReadOnly = isReadOnly;
            notifyDataSetChanged();
        }
    }

    public void setAccount(Account account) {
        if (null == this.account && account != null) {
            this.account = account;
            notifyDataSetChanged();
            return;
        }
        if (this.account != null && account != null && (this.account.getBalance() != account.getBalance()
            || this.account.getNonce() != account.getNonce())) {
            this.account = account;
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
        void bindTransaction(TxQueueAndStatus tx, boolean isReadOnly, Account account, int pos) {
            if (null == tx) {
                return;
            }
            int progressText = tx.isProcessing() ? R.string.tx_result_status_processing :
                    R.string.tx_result_status_waiting;
            int progressColor = tx.status == 0 ? R.color.color_yellow : R.color.color_black;
            Resources resources = binding.getRoot().getResources();
            binding.tvProgress.setText(resources.getString(progressText));
            binding.tvProgress.setTextColor(resources.getColor(progressColor));
            binding.tvContent.setText(TxUtils.createSpanTxQueue(tx));
            binding.ivDelete.setVisibility(isReadOnly || tx.isProcessing() ? View.GONE : View.VISIBLE);
            binding.ivDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(tx);
                }
            });
            binding.ivEdit.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
            binding.ivEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(tx);
                }
            });
            String errorMsg = "";
            if (account != null) {
                if (account.getBalance() < tx.amount + tx.fee && !tx.isProcessing() && pos == 0) {
                    errorMsg = resources.getString(R.string.tx_error_insufficient_balance);
                } else if (account.getNonce() > tx.nonce + 1 && tx.isProcessing()) {
                    errorMsg = resources.getString(R.string.tx_error_nonce_conflict);
                }
            }
            binding.tvError.setVisibility((!isReadOnly && StringUtil.isNotEmpty(errorMsg))
                    ? View.VISIBLE : View.GONE);
            binding.tvError.setText(errorMsg);
        }
    }

    public interface ClickListener {
        void onDeleteClicked(TxQueueAndStatus tx);
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
