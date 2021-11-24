package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<Community, ChooseListAdapter.ViewHolder> {
    private String chainID;
    ChooseListAdapter(String chainID) {
        super(diffCallback);
        this.chainID = chainID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommunityChooseBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_choose,
                parent,
                false);

        return new ViewHolder(binding, chainID);
    }

    @Override
    public void onBindViewHolder(@NonNull ChooseListAdapter.ViewHolder holder, int position) {
        Community community = getItem(position);
        holder.bindCommunity(community);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCommunityChooseBinding binding;
        private String chainID;

        ViewHolder(ItemCommunityChooseBinding binding, String chainID) {
            super(binding.getRoot());
            this.binding = binding;
            this.chainID = chainID;
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(Community community) {
            if(null == community){
                return;
            }
            binding.tvName.setText(community.communityName);
            boolean isChoose = StringUtil.isEquals(chainID, community.chainID);
            int colorRes = binding.getRoot().getResources().getColor(isChoose ? R.color.color_blue :
                    R.color.color_black);
            binding.tvName.setTextColor(colorRes);
        }
    }

    private static final DiffUtil.ItemCallback<Community> diffCallback = new DiffUtil.ItemCallback<Community>() {
        @Override
        public boolean areContentsTheSame(@NonNull Community oldItem, @NonNull Community newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Community oldItem, @NonNull Community newItem) {
            return oldItem.equals(newItem);
        }
    };
}
