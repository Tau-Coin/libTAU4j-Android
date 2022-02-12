package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.databinding.ItemBalanceBinding;

/**
 * 余额列表的Adapter
 */
public class BalanceAdapter extends ListAdapter<CommunityAndMember, BalanceAdapter.ViewHolder> {

    BalanceAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBalanceBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_balance,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceAdapter.ViewHolder holder, int position) {
        CommunityAndMember community = getItem(position);
        holder.bindCommunity(community);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemBalanceBinding binding;

        ViewHolder(ItemBalanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindCommunity(CommunityAndMember community) {
            String communityName = ChainIDUtil.getName(community.chainID);
            String balance = FmtMicrometer.fmtBalance(community.balance);
            binding.tvName.setText(communityName);
            binding.tvBalance.setText(balance);
        }
    }

    private static final DiffUtil.ItemCallback<CommunityAndMember> diffCallback = new DiffUtil.ItemCallback<CommunityAndMember>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem) &&
                    oldItem.balance == newItem.balance;
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}
