package io.taucbd.news.publishing.ui.community;

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
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<Member, ChooseListAdapter.ViewHolder> {
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
        Member member = getItem(position);
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
        void bindCommunity(Member member) {
            if (null == member) {
                return;
            }
            String communityName = ChainIDUtil.getName(member.chainID);
            String communityCode = ChainIDUtil.getCode(member.chainID);
            binding.tvName.setText(context.getString(R.string.main_community_name, communityName, communityCode));

            String balance = FmtMicrometer.fmtBalance(member.getInterimBalance());
//            String time = DateUtil.formatTime(member.balUpdateTime, DateUtil.pattern14);
            String balanceAndTime = context.getResources().getString(R.string.drawer_balance_time_color_no_title,
                    balance);
            binding.tvBalance.setText(Html.fromHtml(balanceAndTime));
            binding.tvBalancePending.setVisibility(View.GONE);

//            boolean isShowTips = member.incomeTime > 0 || member.rewardTime > 0 || member.pendingTime > 0;
            boolean isShowTips = member.pendingTime > 0;
            binding.viewTips.setVisibility(isShowTips ? View.VISIBLE : View.INVISIBLE);
            if (isShowTips) {
//                int resId = R.drawable.circle_pink;
//                if (member.incomeTime > member.rewardTime) {
//                    resId = R.drawable.circle_red;
//                    if (member.pendingTime >= member.incomeTime) {
//                        resId = R.drawable.circle_yellow;
//                    }
//                } else {
//                    if (member.pendingTime >= member.rewardTime) {
//                        resId = R.drawable.circle_yellow;
//                    }
//                }
                int resId = R.drawable.circle_red;
                binding.viewTips.setBackgroundResource(resId);
            }
        }
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem) &&
                    oldItem.balance == newItem.balance &&
                    oldItem.totalOffchainCoins == newItem.totalOffchainCoins &&
                    oldItem.balUpdateTime == newItem.balUpdateTime &&
                    oldItem.pendingTime == newItem.pendingTime;
//                    oldItem.rewardTime == newItem.rewardTime &&
//                    oldItem.incomeTime == newItem.incomeTime;
        }

        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
