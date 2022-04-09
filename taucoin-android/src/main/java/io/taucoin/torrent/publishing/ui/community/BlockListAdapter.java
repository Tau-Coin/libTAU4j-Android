package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.databinding.ItemBlockListBinding;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;

/**
 * 社区同步列表的Adapter
 */
public class BlockListAdapter extends ListAdapter<BlockAndTx, BlockListAdapter.ViewHolder> {
    private ClickListener listener;
    public BlockListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBlockListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_block_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockListAdapter.ViewHolder holder, int position) {
        BlockAndTx block = getItem(position);
        holder.bindBlock(block);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemBlockListBinding binding;
        private ClickListener listener;
        private String blockStr;
        private String nothing;
        private BlockAndTx block;

        ViewHolder(ItemBlockListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            Context context = binding.getRoot().getContext();
            this.blockStr = context.getResources().getString(R.string.chain_block_and_number);
            this.nothing = context.getResources().getString(R.string.community_tip_block_txs_nothing);
        }

        void bindBlock(BlockAndTx block) {
            if (null == block) {
                return;
            }
            this.block = block;
            String blockNumber = String.format(blockStr, FmtMicrometer.fmtLong(block.blockNumber));
            binding.tvNumber.setText(blockNumber);
            binding.itemBlock.setOnClickListener(clickListener);
            if (binding.llBlockDetail.getVisibility() == View.VISIBLE) {
                binding.tvBlockDetail.setText(TxUtils.createBlockSpan(block, false));
                if (block.tx != null) {
                    binding.tvMsg.setText(TxUtils.createBlockTxSpan(block.tx));
                } else {
                    binding.tvMsg.setText(nothing);
                }
            }
            binding.tvOnChain.setVisibility(block.status == 1 ? View.VISIBLE : View.INVISIBLE);
            binding.tvBlockDetail.setOnLongClickListener(longListener);
            binding.tvMsg.setOnLongClickListener(longListener);
        }

        private View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (block.tx != null) {
                    binding.tvMsg.setText(TxUtils.createBlockTxSpan(block.tx));
                } else {
                    binding.tvMsg.setText(nothing);
                }
                binding.tvBlockDetail.setText(TxUtils.createBlockSpan(block, false));
                boolean isVisible = binding.llBlockDetail.getVisibility() == View.VISIBLE;
                binding.llBlockDetail.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                binding.ivDetail.setRotation(isVisible ? 90 : -90);
            }
        };

        private View.OnLongClickListener longListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    boolean isTx = v.getId() == R.id.tv_msg;
                    if (isTx) {
                        listener.onLongClick(block.tx, binding.tvMsg);
                    } else {
                        listener.onLongClick(block);
                    }
                }
                return false;
            }
        };
    }

    public interface ClickListener {
        void onLongClick(BlockAndTx block);
        void onLongClick(Tx tx, TextView view);
    }

    private static final DiffUtil.ItemCallback<BlockAndTx> diffCallback = new DiffUtil.ItemCallback<BlockAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull BlockAndTx oldItem, @NonNull BlockAndTx newItem) {
            return oldItem.equals(newItem) && oldItem.status == newItem.status;
        }

        @Override
        public boolean areItemsTheSame(@NonNull BlockAndTx oldItem, @NonNull BlockAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
