package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndAccount;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<CommunityAndAccount, ChooseListAdapter.ViewHolder> {
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
        CommunityAndAccount community = getItem(position);
        holder.bindCommunity(community);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCommunityChooseBinding binding;
        private Context context;

        ViewHolder(ItemCommunityChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(CommunityAndAccount community) {
            if(null == community){
                return;
            }
            String communityName = community.communityName + " chain";
            binding.tvName.setText(communityName);

            String balance = FmtMicrometer.fmtBalance(community.balance);
            String time = DateUtil.formatTime(community.balUpdateTime, DateUtil.pattern14);
            String balanceAndTime = context.getResources().getString(R.string.drawer_balance_time,
                    balance, time);
            binding.tvBalance.setText(balanceAndTime);
        }
    }

    private static final DiffUtil.ItemCallback<CommunityAndAccount> diffCallback = new DiffUtil.ItemCallback<CommunityAndAccount>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndAccount oldItem, @NonNull CommunityAndAccount newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndAccount oldItem, @NonNull CommunityAndAccount newItem) {
            return oldItem.equals(newItem);
        }
    };
}
