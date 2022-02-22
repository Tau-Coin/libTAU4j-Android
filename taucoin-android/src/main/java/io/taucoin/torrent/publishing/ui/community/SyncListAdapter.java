package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;

/**
 * 社区同步列表的Adapter
 */
public class SyncListAdapter extends ListAdapter<BlockInfo, SyncListAdapter.ViewHolder> {
    SyncListAdapter() {
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
    public void onBindViewHolder(@NonNull SyncListAdapter.ViewHolder holder, int position) {
        BlockInfo block = getItem(position);
        holder.bindBlock(block);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCommunityChooseBinding binding;

        ViewHolder(ItemCommunityChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindBlock(BlockInfo block) {
            binding.tvName.setText(FmtMicrometer.fmtLong(block.blockNumber));
        }
    }

    private static final DiffUtil.ItemCallback<BlockInfo> diffCallback = new DiffUtil.ItemCallback<BlockInfo>() {
        @Override
        public boolean areContentsTheSame(@NonNull BlockInfo oldItem, @NonNull BlockInfo newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull BlockInfo oldItem, @NonNull BlockInfo newItem) {
            return oldItem.equals(newItem);
        }
    };
}
