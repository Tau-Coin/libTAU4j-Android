package io.taucoin.news.publishing.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.UserAndFriend;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.SpanUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ItemAddMembersBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class MembersAdapter extends ListAdapter<UserAndFriend, MembersAdapter.ViewHolder> {
    private ClickListener listener;

    MembersAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAddMembersBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_add_members,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAddMembersBinding binding;
        private ClickListener listener;

        ViewHolder(ItemAddMembersBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, User user) {
            if(null == holder || null == user){
                return;
            }
            holder.binding.cbSelect.setVisibility(View.GONE);
            holder.binding.etAirdropCoins.setVisibility(View.GONE);
            String showName = UsersUtil.getShowName(user, user.publicKey);
            Context context = binding.getRoot().getContext();
            SpanUtils showNameBuilder = new SpanUtils()
                    .append(showName)
                    .append(context.getString(R.string.common_parentheses,
                            UsersUtil.getHideLastPublicKey(user.publicKey)))
                    .setForegroundColor(context.getResources().getColor(R.color.gray_dark))
                    .setFontSize(14, true);

            holder.binding.tvName.setText(showNameBuilder.create());
            holder.binding.leftView.setImageBitmap(UsersUtil.getHeadPic(user));
            holder.binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.onSelectClicked(user.publicKey);
                }
            });
        }
    }

    public interface ClickListener {
        void onSelectClicked(String publicKey);
    }

    private static final DiffUtil.ItemCallback<UserAndFriend> diffCallback = new DiffUtil.ItemCallback<UserAndFriend>() {

        @Override
        public boolean areContentsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
