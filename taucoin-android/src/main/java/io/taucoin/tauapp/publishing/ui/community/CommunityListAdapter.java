package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.BuildConfig;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.databinding.ItemAirdropBinding;
import io.taucoin.tauapp.publishing.databinding.ItemCommunityBinding;
import io.taucoin.tauapp.publishing.databinding.ItemCommunitySelectBinding;

/**
 * 社区选择列表的Adapter
 */
public class CommunityListAdapter extends ListAdapter<Member, CommunityListAdapter.ViewHolder> {

    CommunityListAdapter() {
        super(diffCallback);
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
            String firstLetters = StringUtil.getFirstLettersOfName(communityName);
            binding.rbCommunity.setText(firstLetters);
            int bgColor = Utils.getGroupColor(member.chainID);
            binding.rbCommunity.setBgColor(bgColor);

            boolean isLondonPMC = StringUtil.isEquals(member.chainID, BuildConfig.TEST_CHAIN_ID);
            binding.rbCommunity.setVisibility(isLondonPMC ? View.GONE : View.VISIBLE);
            binding.ivCommunity.setVisibility(!isLondonPMC ? View.GONE : View.VISIBLE);

            String communityCode = ChainIDUtil.getCode(member.chainID);
            binding.tvName.setText(context.getString(R.string.main_community_name, communityName, communityCode));
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
