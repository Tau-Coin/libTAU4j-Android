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
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<CommunityAndMember, ChooseListAdapter.ViewHolder> {
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
        CommunityAndMember community = getItem(position);
        holder.bindCommunity(community);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCommunityChooseBinding binding;

        ViewHolder(ItemCommunityChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(CommunityAndMember community) {
            if(null == community){
                return;
            }
            binding.tvName.setText(community.communityName);
            binding.tvBalance.setText(FmtMicrometer.fmtBalance(community.balance));
        }
    }

    private static final DiffUtil.ItemCallback<CommunityAndMember> diffCallback = new DiffUtil.ItemCallback<CommunityAndMember>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}
