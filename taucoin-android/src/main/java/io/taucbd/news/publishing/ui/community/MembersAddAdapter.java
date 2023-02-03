package io.taucbd.news.publishing.ui.community;

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

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.databinding.ItemAddMembersBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class MembersAddAdapter extends ListAdapter<User, MembersAddAdapter.ViewHolder> {
    private ClickListener listener;
    private final long airdropCoin;
    private final Map<String, String> selectedMap = new HashMap<>();

    MembersAddAdapter(long airdropCoin) {
        super(diffCallback);
        this.airdropCoin = airdropCoin;
    }

    void submitFriendList(@NonNull List<User> list, boolean isSelected) {
        if (isSelected) {
            Map<String, String> selectedMap = new HashMap<>();
            for (User user : list) {
                String key = user.publicKey;
                String value = getInputCoins(this.selectedMap, key, airdropCoin);
                selectedMap.put(key, value);
            }
            this.selectedMap.clear();
            this.selectedMap.putAll(selectedMap);
        }
        submitList(list);
    }

//    public static String getInputCoins(Map<String, String> selectedMap, String publicKey) {
//        return getInputCoins(selectedMap, publicKey, "");
//    }

    @SuppressWarnings("ConstantConditions")
    private static String getInputCoins(Map<String, String> selectedMap, String publicKey, long defaultValue) {
        String value = FmtMicrometer.fmtFormat(String.valueOf(defaultValue));
        if (selectedMap.containsKey(publicKey)) {
            value = selectedMap.get(publicKey);
        }
        return value;
    }

    public void setListener(ClickListener listener) {
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

        return new ViewHolder(binding, listener, selectedMap, airdropCoin);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    List<User> getSelectedList() {
        List<User> list = new ArrayList<>();
        for (User user : getCurrentList()) {
            if (selectedMap.containsKey(user.publicKey)) {
                list.add(user);
            }
        }
        return list;
    }

    Map<String, String> getSelectedMap() {
        return selectedMap;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAddMembersBinding binding;
        private ClickListener listener;
        private Map<String, String> selectedMap;
        private long airdropCoin;

        ViewHolder(ItemAddMembersBinding binding, ClickListener listener,
                   Map<String, String> selectedMap, long airdropCoin) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.selectedMap = selectedMap;
            this.airdropCoin = airdropCoin;
        }

        void bind(ViewHolder holder, User user) {
            if(null == holder || null == user){
                return;
            }
            holder.binding.cbSelect.setVisibility(View.VISIBLE);
            holder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedMap.remove(user.publicKey);
                if (isChecked) {
                    selectedMap.put(user.publicKey, ViewUtils.getText(holder.binding.etAirdropCoins));
                }
                if(listener != null){
                    listener.onSelectClicked();
                }
            });
            holder.binding.cbSelect.setChecked(selectedMap.containsKey(user.publicKey));
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

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (listener != null) {
                        selectedMap.put(user.publicKey, ViewUtils.getText(holder.binding.etAirdropCoins));
                        listener.onTextChanged();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };
            TextWatcher oldTextWatcher = (TextWatcher) holder.binding.etAirdropCoins.getTag();
            if (oldTextWatcher != null) {
                holder.binding.etAirdropCoins.removeTextChangedListener(oldTextWatcher);
            }
            holder.binding.etAirdropCoins.setText(getInputCoins(this.selectedMap, user.publicKey, airdropCoin));
            holder.binding.etAirdropCoins.addTextChangedListener(textWatcher);
            holder.binding.etAirdropCoins.setTag(textWatcher);
        }
    }

    public interface ClickListener {
        void onSelectClicked();
        void onTextChanged();
    }

    private static final DiffUtil.ItemCallback diffCallback = new DiffUtil.ItemCallback<User>() {

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
