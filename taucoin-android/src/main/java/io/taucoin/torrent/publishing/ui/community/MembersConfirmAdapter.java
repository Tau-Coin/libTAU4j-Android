package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ItemAddMembersBinding;
import io.taucoin.torrent.publishing.databinding.ItemConfirmMembersBinding;

/**
 * 社区成员发币确认Adapter
 */
public class MembersConfirmAdapter extends ListAdapter<User, MembersConfirmAdapter.ViewHolder> {

    private Map<String, String> selectedMap;
    MembersConfirmAdapter(Map<String, String> selectedMap) {
        super(diffCallback);
        this.selectedMap = selectedMap;
    }

    private static String getInputCoins(Map<String, String> selectedMap, String publicKey) {
        return getInputCoins(selectedMap, publicKey, "");
    }

    @SuppressWarnings("ConstantConditions")
    private static String getInputCoins(Map<String, String> selectedMap, String publicKey, String defaultValue) {
        String value = defaultValue;
        if (selectedMap.containsKey(publicKey)) {
            value = selectedMap.get(publicKey);
        }
        return value;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemConfirmMembersBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_confirm_members,
                parent,
                false);

        return new ViewHolder(binding, selectedMap);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemConfirmMembersBinding binding;
        private Map<String, String> selectedMap;

        ViewHolder(ItemConfirmMembersBinding binding, Map<String, String> selectedMap) {
            super(binding.getRoot());
            this.binding = binding;
            this.selectedMap = selectedMap;
        }

        void bind(ViewHolder holder, User user) {
            if(null == holder || null == user){
                return;
            }
            String showName = UsersUtil.getShowName(user, user.publicKey);
            Context context = binding.getRoot().getContext();
            SpanUtils showNameBuilder = new SpanUtils()
                    .append(showName)
                    .append(context.getString(R.string.common_parentheses,
                            UsersUtil.getLastPublicKey(user.publicKey)))
                    .setForegroundColor(context.getResources().getColor(R.color.gray_dark))
                    .setFontSize(14, true);

            holder.binding.tvName.setText(showNameBuilder.create());
            holder.binding.tvAirdropCoins.setVisibility(View.VISIBLE);
            holder.binding.tvAirdropCoins.setText(getInputCoins(this.selectedMap, user.publicKey));
        }
    }

    private static final DiffUtil.ItemCallback<User> diffCallback = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }
    };
}
