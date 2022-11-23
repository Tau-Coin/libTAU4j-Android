package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.ItemAirdropBinding;
import io.taucoin.tauapp.publishing.databinding.ItemCommunityBinding;

/**
 * 社区选择列表的Adapter
 */
public class CommunityListAdapter extends ListAdapter<Member, CommunityListAdapter.ViewHolder> {

    private Member member;

    CommunityListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommunityBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_community,
                parent,
                false);

        return new ViewHolder(this, binding);
    }

    public Member getMember() {
        return member;
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityListAdapter.ViewHolder holder, int position) {
        Member member = getItem(position);
        holder.bindMember(member);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommunityBinding binding;
        private final CommunityListAdapter adapter;
        private final Context context;

        ViewHolder(CommunityListAdapter adapter, ItemCommunityBinding binding) {
            super(binding.getRoot());
            this.adapter = adapter;
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        /**
         * 绑定社区数据
         */
        void bindMember(Member member) {
            if (null == member) {
                return;
            }
            binding.cbSelect.setOnCheckedChangeListener(null);
            boolean checked = adapter.member != null && StringUtil.isEquals(adapter.member.chainID,
                    member.chainID);
            binding.cbSelect.setChecked(checked);
            if (checked) {
                adapter.member = member;
            }
            binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    adapter.member = member;
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.member = null;
                }
            });
            String communityName = ChainIDUtil.getName(member.chainID);
            String communityCode = ChainIDUtil.getCode(member.chainID);
            binding.tvLeft.setText(context.getString(R.string.main_community_name, communityName, communityCode));
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
