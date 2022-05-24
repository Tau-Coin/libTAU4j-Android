package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ItemTransactionListBinding;

/**
 * 社区同步列表的Adapter
 */
public class TransactionListAdapter extends ListAdapter<UserAndTx, TransactionListAdapter.ViewHolder> {
    public TransactionListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTransactionListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_transaction_list,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionListAdapter.ViewHolder holder, int position) {
        UserAndTx tx = getItem(position);
        holder.bindTransaction(tx);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemTransactionListBinding binding;
        private Resources resources;

        ViewHolder(ItemTransactionListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            Context context = binding.getRoot().getContext();
            resources = context.getResources();
        }

        void bindTransaction(UserAndTx tx) {
            if (null == tx) {
                return;
            }
            // Transfer from
            boolean isMyself = StringUtil.isEquals(tx.senderPk, MainApplication.getInstance().getPublicKey());
            StringBuilder stringBuilder = new StringBuilder();
            if (isMyself) {
                binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.receiver));
                String name = UsersUtil.getShowName(tx.receiver);
                binding.tvName.setText(resources.getString(R.string.community_transfer_to, name));
                stringBuilder.append("-");
                binding.tvAmount.setTextColor(resources.getColor(R.color.color_red));
            } else {
                binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
                String name = UsersUtil.getShowName(tx.sender);
                binding.tvName.setText(resources.getString(R.string.community_transfer_from, name));
                stringBuilder.append("+");
                binding.tvAmount.setTextColor(resources.getColor(R.color.color_yellow_dark));
            }
            stringBuilder.append(FmtMicrometer.fmtMiningIncome(tx.amount));
            binding.tvAmount.setText(stringBuilder.toString());
            binding.tvTime.setText(DateUtil.formatTime(tx.timestamp, DateUtil.pattern13));
        }
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<UserAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem) && oldItem.txStatus == newItem.txStatus;
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
