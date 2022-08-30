package io.taucoin.tauapp.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.tauapp.publishing.core.utils.HashUtil;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ItemChainTopBinding;

/**
 * 社区排名的Adapter
 */
public class TopConsensusAdapter extends ListAdapter<Object, TopConsensusAdapter.ViewHolder> {

    TopConsensusAdapter() {
        super(diffCallback);
    }

    public void submitList(@Nullable List<Object> list) {
        super.submitList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChainTopBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_chain_top,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemChainTopBinding binding;

        ViewHolder(ItemChainTopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ViewHolder holder, Object obj, int pos) {
            if(null == holder || null == obj){
                return;
            }
            holder.binding.tvCol1.setText(String.valueOf(pos + 1));
            if (obj instanceof BlockInfo) {
                BlockInfo info = (BlockInfo) obj;
                holder.binding.tvCol2.setText(String.valueOf(info.blockNumber));
                holder.binding.tvCol3.setText(HashUtil.hashMiddleHide(info.blockHash));
            }
            ViewUtils.updateViewWeight(binding.tvCol2, 3);
            ViewUtils.updateViewWeight(binding.tvCol3, 4);
        }
    }

    private static final DiffUtil.ItemCallback<Object> diffCallback = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem instanceof BlockInfo) {
                return ((BlockInfo)oldItem).equals(newItem);
            }
            return true;
        }

        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem instanceof BlockInfo) {
                return ((BlockInfo)oldItem).equals(newItem);
            }
            return oldItem.equals(newItem);
        }
    };
}
