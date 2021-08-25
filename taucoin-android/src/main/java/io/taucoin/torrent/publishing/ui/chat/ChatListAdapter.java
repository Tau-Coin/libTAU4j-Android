package io.taucoin.torrent.publishing.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemPictureBinding;
import io.taucoin.torrent.publishing.databinding.ItemPictureRightBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextRightBinding;
import io.taucoin.torrent.publishing.ui.customviews.HashImageView;
import io.taucoin.torrent.publishing.ui.customviews.HashTextView;
import io.taucoin.torrent.publishing.ui.customviews.RoundButton;
import io.taucoin.torrent.publishing.core.model.data.message.MessageType;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends ListAdapter<ChatMsg, ChatListAdapter.ViewHolder> {

    enum ViewType {
        LEFT_TEXT,
        LEFT_PICTURE,
        RIGHT_TEXT,
        RIGHT_PICTURE
    }
    private ClickListener listener;
    private User friend;
    private byte[] cryptoKey;

    ChatListAdapter(ClickListener listener, String friendPk) {
        super(diffCallback);
        this.listener = listener;
        this.cryptoKey = Utils.keyExchange(friendPk, MainApplication.getInstance().getSeed());
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ViewDataBinding binding;
        if (viewType == ViewType.RIGHT_PICTURE.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_picture_right,
                    parent,
                    false);
        } else if (viewType == ViewType.RIGHT_TEXT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text_right,
                    parent,
                    false);
        }  else if (viewType == ViewType.LEFT_PICTURE.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_picture,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener, friend, cryptoKey);
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
                if (chat.contentType == MessageType.PICTURE.getType()) {
                    return ViewType.RIGHT_PICTURE.ordinal();
                } else {
                    return ViewType.RIGHT_TEXT.ordinal();
                }
            } else {
                if (chat.contentType == MessageType.PICTURE.getType()) {
                    return ViewType.LEFT_PICTURE.ordinal();
                } else {
                    return ViewType.LEFT_TEXT.ordinal();
                }
            }
        }
        return ViewType.LEFT_TEXT.ordinal();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMsg previousChat = null;
        if (position > 0) {
            previousChat = getItem(position - 1);
        }
        if (getItemViewType(position) == ViewType.RIGHT_PICTURE.ordinal()) {
            ItemPictureRightBinding binding = (ItemPictureRightBinding) holder.binding;
            holder.bindPictureRight(binding, getItem(position), previousChat);
        } else if (getItemViewType(position) == ViewType.RIGHT_TEXT.ordinal()) {
            ItemTextRightBinding binding = (ItemTextRightBinding) holder.binding;
            holder.bindTextRight(binding, getItem(position), previousChat);
        } else if (getItemViewType(position) == ViewType.LEFT_PICTURE.ordinal()) {
            ItemPictureBinding binding = (ItemPictureBinding) holder.binding;
            holder.bindPicture(binding, getItem(position), previousChat);
        } else {
            ItemTextBinding binding = (ItemTextBinding) holder.binding;
            holder.bindText(binding, getItem(position), previousChat);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private User friend;
        private byte[] cryptoKey;

        ViewHolder(ViewDataBinding binding, ClickListener listener, User friend, byte[] cryptoKey) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.friend = friend;
            this.cryptoKey = cryptoKey;
        }

        void bindTextRight(ItemTextRightBinding binding, ChatMsg msg, ChatMsg previousChat) {
            if (null == binding || null == msg) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, binding.ivWarning, msg);
            bindText(binding.roundButton, binding.tvTime, binding.tvMsg, msg, previousChat);
        }

        void bindText(ItemTextBinding binding, ChatMsg msg, ChatMsg previousChat) {
            if (null == binding || null == msg) {
                return;
            }
            bindText(binding.roundButton, binding.tvTime, binding.tvMsg, msg, previousChat);
        }

        private void bindText(RoundButton roundButton, TextView tvTime, HashTextView tvMsg,
                              ChatMsg msg, ChatMsg previousChat) {
            if (null == msg) {
                return;
            }
            roundButton.setBgColor(Utils.getGroupColor(msg.senderPk));

            String showName;
            if (StringUtil.isEquals(msg.senderPk, MainApplication.getInstance().getPublicKey())) {
                showName = UsersUtil.getShowName(MainApplication.getInstance().getCurrentUser(), msg.senderPk);
            } else {
                showName = UsersUtil.getShowName(friend, msg.senderPk);
            }
            roundButton.setText(StringUtil.getFirstLettersOfName(showName));
            roundButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClicked(msg);
                }
            });

            boolean isShowTime = isShowTime(msg, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(msg.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            String contentStr = Utils.textBytesToString(msg.content);
            tvMsg.setText(contentStr);
        }

        private void showStatusView(ImageView ivStats, ProgressBar tvProgress, ImageView ivWarning, ChatMsg msg) {
            if (null == ivWarning) {
                return;
            }
            ivStats.setImageResource(R.mipmap.icon_logs);
            ivStats.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.GONE);
            ivWarning.setVisibility(msg.unsent == 1 ? View.GONE : View.VISIBLE);
            ivWarning.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResendClicked(msg);
                }
            });

            ivStats.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMsgLogsClicked(msg);
                }
            });
        }

        private boolean isShowTime(ChatMsg chat, ChatMsg previousChat) {
            if (previousChat != null) {
                int interval = DateUtil.getSeconds(previousChat.timestamp, chat.timestamp);
                return interval > 2 * 60;
            }
            return true;
        }

        void bindPictureRight(ItemPictureRightBinding binding, ChatMsg msg, ChatMsg previousChat) {
            if (null == binding || null == msg) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, null, msg);
            bindPicture(binding.roundButton, binding.tvTime, binding.tvImage, msg, previousChat);
        }

        void bindPicture(ItemPictureBinding binding, ChatMsg msg, ChatMsg previousChat) {
            if(null == binding || null == msg){
                return;
            }
            bindPicture(binding.roundButton, binding.tvTime, binding.tvImage, msg, previousChat);
        }

        private void bindPicture(RoundButton roundButton, TextView tvTime, HashImageView tvImage,
                                 ChatMsg msg, ChatMsg previousChat) {
            if (null == msg) {
                return;
            }
            roundButton.setBgColor(Utils.getGroupColor(msg.senderPk));
            String showName;
            if (StringUtil.isEquals(msg.senderPk, MainApplication.getInstance().getPublicKey())) {
                showName = UsersUtil.getShowName(MainApplication.getInstance().getCurrentUser(), msg.senderPk);
            } else {
                showName = UsersUtil.getShowName(friend, msg.senderPk);
            }
            roundButton.setText(StringUtil.getFirstLettersOfName(showName));
            roundButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClicked(msg);
                }
            });

            boolean isShowTime = isShowTime(msg, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(msg.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            tvImage.setImageHash(msg.hash, msg.senderPk, cryptoKey);
        }
    }

    public interface ClickListener {
        void onMsgLogsClicked(ChatMsg msg);
        void onResendClicked(ChatMsg msg);
        void onUserClicked(ChatMsg msg);
    }

    private static final DiffUtil.ItemCallback<ChatMsg> diffCallback = new DiffUtil.ItemCallback<ChatMsg>() {
        @Override
        public boolean areContentsTheSame(@NonNull ChatMsg oldItem, @NonNull ChatMsg newItem) {
            return oldItem.equals(newItem)
                    && StringUtil.isEquals(oldItem.logicMsgHash, newItem.logicMsgHash)
                    && oldItem.unsent == newItem.unsent;
        }

        @Override
        public boolean areItemsTheSame(@NonNull ChatMsg oldItem, @NonNull ChatMsg newItem) {
            return oldItem.equals(newItem);
        }
    };
}
