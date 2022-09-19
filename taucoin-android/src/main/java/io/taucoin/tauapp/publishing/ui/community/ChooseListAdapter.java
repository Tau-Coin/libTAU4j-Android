package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndAmount;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<MemberAndAmount, ChooseListAdapter.ViewHolder> {
    ChooseListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommunityChooseBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_choose,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChooseListAdapter.ViewHolder holder, int position) {
        MemberAndAmount member = getItem(position);
        holder.bindCommunity(member);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommunityChooseBinding binding;
        private final Context context;

        ViewHolder(ItemCommunityChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(MemberAndAmount member) {
            if (null == member) {
                return;
            }
            String communityName = ChainIDUtil.getName(member.chainID) + " chain";
            binding.tvName.setText(communityName);

            String balance = FmtMicrometer.fmtBalance(member.balance);
            String time = DateUtil.formatTime(member.balUpdateTime, DateUtil.pattern14);
            String balanceAndTime = context.getResources().getString(R.string.drawer_balance_time_color_no_title,
                    balance, time);
            binding.tvBalance.setText(Html.fromHtml(balanceAndTime));
            binding.tvBalancePending.setVisibility(member.amount > 0 ? View.VISIBLE : View.GONE);
            if (member.amount > 0) {
                String balancePending = FmtMicrometer.fmtBalance(member.amount);
                binding.tvBalancePending.setText(context.getResources().getString(R.string.drawer_balance_pending,
                        balancePending));
            }

            boolean isShowTips = member.incomeTime > 0 || member.rewardTime > 0 || member.pendingTime > 0;
            binding.viewTips.setVisibility(isShowTips ? View.VISIBLE : View.INVISIBLE);
            if (isShowTips) {
                int resId = R.drawable.circle_pink;
                if (member.incomeTime > member.rewardTime) {
                    resId = R.drawable.circle_red;
                    if (member.pendingTime >= member.incomeTime) {
                        resId = R.drawable.circle_yellow;
                    }
                } else {
                    if (member.pendingTime >= member.rewardTime) {
                        resId = R.drawable.circle_yellow;
                    }
                }
                binding.viewTips.setBackgroundResource(resId);
            }
        }
    }

    private static final DiffUtil.ItemCallback<MemberAndAmount> diffCallback = new DiffUtil.ItemCallback<MemberAndAmount>() {
        @Override
        public boolean areContentsTheSame(@NonNull MemberAndAmount oldItem, @NonNull MemberAndAmount newItem) {
            return oldItem.equals(newItem) &&
                    oldItem.balance == newItem.balance &&
                    oldItem.balUpdateTime == newItem.balUpdateTime &&
                    oldItem.pendingTime == newItem.pendingTime &&
                    oldItem.rewardTime == newItem.rewardTime &&
                    oldItem.incomeTime == newItem.incomeTime &&
                    oldItem.amount == newItem.amount;
        }

        @Override
        public boolean areItemsTheSame(@NonNull MemberAndAmount oldItem, @NonNull MemberAndAmount newItem) {
            return oldItem.equals(newItem);
        }
    };
}
