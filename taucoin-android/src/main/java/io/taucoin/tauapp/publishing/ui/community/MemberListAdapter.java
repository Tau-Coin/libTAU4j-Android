package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndFriend;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ItemMemberListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class MemberListAdapter extends PagedListAdapter<MemberAndFriend, MemberListAdapter.ViewHolder> {
    private ClickListener listener;

    MemberListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMemberListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_member_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMemberListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemMemberListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, MemberAndFriend member) {
            if (null == holder || null == member) {
                return;
            }
            String showName = UsersUtil.getDefaultName(member.publicKey);
            if (member.user != null) {
                showName = UsersUtil.getShowName(member.user);
            }
            holder.binding.tvName.setText(showName);
            holder.binding.leftView.setImageBitmap(UsersUtil.getHeadPic(member.user));

            holder.binding.tvNonMember.setVisibility(!member.onChain() ? View.VISIBLE : View.GONE);

            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(member);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(MemberAndFriend item);
    }

    private static final DiffUtil.ItemCallback<MemberAndFriend> diffCallback = new DiffUtil.ItemCallback<MemberAndFriend>() {
        @Override
        public boolean areContentsTheSame(@NonNull MemberAndFriend oldItem, @NonNull MemberAndFriend newItem) {
            return oldItem.equals(newItem) && oldItem.balance == newItem.balance && oldItem.nonce == newItem.nonce;
        }

        @Override
        public boolean areItemsTheSame(@NonNull MemberAndFriend oldItem, @NonNull MemberAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
