package io.taucoin.tauapp.publishing.ui.chat;

import android.graphics.Bitmap;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.tauapp.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.core.utils.BitmapUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.databinding.ItemTextBinding;
import io.taucoin.tauapp.publishing.databinding.ItemTextRightBinding;
import io.taucoin.tauapp.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.tauapp.publishing.ui.customviews.RoundImageView;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends ListAdapter<ChatMsgAndLog, ChatListAdapter.ViewHolder> {

    enum ViewType {
        LEFT_TEXT,
        RIGHT_TEXT
    }
    private ClickListener listener;
    private String showName;
    private byte[] headPic;
    private byte[] myHeadPic;
    private Bitmap bitmap;
    private Bitmap myBitmap;

    ChatListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    public void setFriend(User friend) {
        if (friend != null) {
            if (null == bitmap || bitmap.isRecycled() || !Arrays.equals(headPic, friend.headPic) ||
                StringUtil.isNotEquals(showName, UsersUtil.getShowName(friend))) {
                headPic = friend.headPic;
                this.bitmap = UsersUtil.getHeadPic(friend);
                notifyDataSetChanged();
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ViewDataBinding binding;
        if (viewType == ViewType.RIGHT_TEXT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text_right,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener);
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMsg chat = getItem(position);
        String userPk = MainApplication.getInstance().getPublicKey();
        if (chat != null) {
            if (StringUtil.isEquals(userPk, chat.senderPk) ) {
                return ViewType.RIGHT_TEXT.ordinal();
            } else {
                return ViewType.LEFT_TEXT.ordinal();
            }
        }
        return ViewType.LEFT_TEXT.ordinal();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        long previousTime = 0;
        if (position > 0) {
            previousTime = getItem(position - 1).timestamp;
        }
        ChatMsgAndLog currentChat = getItem(position);
        boolean isShowTime = DateUtil.isShowTime(currentChat.timestamp, previousTime);
        if (getItemViewType(position) == ViewType.RIGHT_TEXT.ordinal()) {
            User myself = MainApplication.getInstance().getCurrentUser();
            if (myself != null) {
                if (null == myBitmap || myBitmap.isRecycled() ||
                        Arrays.equals(myself.headPic, myHeadPic)) {
                    myBitmap = UsersUtil.getHeadPic(myself);
                    myHeadPic = myself.headPic;
                }
            }
            ItemTextRightBinding binding = (ItemTextRightBinding) holder.binding;
            holder.bindTextRight(binding, currentChat, isShowTime, myBitmap);
        } else {
            ItemTextBinding binding = (ItemTextBinding) holder.binding;
            holder.bindText(binding, currentChat, isShowTime, bitmap);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private ChatMsg msg;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bindTextRight(ItemTextRightBinding binding, ChatMsgAndLog msg, boolean isShowTime, Bitmap myBitmap) {
            if (null == binding || null == msg) {
                return;
            }
            this.msg = msg;
            showStatusView(binding.ivWarning, msg);
            showUserView(binding.ivHeadPic, binding.tvTime, isShowTime, null, myBitmap);
            bindText(binding.tvMsg);
        }

        void bindText(ItemTextBinding binding, ChatMsg msg, boolean isShowTime, Bitmap headPic) {
            if (null == binding || null == msg) {
                return;
            }
            this.msg = msg;
            showUserView(binding.ivHeadPic, binding.tvTime, isShowTime, headPic, null);
            bindText(binding.tvMsg);
        }

        private void showUserView(RoundImageView roundButton, TextView tvTime,
                              boolean isShowTime, Bitmap headPic, Bitmap myBitmap) {
            if (null == msg) {
                return;
            }
            if (headPic != null) {
                roundButton.setImageBitmap(headPic);
            } else {
                roundButton.setImageBitmap(myBitmap);
            }
            roundButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClicked(msg);
                }
            });

            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(msg.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
        }

        private void bindText(AutoLinkTextView tvMsg) {
            if (null == msg) {
                return;
            }
            String contentStr = Utils.textBytesToString(msg.content);
            tvMsg.setText(contentStr);

            Linkify.addLinks(tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(tvMsg, friend, null);

            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {

                @Override
                public void onClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onItemClicked(msg);
                    }
                }

                @Override
                public void onLongClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onLongClick(view);
                    }
                }

                @Override
                public void onLinkClick(String link) {
                    if (listener != null) {
                        listener.onLinkClick(link);
                    }
                }
            });
        }

        private void showStatusView(ImageView ivWarning, ChatMsgAndLog msg) {
            if (null == ivWarning) {
                return;
            }
            ivWarning.setImageResource(parseWarningReid(msg));

            ivWarning.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMsgLogsClicked(msg);
                }
            });
        }
    }

    private static int parseWarningReid(ChatMsgAndLog msg) {
        List<ChatMsgLog> logs = msg.logs;
        if (null == logs || logs.size() <= 0) {
            return R.mipmap.icon_msg_waitting;
        }
        ChatMsgLog log = msg.logs.get(0);
        if (log.status == ChatMsgStatus.CONFIRMED.getStatus()) {
            return R.mipmap.icon_msg_displayed;
        } else if (log.status == ChatMsgStatus.ARRIVED_SWARM.getStatus()) {
            return R.mipmap.icon_msg_swarm;
        } else {
            return R.mipmap.icon_msg_waitting;
        }
    }

    public interface ClickListener {
        void onItemClicked(ChatMsg msg);
        void onMsgLogsClicked(ChatMsgAndLog msg);
        void onUserClicked(ChatMsg msg);
        void onLongClick(AutoLinkTextView view);
        void onLinkClick(String link);
    }

    private static final DiffUtil.ItemCallback<ChatMsgAndLog> diffCallback = new DiffUtil.ItemCallback<ChatMsgAndLog>() {
        @Override
        public boolean areContentsTheSame(@NonNull ChatMsgAndLog oldItem, @NonNull ChatMsgAndLog newItem) {
            return oldItem.equals(newItem)
                    && StringUtil.isEquals(oldItem.logicMsgHash, newItem.logicMsgHash)
                    && parseWarningReid(oldItem) == parseWarningReid(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull ChatMsgAndLog oldItem, @NonNull ChatMsgAndLog newItem) {
            return oldItem.equals(newItem);
        }
    };

    public void recycle() {
        BitmapUtil.recycleBitmap(bitmap);
        BitmapUtil.recycleBitmap(myBitmap);
        this.listener = null;
    }
}
