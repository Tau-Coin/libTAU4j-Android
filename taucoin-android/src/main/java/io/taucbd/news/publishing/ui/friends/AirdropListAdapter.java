package io.taucbd.news.publishing.ui.friends;

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
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.message.AirdropStatus;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.databinding.ItemAirdropBinding;

/**
 * 社区选择列表的Adapter
 */
public class AirdropListAdapter extends ListAdapter<Member, AirdropListAdapter.ViewHolder> {

    private OnClickListener listener;
    private boolean linksSelector;
    private Member member;

    AirdropListAdapter(OnClickListener listener, boolean linksSelector) {
        super(diffCallback);
        this.listener = listener;
        this.linksSelector = linksSelector;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAirdropBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_airdrop,
                parent,
                false);

        return new ViewHolder(this, binding);
    }

    public Member getMember() {
        return member;
    }

    @Override
    public void onBindViewHolder(@NonNull AirdropListAdapter.ViewHolder holder, int position) {
        Member member = getItem(position);
        holder.bindMember(member);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAirdropBinding binding;
        private final AirdropListAdapter adapter;
        private final Context context;

        ViewHolder(AirdropListAdapter adapter, ItemAirdropBinding binding) {
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
            binding.cbSelect.setVisibility(adapter.linksSelector ? View.VISIBLE : View.GONE);
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
            String name = ChainIDUtil.getName(member.chainID);
            String balance = FmtMicrometer.fmtBalance(member.getInterimBalance());
            String nameAndBalance = context.getResources().getString(R.string.drawer_balance_name_color,
                    name, balance);
            binding.tvLeft.setText(Html.fromHtml(nameAndBalance));
            AirdropStatus status = AirdropStatus.valueOf(member.airdropStatus);
            Resources resources = binding.getRoot().getResources();
            binding.tvRight.setText(resources.getString(status.getName()));
            binding.tvShare.setVisibility(status == AirdropStatus.ON ? View.VISIBLE : View.INVISIBLE);

            binding.tvShare.setOnClickListener(v -> {
                if (adapter.listener != null) {
                    long airdropTime = member.airdropTime / 60 / 1000;
                    adapter.listener.onShare(member.chainID, member.airdropCoins, airdropTime);
                }
            });
        }
    }

    interface OnClickListener {
        void onShare(String chainID, long airdropCoins, long airdropTime);
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
