package io.taucoin.torrent.publishing.ui.chat;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;

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
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextRightBinding;
import io.taucoin.torrent.publishing.ui.customviews.HashTextView;
import io.taucoin.torrent.publishing.ui.customviews.RoundImageView;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends ListAdapter<ChatMsg, ChatListAdapter.ViewHolder> {

    enum ViewType {
        LEFT_TEXT,
        RIGHT_TEXT
    }
    private ClickListener listener;
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
            if (null == bitmap || bitmap.isRecycled() || !Arrays.equals(headPic, friend.headPic)) {
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
        ChatMsg previousChat = null;
        if (position > 0) {
            previousChat = getItem(position - 1);
        }
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
            holder.bindTextRight(binding, getItem(position), previousChat, myBitmap);
        } else {
            ItemTextBinding binding = (ItemTextBinding) holder.binding;
            holder.bindText(binding, getItem(position), previousChat, bitmap);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bindTextRight(ItemTextRightBinding binding, ChatMsg msg, ChatMsg previousChat, Bitmap myBitmap) {
            if (null == binding || null == msg) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, binding.ivWarning, msg);
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, msg, previousChat, null, myBitmap);
        }

        void bindText(ItemTextBinding binding, ChatMsg msg, ChatMsg previousChat, Bitmap headPic) {
            if (null == binding || null == msg) {
                return;
            }
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, msg, previousChat, headPic, null);
        }

        private void bindText(RoundImageView roundButton, TextView tvTime, HashTextView tvMsg,
                              ChatMsg msg, ChatMsg previousChat, Bitmap headPic, Bitmap myBitmap) {
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

    public void recycle() {
        BitmapUtil.recycleBitmap(bitmap);
        BitmapUtil.recycleBitmap(myBitmap);
    }
}
