package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ItemChainTopBinding;

/**
 * 社区排名的Adapter
 */
public class TopPeersAdapter extends ListAdapter<Member, TopPeersAdapter.ViewHolder> {
    private final ClickListener listener;

    TopPeersAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChainTopBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_chain_top,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChainTopBinding binding;
        private final ClickListener listener;

        ViewHolder(ItemChainTopBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, Member member, int pos) {
            if(null == holder || null == member){
                return;
            }
            holder.binding.tvCol1.setText(String.valueOf(pos + 1));
            String midHideName = UsersUtil.getMidHideName(member.publicKey, 6);
            holder.binding.tvCol2.setText(midHideName);
            holder.binding.tvCol2.setTextColor(binding.getRoot().getResources().getColor(R.color.color_yellow));
            holder.binding.tvCol3.setText(FmtMicrometer.fmtBalance(member.balance));

            ViewUtils.updateViewWeight(binding.tvCol2, 3);
            ViewUtils.updateViewWeight(binding.tvCol3, 2);
            holder.binding.tvCol2.setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(member.publicKey);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(String publicKey);
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
