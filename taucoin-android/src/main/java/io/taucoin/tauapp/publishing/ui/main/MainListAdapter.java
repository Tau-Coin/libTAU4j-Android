package io.taucoin.tauapp.publishing.ui.main;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.BuildConfig;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.tauapp.publishing.core.utils.BitmapUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.Logarithm;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.databinding.ItemChatListBinding;
import io.taucoin.tauapp.publishing.databinding.ItemGroupListBinding;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<CommunityAndFriend, MainListAdapter.ViewHolder> {
    private final ClickListener listener;

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
        private TauDaemon daemon;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.daemon = TauDaemon.getInstance(this.context.getApplicationContext());
        }

        void bind(ViewHolder holder, CommunityAndFriend bean) {
            if(null == holder || null == bean){
                return;
            }
            ImageView ivLongPress = null;
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
                int nameReId = bean.joined == 1 ? R.string.main_community_name : R.string.main_community_name_discovered;
                binding.tvGroupName.setText(context.getString(nameReId, communityName, communityCode));
                String firstLetters = StringUtil.getFirstLettersOfName(communityName);
                binding.leftView.setText(firstLetters);
                boolean isNotExpired = daemon.getMyAccountManager().isNotExpired(bean.ID);
                boolean onChain = bean.onChain() && isNotExpired;
//                if (!onChain) {
//                    binding.tvBalancePower.setText(context.getString(R.string.main_community_in_mining));
//                } else {
                    double showPower = Logarithm.log2(2 + bean.power);
                    String power = FmtMicrometer.formatThreeDecimal(showPower);
                    String balance = FmtMicrometer.fmtBalance(bean.getDisplayBalance());
//                    String time = DateUtil.formatTime(bean.balUpdateTime, DateUtil.pattern14);
                    String balanceAndTime = context.getResources().getString(R.string.drawer_balance_time_color,
                            balance, power);
                    binding.tvBalancePower.setText(Html.fromHtml(balanceAndTime));
//                }

                binding.tvUserMessage.setVisibility(StringUtil.isNotEmpty(bean.memo) ?
                        View.VISIBLE : View.GONE);
                binding.tvUserMessage.setText(bean.memo);

                int bgColor = Utils.getGroupColor(bean.ID);
                binding.leftView.setBgColor(bgColor);

                binding.msgUnread.setVisibility(bean.msgUnread > 0 ? View.VISIBLE : View.GONE);
                ivLongPress = binding.ivLongPress;

                boolean isSanFrancisco = StringUtil.isEquals(bean.ID, BuildConfig.TEST_CHAIN_ID);
                binding.leftView.setVisibility(isSanFrancisco ? View.GONE : View.VISIBLE);
                binding.ivGroup.setVisibility(isSanFrancisco ? View.VISIBLE : View.GONE);
                if (isSanFrancisco) {
                    binding.ivGroup.setImageRes(R.mipmap.icon_cbd_logo);
                }
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
                boolean isShowPoint = bean.msgUnread > 0 || bean.focused > 0;
                binding.msgUnread.setVisibility(isShowPoint ? View.VISIBLE : View.GONE);
                if (isShowPoint) {
                    binding.msgUnread.setBackgroundResource(bean.msgUnread > 0 ?
                            R.drawable.circle_red : R.drawable.circle_yellow);
                }
                ivLongPress = binding.ivLongPress;

                binding.leftView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onFriendClicked(bean);
                    }
                });
                binding.tvGroupName.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onFriendClicked(bean);
                    }
                });
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
            if (ivLongPress != null) {
                ivLongPress.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemLongClicked(bean);
                    }
                });
            }
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
        void onFriendClicked(CommunityAndFriend item);
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
                            oldItem.balUpdateTime == newItem.balUpdateTime &&
                            oldItem.nonce == newItem.nonce &&
                            oldItem.joined == newItem.joined &&
                            StringUtil.isEquals(oldItem.memo, newItem.memo);
                } else {
                    isSame = oldItem.timestamp == newItem.timestamp &&
                            oldItem.msgUnread == newItem.msgUnread &&
                            oldItem.stickyTop == newItem.stickyTop &&
                            oldItem.focused == newItem.focused &&
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
