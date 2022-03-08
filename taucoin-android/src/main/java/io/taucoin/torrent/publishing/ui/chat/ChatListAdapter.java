package io.taucoin.torrent.publishing.ui.chat;

import android.graphics.Bitmap;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextRightBinding;
import io.taucoin.torrent.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.torrent.publishing.ui.customviews.RoundImageView;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends ListAdapter<ChatMsgAndLog, ChatListAdapter.ViewHolder> {

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
            showStatusView(binding.ivStats, binding.tvProgress, binding.ivWarning, msg);
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, isShowTime, null, myBitmap);
        }

        void bindText(ItemTextBinding binding, ChatMsg msg, boolean isShowTime, Bitmap headPic) {
            if (null == binding || null == msg) {
                return;
            }
            this.msg = msg;
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, isShowTime, headPic, null);
        }

        private void bindText(RoundImageView roundButton, TextView tvTime, AutoLinkTextView tvMsg,
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
            String contentStr = Utils.textBytesToString(msg.content);
            tvMsg.setText(contentStr);

            Linkify.addLinks(tvMsg, Linkify.WEB_URLS);
            Pattern airdrop = Pattern.compile(UrlUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(UrlUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(tvMsg, chain, null);

            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {

                @Override
                public void onClick(AutoLinkTextView view) {

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

        private void showStatusView(ImageView ivStats, ProgressBar tvProgress, ImageView ivWarning, ChatMsgAndLog msg) {
            if (null == ivWarning) {
                return;
            }
            ivStats.setImageResource(R.mipmap.icon_logs);
            ivStats.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.GONE);
            boolean isNeedResend = msg.unsent != 1 || isNeedResend(msg);
            ivWarning.setVisibility(isNeedResend ? View.VISIBLE : View.GONE);
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
    }

    private static boolean isNeedResend(ChatMsgAndLog msg) {
        List<ChatMsgLog> logs = msg.logs;
        if (null == logs || logs.size() <= 0) {
            return false;
        }
        long maxTimestamp = 0;
        for (ChatMsgLog log : logs) {
            if (log.status == ChatMsgStatus.SYNC_CONFIRMED.getStatus()) {
                return false;
            } else {
                maxTimestamp = log.timestamp;
            }
        }
        long currentTime = DateUtil.getMillisTime();
        return maxTimestamp > 0 && DateUtil.timeDiffHours(maxTimestamp, currentTime) >=
                Constants.MSG_RESEND_PERIOD;
    }

    public interface ClickListener {
        void onMsgLogsClicked(ChatMsg msg);
        void onResendClicked(ChatMsgAndLog msg);
        void onUserClicked(ChatMsg msg);
        void onLongClick(AutoLinkTextView view);
        void onLinkClick(String link);
    }

    private static final DiffUtil.ItemCallback<ChatMsgAndLog> diffCallback = new DiffUtil.ItemCallback<ChatMsgAndLog>() {
        @Override
        public boolean areContentsTheSame(@NonNull ChatMsgAndLog oldItem, @NonNull ChatMsgAndLog newItem) {
            return oldItem.equals(newItem)
                    && StringUtil.isEquals(oldItem.logicMsgHash, newItem.logicMsgHash)
                    && oldItem.unsent == newItem.unsent
                    && isNeedResend(oldItem) == isNeedResend(newItem);
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
