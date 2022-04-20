package io.taucoin.torrent.publishing.ui.community;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libTAU4j.Account;

import java.util.Arrays;

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
    private Account account;

    public QueueListAdapter(ClickListener listener) {
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
        holder.bindTransaction(tx, account, position);
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
        void bindTransaction(TxQueueAndStatus tx, Account account, int pos) {
            if (null == tx) {
                return;
            }
            int progressText = tx.isProcessing() && pos == 0 ? R.string.tx_result_status_processing :
                    R.string.tx_result_status_waiting;
            int progressColor = tx.isProcessing() && pos == 0 ? R.color.color_yellow : R.color.color_black;
            Resources resources = binding.getRoot().getResources();
            binding.tvProgress.setText(resources.getString(progressText));
            binding.tvProgress.setTextColor(resources.getColor(progressColor));
            binding.tvContent.setText(TxUtils.createSpanTxQueue(tx, pos == 0));
            binding.ivDelete.setVisibility(tx.isProcessing() ? View.GONE : View.VISIBLE);
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
            String errorMsg = "";
            if (account != null && pos == 0) {
                if (account.getBalance() < tx.amount + tx.fee && !tx.isProcessing()) {
                    errorMsg = resources.getString(R.string.tx_error_insufficient_balance);
                }
                // 取消nonce冲突提示，会主动重发
//                else if (account.getNonce() >= tx.nonce && tx.isProcessing()) {
//                    errorMsg = resources.getString(R.string.tx_error_nonce_conflict);
//                }
            }
            binding.tvError.setVisibility((StringUtil.isNotEmpty(errorMsg))
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
                    oldItem.nonce == newItem.nonce &&
                    Arrays.equals(oldItem.content, newItem.content);
        }

        @Override
        public boolean areItemsTheSame(@NonNull TxQueueAndStatus oldItem, @NonNull TxQueueAndStatus newItem) {
            return oldItem.equals(newItem);
        }
    };
}
