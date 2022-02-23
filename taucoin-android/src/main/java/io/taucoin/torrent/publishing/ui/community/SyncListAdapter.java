package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.databinding.ItemSyncListBinding;

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
        ItemSyncListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_sync_list,
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
        private ItemSyncListBinding binding;
        private String blockStr;

        ViewHolder(ItemSyncListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            Context context = binding.getRoot().getContext();
            this.blockStr = context.getResources().getString(R.string.chain_block);
        }

        void bindBlock(BlockInfo block) {
            if (null == block) {
                return;
            }
            String blockNumber = blockStr + " " + FmtMicrometer.fmtLong(block.blockNumber);
            binding.tvNumber.setText(blockNumber);
            binding.itemBlock.setOnClickListener(l -> {
                View blockDetail = binding.itemBlockDetail.getRoot();
                boolean isVisible = blockDetail.getVisibility() == View.VISIBLE;
                blockDetail.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                binding.ivDetail.setRotation(isVisible ? 90 : -90);
            });

            binding.itemBlockDetail.tvMiner.setText(block.miner);
            binding.itemBlockDetail.tvHash.setText(block.blockHash);
            binding.itemBlockDetail.tvReward.setText(FmtMicrometer.fmtBalance(block.rewards));
            binding.itemBlockDetail.tvDifficulty.setText(FmtMicrometer.fmtDecimal(block.difficulty));
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
