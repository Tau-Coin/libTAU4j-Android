package io.taucoin.torrent.publishing.ui.friends;

import android.content.Context;
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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemFriendListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class FriendsListAdapter extends ListAdapter<UserAndFriend, FriendsListAdapter.ViewHolder> {
    private ClickListener listener;
    private Map<String, UserAndFriend> selectedList = new HashMap<>();
    private int page;
    private int order;
    private String friendPk;

    FriendsListAdapter(ClickListener listener, int type, int order, String friendPk) {
        super(diffCallback);
        this.listener = listener;
        this.page = type;
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

        return new ViewHolder(binding, listener, page, selectedList, friendPk);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), order);
    }

    ArrayList<UserAndFriend> getSelectedList() {
        return new ArrayList<>(selectedList.values());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFriendListBinding binding;
        private ClickListener listener;
        private Context context;
        private int type;
        private Map<String, UserAndFriend> selectedMap;
        private String friendPk;

        ViewHolder(ItemFriendListBinding binding, ClickListener listener, int type,
                   Map<String, UserAndFriend> selectedMap, String friendPk) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.type = type;
            this.selectedMap = selectedMap;
            this.friendPk = friendPk;
        }

        void bind(ViewHolder holder, UserAndFriend user, int order) {
            if(null == holder || null == user){
                return;
            }
            boolean isShowSelect = type == FriendsActivity.PAGE_ADD_MEMBERS ||
                    type == FriendsActivity.PAGE_CREATION_ADD_MEMBERS;
            holder.binding.cbSelect.setVisibility(isShowSelect ? View.VISIBLE : View.GONE);
            holder.binding.ivShare.setVisibility(View.GONE);
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
            if (type == FriendsActivity.PAGE_FRIENDS_LIST) {
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
            String firstLetters = StringUtil.getFirstLettersOfName(showName);
            holder.binding.leftView.setText(firstLetters);

            String time = "";
            if (order == 0 && user.lastSeenTime > 0) {
                time = DateUtil.format(user.lastSeenTime, DateUtil.pattern6);
                time = context.getResources().getString(R.string.contacts_last_seen, time);
            } else if (order != 0 && user.lastCommTime > 0) {
                time = DateUtil.format(user.lastCommTime, DateUtil.pattern6);
                time = context.getResources().getString(R.string.contacts_last_communication, time);
            }
            holder.binding.tvTime.setVisibility(StringUtil.isEmpty(time) ? View.GONE : View.VISIBLE);
            holder.binding.tvTime.setText(time);

            StringBuilder communities = new StringBuilder();
            if(user.members != null && user.members.size() > 0){
                List<Member> members = user.members;
                int size = members.size();
                for (int i = 0; i < size; i++) {
                    Member member = members.get(i);
                    if(member.balance <=0 && member.power <= 0){
                        continue;
                    }
                    if(communities.length() == 0){
                        communities.append(context.getResources().getString(R.string.contacts_community_from));
                    }
                    String communityName = ChainIDUtil.getName(member.chainID);
                    String community = context.getResources().getString(R.string.contacts_community_more, communityName);
                    if(i == 0){
                        community = community.substring(1);
                    }
                    communities.append(community);
                }
            }
            holder.binding.tvCommunities.setVisibility(communities.length() == 0
                    ? View.GONE : View.VISIBLE);
            holder.binding.tvCommunities.setText(communities.toString());

            int bgColor = Utils.getGroupColor(user.publicKey);
            holder.binding.leftView.setBgColor(bgColor);

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(user);
                }
            });
            holder.binding.ivShare.setOnClickListener(v -> {
                if(listener != null){
                    listener.onShareClicked(user);
                }
            });
            // 新朋友高亮显示
            boolean isNewScanFriend = StringUtil.isEquals(friendPk, user.publicKey);
            bgColor = isNewScanFriend ? R.color.color_bg : R.color.color_white;
            holder.binding.getRoot().setBackgroundColor(context.getResources().getColor(bgColor));
        }
    }

    public interface ClickListener {
        void onItemClicked(UserAndFriend item);
        void onShareClicked(UserAndFriend item);
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
