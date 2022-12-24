package io.taucoin.news.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.news.publishing.core.utils.DateUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ItemTransactionListBinding;

/**
 * 社区同步列表的Adapter
 */
public class MiningIncomeListAdapter extends ListAdapter<IncomeAndExpenditure, MiningIncomeListAdapter.ViewHolder> {

    private ClickListener listener;
    public MiningIncomeListAdapter(ClickListener listener) {
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
    public void onBindViewHolder(@NonNull MiningIncomeListAdapter.ViewHolder holder, int position) {
        IncomeAndExpenditure tx = getItem(position);
        holder.bindTransaction(tx);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionListBinding binding;
        private final Resources resources;
        private final ClickListener listener;

        ViewHolder(ItemTransactionListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            Context context = binding.getRoot().getContext();
            resources = context.getResources();
        }

        void bindTransaction(IncomeAndExpenditure entry) {
            if (null == entry) {
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
            binding.tvName.setText(resources.getString(R.string.community_mining_rewards,
                        FmtMicrometer.fmtLong(entry.blockNumber)));

            stringBuilder.append("+");
            stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount));
            binding.tvAmount.setTextColor(resources.getColor(R.color.color_yellow));
            binding.tvAmount.setText(stringBuilder.toString());
            binding.tvConfirmRate.setVisibility(View.GONE);

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

    private static final DiffUtil.ItemCallback<IncomeAndExpenditure> diffCallback = new DiffUtil.ItemCallback<>() {
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
