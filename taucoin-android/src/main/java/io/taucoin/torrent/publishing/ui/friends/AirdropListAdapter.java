package io.taucoin.torrent.publishing.ui.friends;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.databinding.ItemAirdropBinding;

/**
 * 社区选择列表的Adapter
 */
public class AirdropListAdapter extends ListAdapter<Member, AirdropListAdapter.ViewHolder> {
    AirdropListAdapter() {
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
    public void onBindViewHolder(@NonNull AirdropListAdapter.ViewHolder holder, int position) {
        Member member = getItem(position);
        holder.bindMember(member);
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
        void bindMember(Member member) {
            if (null == member) {
                return;
            }
            binding.tvLeft.setText(ChainIDUtil.getName(member.chainID));
            AirdropStatus status = AirdropStatus.valueOf(member.airdropStatus);
            Resources resources = binding.getRoot().getResources();
            binding.tvRight.setText(resources.getString(status.getName()));
            binding.tvRight.setTextColor(status == AirdropStatus.ON ? resources.getColor(R.color.color_yellow) :
                    resources.getColor(R.color.color_black));
        }
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem) &&
                    oldItem.airdropStatus == newItem.airdropStatus;
        }

        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
