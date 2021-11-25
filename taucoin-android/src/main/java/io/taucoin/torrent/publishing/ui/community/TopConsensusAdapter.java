package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ConsensusInfo;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ItemChainTopBinding;

/**
 * 社区排名的Adapter
 */
public class TopConsensusAdapter extends ListAdapter<ConsensusInfo, TopConsensusAdapter.ViewHolder> {
    private ClickListener listener;
    private int type;

    TopConsensusAdapter(ClickListener listener, int type) {
        super(diffCallback);
        this.listener = listener;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChainTopBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_chain_top,
                parent,
                false);

        return new ViewHolder(binding, listener, type);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemChainTopBinding binding;
        private ClickListener listener;
        private int type;

        ViewHolder(ItemChainTopBinding binding, ClickListener listener, int type) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.type = type;
        }

        void bind(ViewHolder holder, ConsensusInfo info, int pos) {
            if(null == holder || null == info){
                return;
            }
            holder.binding.tvCol1.setText(String.valueOf(pos + 1));
            holder.binding.tvCol2.setText(String.valueOf(info.getNumber()));
            holder.binding.tvCol3.setText(info.getHash());

            ViewUtils.updateViewWeight(binding.tvCol2, 3);
            ViewUtils.updateViewWeight(binding.tvCol3, 4);
        }
    }

    public interface ClickListener {
        void onItemClicked(String publicKey);
    }

    private static final DiffUtil.ItemCallback<ConsensusInfo> diffCallback = new DiffUtil.ItemCallback<ConsensusInfo>() {
        @Override
        public boolean areContentsTheSame(@NonNull ConsensusInfo oldItem, @NonNull ConsensusInfo newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull ConsensusInfo oldItem, @NonNull ConsensusInfo newItem) {
            return oldItem.equals(newItem);
        }
    };
}
