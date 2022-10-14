package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ItemTransactionListBinding;

/**
 * 社区同步列表的Adapter
 */
public class TransactionListAdapter extends ListAdapter<IncomeAndExpenditure, TransactionListAdapter.ViewHolder> {

    private ClickListener listener;
    public TransactionListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTransactionListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_transaction_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionListAdapter.ViewHolder holder, int position) {
        IncomeAndExpenditure tx = getItem(position);
        holder.bindTransaction(tx);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionListBinding binding;
        private final Resources resources;
        private final String confirmRate;
        private final String pendingText;
        private final String myPublicKey;
        private final ClickListener listener;

        ViewHolder(ItemTransactionListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            Context context = binding.getRoot().getContext();
            resources = context.getResources();
            confirmRate = resources.getString(R.string.community_tx_confidence);
            pendingText = resources.getString(R.string.community_tx_pending);
            myPublicKey = MainApplication.getInstance().getPublicKey();
        }

        void bindTransaction(IncomeAndExpenditure entry) {
            if (null == entry) {
                return;
            }
            // Transfer from
            boolean isMyself = StringUtil.isEquals(entry.senderOrMiner, myPublicKey);
            StringBuilder stringBuilder = new StringBuilder();
            if (isMyself && entry.txType != -1) {
                if (entry.txType != 2) {
                    int typeName = TxType.valueOf(entry.txType).getName();
                    binding.tvName.setText(resources.getString(R.string.community_post_news,
                            resources.getString(typeName)));
                    binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
                } else {
                    String name = UsersUtil.getShowName(entry.receiver);
                    binding.tvName.setText(resources.getString(R.string.community_transfer_to, name));
                    binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.receiver));
                }
                stringBuilder.append("-");
                stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount + entry.fee));
                binding.tvAmount.setTextColor(resources.getColor(R.color.color_red));
            } else {
                binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
                if (entry.txType == -1) {
                    binding.tvName.setText(resources.getString(R.string.community_mining_rewards,
                            FmtMicrometer.fmtLong(entry.blockNumber)));
                } else {
                    String name = UsersUtil.getShowName(entry.sender);
                    binding.tvName.setText(resources.getString(R.string.community_transfer_from, name));
                }
                stringBuilder.append("+");
                stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount));
                binding.tvAmount.setTextColor(resources.getColor(R.color.color_yellow));
            }
            binding.tvAmount.setText(stringBuilder.toString());

            if (entry.onlineStatus == 1) {
                binding.tvConfirmRate.setVisibility(View.VISIBLE);
                long currentTime = DateUtil.getTime();
                int rate = (int) ((currentTime - entry.onlineTime) * 100f / 60 / 180);
                rate = Math.min(100, rate);
                rate = Math.max(0, rate);
                String confirmRateStr = String.format(confirmRate, rate) + "%";
                binding.tvConfirmRate.setText(Html.fromHtml(confirmRateStr));
                binding.tvConfirmRate.setTextColor(resources.getColor(R.color.gray_dark));
            } else {
                binding.tvConfirmRate.setText(pendingText);
                binding.tvConfirmRate.setTextColor(resources.getColor(R.color.color_blue_dark));
            }
            binding.tvTime.setText(DateUtil.formatTime(entry.createTime, DateUtil.pattern13));

            binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemClicked(entry);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(IncomeAndExpenditure entry);
    }

    private static final DiffUtil.ItemCallback<IncomeAndExpenditure> diffCallback = new DiffUtil.ItemCallback<IncomeAndExpenditure>() {
        @Override
        public boolean areContentsTheSame(@NonNull IncomeAndExpenditure oldItem, @NonNull IncomeAndExpenditure newItem) {
            return oldItem.equals(newItem) && oldItem.onlineStatus == newItem.onlineStatus;
        }

        @Override
        public boolean areItemsTheSame(@NonNull IncomeAndExpenditure oldItem, @NonNull IncomeAndExpenditure newItem) {
            return oldItem.equals(newItem);
        }
    };
}
