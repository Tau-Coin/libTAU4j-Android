package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ItemChainTopBinding;
import io.taucoin.torrent.publishing.databinding.ItemMemberListBinding;

/**
 * 社区排名的Adapter
 */
public class ChainTopAdapter extends ListAdapter<MemberAndUser,
        ChainTopAdapter.ViewHolder> {
    private ClickListener listener;
    private int type;

    ChainTopAdapter(ClickListener listener, int type) {
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

        void bind(ViewHolder holder, MemberAndUser member, int pos) {
            if(null == holder || null == member){
                return;
            }
            holder.binding.tvCol1.setText(String.valueOf(pos + 1));
            if (type == ChainTopFragment.TOP_COIN) {
                String midHideName = UsersUtil.getMidHideName(member.publicKey, 6);
                holder.binding.tvCol2.setText(midHideName);
                holder.binding.tvCol2.setTextColor(binding.getRoot().getResources().getColor(R.color.color_blue));
                holder.binding.tvCol3.setText(FmtMicrometer.fmtBalance(member.balance));
            } else if (type == ChainTopFragment.TOP_POWER) {
                String midHideName = UsersUtil.getMidHideName(member.publicKey, 6);
                holder.binding.tvCol2.setText(midHideName);
                holder.binding.tvCol2.setTextColor(binding.getRoot().getResources().getColor(R.color.color_blue));
                holder.binding.tvCol3.setText(String.valueOf(member.power));
            } else {
                holder.binding.tvCol2.setText(String.valueOf(member.power));
                holder.binding.tvCol3.setText(member.chainID);
            }

            boolean isTopPeers = type == ChainTopFragment.TOP_COIN || type == ChainTopFragment.TOP_POWER;
            ViewUtils.updateViewWeight(binding.tvCol2, isTopPeers ? 3 : 3);
            ViewUtils.updateViewWeight(binding.tvCol3, isTopPeers ? 2 : 4);

            if (type == ChainTopFragment.TOP_COIN || type == ChainTopFragment.TOP_POWER) {
                holder.binding.tvCol2.setOnClickListener(v -> {
                    if(listener != null){
                        listener.onItemClicked(member.publicKey);
                    }
                });
            }
        }
    }

    public interface ClickListener {
        void onItemClicked(String publicKey);
    }

    private static final DiffUtil.ItemCallback<MemberAndUser> diffCallback = new DiffUtil.ItemCallback<MemberAndUser>() {
        @Override
        public boolean areContentsTheSame(@NonNull MemberAndUser oldItem, @NonNull MemberAndUser newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull MemberAndUser oldItem, @NonNull MemberAndUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
