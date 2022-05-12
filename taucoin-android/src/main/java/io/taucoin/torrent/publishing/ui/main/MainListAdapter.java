package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemChatListBinding;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<CommunityAndFriend, MainListAdapter.ViewHolder> {
    private ClickListener listener;

    MainListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if (viewType == 0) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_group_list,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_chat_list,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        return getCurrentList().get(position).type;
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, CommunityAndFriend bean) {
            if(null == holder || null == bean){
                return;
            }
            if (holder.binding instanceof ItemGroupListBinding) {
                ItemGroupListBinding binding = (ItemGroupListBinding) holder.binding;
                if (bean.timestamp > 0) {
                    String time = DateUtil.getWeekTime(bean.timestamp);
                    binding.tvMsgLastTime.setText(time);
                    binding.tvMsgLastTime.setVisibility(View.VISIBLE);
                } else {
                    binding.tvMsgLastTime.setText(null);
                    binding.tvMsgLastTime.setVisibility(View.GONE);
                }
                String communityName = ChainIDUtil.getName(bean.ID);
                String communityCode = ChainIDUtil.getCode(bean.ID);
                binding.tvGroupName.setText(context.getString(R.string.main_community_name,
                        communityName, communityCode));
                String firstLetters = StringUtil.getFirstLettersOfName(communityName);
                binding.leftView.setText(firstLetters);
                String balance = FmtMicrometer.fmtBalance(bean.balance);
                boolean readOnly = bean.isReadOnly();
                if (readOnly) {
                    binding.tvBalancePower.setText(context.getString(R.string.main_community_non_member));
                } else {
                    String power = FmtMicrometer.fmtLong(bean.power);
                    binding.tvBalancePower.setText(context.getString(R.string.main_balance_power, balance, power));
                }

                binding.tvUserMessage.setVisibility(StringUtil.isNotEmpty(bean.memo) ?
                        View.VISIBLE : View.GONE);
                binding.tvUserMessage.setText(bean.memo);

                int bgColor = Utils.getGroupColor(bean.ID);
                binding.leftView.setBgColor(bgColor);

                binding.msgUnread.setVisibility(bean.msgUnread > 0 ? View.VISIBLE : View.GONE);
            } else if (holder.binding instanceof ItemChatListBinding) {
                ItemChatListBinding binding = (ItemChatListBinding) holder.binding;
                String friendNickName = UsersUtil.getShowNameWithYourself(bean.friend, bean.ID);
                binding.tvGroupName.setText(friendNickName);
                binding.leftView.setImageBitmap(UsersUtil.getHeadPic(bean.friend));

                byte[] msg = bean.msg;
                if (msg != null) {
                    String messageStr = Utils.textBytesToString(bean.msg);
                    binding.tvUserMessage.setText(messageStr);
                } else {
                    binding.tvUserMessage.setText(context.getString(R.string.main_no_messages));
                }
                if (bean.timestamp > 0) {
                    String time = DateUtil.getWeekTime(bean.timestamp);
                    binding.tvMsgLastTime.setText(time);
                    binding.tvMsgLastTime.setVisibility(View.VISIBLE);
                } else {
                    binding.tvMsgLastTime.setText(null);
                    binding.tvMsgLastTime.setVisibility(View.GONE);
                }
                binding.msgUnread.setVisibility(bean.msgUnread > 0 ? View.VISIBLE : View.GONE);
            }
            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(bean);
                }
            });
            holder.binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClicked(bean);
                }
                return true;
            });

            if (bean.stickyTop == 1) {
                holder.binding.getRoot().setBackgroundColor(context.getResources().getColor(R.color.divider_light));
            } else {
                holder.binding.getRoot().setBackgroundResource(R.drawable.main_white_rect_round_bg);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        if (holder.binding instanceof ItemChatListBinding) {
            BitmapUtil.recycleImageView(((ItemChatListBinding) holder.binding).leftView);
        }
        super.onViewRecycled(holder);
    }

    public interface ClickListener {
        void onItemClicked(CommunityAndFriend item);
        void onItemLongClicked(CommunityAndFriend item);
    }

    private static final DiffUtil.ItemCallback<CommunityAndFriend> diffCallback = new DiffUtil.ItemCallback<CommunityAndFriend>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndFriend oldItem, @NonNull CommunityAndFriend newItem) {
            boolean isSame = oldItem.equals(newItem);
            if (isSame) {
                if (oldItem.type == 0) {
                    isSame = oldItem.timestamp == newItem.timestamp &&
                            oldItem.msgUnread == newItem.msgUnread &&
                            oldItem.stickyTop == newItem.stickyTop &&
                            oldItem.balance == newItem.balance &&
                            oldItem.power == newItem.power &&
                            StringUtil.isEquals(oldItem.memo, newItem.memo);
                } else {
                    isSame = oldItem.timestamp == newItem.timestamp &&
                            oldItem.msgUnread == newItem.msgUnread &&
                            oldItem.stickyTop == newItem.stickyTop &&
                            Arrays.equals(oldItem.msg, newItem.msg) &&
                            StringUtil.isEquals(oldItem.friend != null ? oldItem.friend.remark : null,
                                    newItem.friend != null ? newItem.friend.remark : null) &&
                            StringUtil.isEquals(oldItem.friend != null ? oldItem.friend.nickname : null,
                                    newItem.friend != null ? newItem.friend.nickname : null) &&
                            Arrays.equals(oldItem.friend != null ? oldItem.friend.headPic : null,
                            newItem.friend != null ? newItem.friend.headPic : null);
                }
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndFriend oldItem, @NonNull CommunityAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
