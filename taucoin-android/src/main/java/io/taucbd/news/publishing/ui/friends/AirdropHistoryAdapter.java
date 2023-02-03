package io.taucbd.news.publishing.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.AirdropHistory;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemAirdropBinding;

/**
 * 社区选择列表的Adapter
 */
public class AirdropHistoryAdapter extends ListAdapter<AirdropHistory, AirdropHistoryAdapter.ViewHolder> {
    AirdropHistoryAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAirdropBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_airdrop,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AirdropHistoryAdapter.ViewHolder holder, int position) {
        AirdropHistory airdrop = getItem(position);
        holder.bindMember(airdrop);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAirdropBinding binding;

        ViewHolder(ItemAirdropBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定社区数据
         */
        void bindMember(AirdropHistory airdrop) {
            if (null == airdrop) {
                return;
            }
            binding.ivArrow.setVisibility(View.GONE);
            String showName = UsersUtil.getShowName(airdrop.friend, airdrop.receiverPk);
            binding.tvLeft.setText(showName);
        }
    }

    private static final DiffUtil.ItemCallback<AirdropHistory> diffCallback = new DiffUtil.ItemCallback<AirdropHistory>() {
        @Override
        public boolean areContentsTheSame(@NonNull AirdropHistory oldItem, @NonNull AirdropHistory newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull AirdropHistory oldItem, @NonNull AirdropHistory newItem) {
            return oldItem.equals(newItem);
        }
    };
}
