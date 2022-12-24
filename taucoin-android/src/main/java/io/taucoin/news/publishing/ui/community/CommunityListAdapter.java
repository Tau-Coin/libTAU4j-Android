package io.taucoin.news.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.BuildConfig;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.databinding.ItemAirdropBinding;
import io.taucoin.news.publishing.databinding.ItemCommunityBinding;
import io.taucoin.news.publishing.databinding.ItemCommunitySelectBinding;

/**
 * 社区选择列表的Adapter
 */
public class CommunityListAdapter extends ListAdapter<Member, CommunityListAdapter.ViewHolder> {

    private String chainID;
    public CommunityListAdapter() {
        super(diffCallback);
    }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public String getChainID() {
        return chainID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommunitySelectBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_select,
                parent,
                false);

        return new ViewHolder(this, binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityListAdapter.ViewHolder holder, int position) {
        Member member = getItem(position);
        holder.bindMember(member);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommunitySelectBinding binding;
        private final CommunityListAdapter adapter;
        private final Context context;

        ViewHolder(CommunityListAdapter adapter, ItemCommunitySelectBinding binding) {
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
            String communityName = ChainIDUtil.getName(member.chainID);
            String communityCode = ChainIDUtil.getCode(member.chainID);
            String balance = FmtMicrometer.fmtBalance(member.getInterimBalance());
            binding.tvName.setText(context.getString(R.string.main_community_name_balance,
                    communityName, communityCode, balance));
            binding.ivSelected.setVisibility(StringUtil.isEquals(adapter.getChainID(), member.chainID)
                    ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<>() {
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
