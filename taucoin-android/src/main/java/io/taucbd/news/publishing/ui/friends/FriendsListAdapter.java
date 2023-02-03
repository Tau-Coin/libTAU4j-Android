package io.taucbd.news.publishing.ui.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.UserAndFriend;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.GeoUtils;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemFriendListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class FriendsListAdapter extends ListAdapter<UserAndFriend, FriendsListAdapter.ViewHolder> {
    private final ClickListener listener;
    private final Map<String, UserAndFriend> selectedList = new HashMap<>();
    private final int pageType;
    private final String friendPk;
    private int order;

    FriendsListAdapter(ClickListener listener, int pageType, int order, String friendPk) {
        super(diffCallback);
        this.listener = listener;
        this.pageType = pageType;
        this.order = order;
        this.friendPk = friendPk;
    }

    void setOrder(int order) {
        this.order = order;
        diffCallback.updateOrder(order);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFriendListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_friend_list,
                parent,
                false);

        return new ViewHolder(binding, listener, pageType, selectedList, friendPk);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), order);
    }

    ArrayList<UserAndFriend> getSelectedList() {
        return new ArrayList<>(selectedList.values());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendListBinding binding;
        private final ClickListener listener;
        private final Context context;
        private final int pageType;
        private final Map<String, UserAndFriend> selectedMap;
        private final String friendPk;

        ViewHolder(ItemFriendListBinding binding, ClickListener listener, int pageType,
                   Map<String, UserAndFriend> selectedMap, String friendPk) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.pageType = pageType;
            this.selectedMap = selectedMap;
            this.friendPk = friendPk;
        }

        void bind(ViewHolder holder, UserAndFriend user, int order) {
            if(null == holder || null == user){
                return;
            }
            boolean isShowSelect = pageType == FriendsActivity.PAGE_ADD_MEMBERS;
            holder.binding.cbSelect.setVisibility(isShowSelect ? View.VISIBLE : View.GONE);
            if (isShowSelect) {
                holder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked){
                        selectedMap.put(user.publicKey, user);
                    } else {
                        selectedMap.remove(user.publicKey);
                    }
                });
                holder.binding.cbSelect.setChecked(selectedMap.containsKey(user.publicKey));
            }
            SpanUtils showNameBuilder = new SpanUtils();
            String showName;
            if (pageType == FriendsActivity.PAGE_FRIENDS_LIST) {
                showName = UsersUtil.getShowNameWithYourself(user, user.publicKey);
                showNameBuilder.append(showName);
                if (user.isDiscovered()) {
                    showNameBuilder.append(" ")
                            .append(context.getString(R.string.contacts_discovered))
                            .setForegroundColor(context.getResources().getColor(R.color.color_blue))
                            .setFontSize(12, true);
                } else {
                    showNameBuilder.append(" ")
                            .append(String.valueOf(user.onlineCount))
                            .setForegroundColor(context.getResources().getColor(R.color.color_blue))
                            .setFontSize(12, true)
                            .setSuperscript();
                }
            } else {
                showName = UsersUtil.getShowName(user, user.publicKey);
                showNameBuilder.append(showName);
                showNameBuilder.append(context.getString(R.string.common_parentheses,
                        UsersUtil.getHideLastPublicKey(user.publicKey)))
                        .setForegroundColor(context.getResources().getColor(R.color.gray_dark))
                        .setFontSize(14, true);
            }
            holder.binding.tvName.setText(showNameBuilder.create());
            holder.binding.leftView.setImageBitmap(UsersUtil.getHeadPic(user));

            String time = "";
            if (order == 0 && user.lastSeenTime > 0) {
                time = DateUtil.formatTime(user.lastSeenTime, DateUtil.pattern6);
//                time = context.getResources().getString(R.string.contacts_last_seen, time);
            } else if (order != 0 && user.lastCommTime > 0) {
                time = DateUtil.format(user.lastCommTime, DateUtil.pattern6);
//                time = context.getResources().getString(R.string.contacts_last_communication, time);
            }
            holder.binding.tvTime.setVisibility(StringUtil.isEmpty(time) ? View.GONE : View.VISIBLE);
            holder.binding.tvTime.setText(time);

            User currentUser = MainApplication.getInstance().getCurrentUser();
            String distance = null;
            String userPk = null;
            if (currentUser != null) {
                userPk = currentUser.publicKey;
                if (user.longitude != 0 && user.latitude != 0 &&
                        currentUser.longitude != 0 && currentUser.latitude != 0) {
                    distance = GeoUtils.getDistanceStr(user.longitude, user.latitude,
                            currentUser.longitude, currentUser.latitude);
                }
            }
            if (StringUtil.isNotEmpty(distance)) {
                holder.binding.tvDistance.setText(distance);
                holder.binding.tvDistance.setVisibility(View.VISIBLE);
            } else {
                holder.binding.tvDistance.setVisibility(View.GONE);
            }
            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(user);
                }
            });
            // 新朋友高亮显示
            boolean isNewScanFriend = StringUtil.isEquals(friendPk, user.publicKey);
            int bgColor = isNewScanFriend ? R.color.color_bg : R.color.color_white;
            View rootView = holder.binding.getRoot();
            rootView.setBackgroundColor(context.getResources().getColor(bgColor));
            boolean isShowMyself = pageType != FriendsActivity.PAGE_FRIENDS_LIST || StringUtil.isNotEquals(userPk, user.publicKey);
            ViewGroup.LayoutParams layoutParams = rootView.getLayoutParams();
            layoutParams.height = isShowMyself ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            rootView.setLayoutParams(layoutParams);
        }
    }

    public interface ClickListener {
        void onItemClicked(User item);
    }

    static abstract class ItemCallback extends DiffUtil.ItemCallback<UserAndFriend> {
        int oldOrder;
        int order;

        void updateOrder(int order) {
            this.oldOrder = this.order;
            this.order = order;

        }
    }

    private static final ItemCallback diffCallback = new ItemCallback() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem) && oldOrder == order &&
                    oldItem.status == newItem.status &&
                    StringUtil.isEquals(oldItem.remark, newItem.remark) &&
                    oldItem.onlineCount == newItem.onlineCount &&
                    ((order == 0 && oldItem.lastSeenTime == newItem.lastSeenTime) ||
                            (order == 1 && oldItem.lastCommTime == newItem.lastCommTime));
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
