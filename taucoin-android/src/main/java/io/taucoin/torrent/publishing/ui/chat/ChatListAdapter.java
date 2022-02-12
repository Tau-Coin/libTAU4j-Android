package io.taucoin.torrent.publishing.ui.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.selecttext.CustomPop;
import io.taucoin.torrent.publishing.core.utils.selecttext.SelectTextEvent;
import io.taucoin.torrent.publishing.core.utils.selecttext.SelectTextEventBus;
import io.taucoin.torrent.publishing.core.utils.selecttext.SelectTextHelper;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextRightBinding;
import io.taucoin.torrent.publishing.ui.customviews.HashTextView;
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
    private Context mContext;

    ChatListAdapter(Context context, ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
        this.mContext = context;
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
        ChatMsgAndLog previousChat = null;
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

    class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private SelectTextHelper mSelectableTextHelper;
        private String selectedText;
        private HashTextView textView;
        private LinearLayout textContainer;
        private ChatMsg msg;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bindTextRight(ItemTextRightBinding binding, ChatMsgAndLog msg, ChatMsgAndLog previousChat, Bitmap myBitmap) {
            if (null == binding || null == msg) {
                return;
            }
            this.textView = binding.tvMsg;
            this.textContainer = binding.middleView;
            this.msg = msg;
            showStatusView(binding.ivStats, binding.tvProgress, binding.ivWarning, msg);
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, previousChat, null, myBitmap);
        }

        void bindText(ItemTextBinding binding, ChatMsg msg, ChatMsg previousChat, Bitmap headPic) {
            if (null == binding || null == msg) {
                return;
            }
            this.textView = binding.tvMsg;
            this.textContainer = binding.middleView;
            this.msg = msg;
            bindText(binding.ivHeadPic, binding.tvTime, binding.tvMsg, previousChat, headPic, null);
        }

        private void bindText(RoundImageView roundButton, TextView tvTime, HashTextView tvMsg,
                              ChatMsg previousChat, Bitmap headPic, Bitmap myBitmap) {
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
//            tvMsg.setText(SpannableUrl.generateSpannableUrl(contentStr));
            tvMsg.setText(contentStr);

            Linkify.addLinks(tvMsg, Linkify.WEB_URLS);
            Pattern airdrop = Pattern.compile(UrlUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(UrlUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(tvMsg, chain, null);

            mSelectableTextHelper = new SelectTextHelper
                    .Builder(tvMsg)// 放你的textView到这里！！
                    .setCursorHandleColor(0xFF1379D6)
                    .setCursorHandleSizeInDp(16)
                    .setSelectedColor(0xFFAFE1F4)
                    .setSelectAll(true)
                    .setScrollShow(true)
                    .setSelectedAllNoPop(true)// 已经全选无弹窗，设置了true在监听会回调 onSelectAllShowCustomPop 方法 default false
                    .setMagnifierShow(false)// 放大镜 default true
                    .setPopSpanCount(5)// 设置操作弹窗每行个数 default 5
                    .setPopStyle(R.drawable.shape_color_4c4c4c_radius_8, R.mipmap.ic_arrow)
                    .addItem(0, R.string.operation_copy,
                            ()-> copyText(mSelectableTextHelper, selectedText))
//                    .addItem(0, R.string.operation_select_all,
//                            this::selectAll)
                    .build();

            mSelectableTextHelper.setSelectListener(new SelectTextHelper.OnSelectListener() {
                /**
                 * 点击回调
                 */
                @Override
                public void onClick(View v) {
                }

                /**
                 * 长按回调
                 */
                @Override
                public void onLongClick(View v) {
                    postShowCustomPop();
                }

                /**
                 * 选中文本回调
                 */
                @Override
                public void onTextSelected(CharSequence content) {
                    selectedText = content.toString();
                }

                /**
                 * 弹窗关闭回调
                 */
                @Override
                public void onDismiss() {
                }

                /**
                 * 点击TextView里的url回调
                 *
                 * 已被下面重写
                 * textView.setMovementMethod(new LinkMovementMethodInterceptor());
                 */
                @Override
                public void onClickUrl(String url) {
                    ActivityUtil.openUri(url);
                }

                /**
                 * 全选显示自定义弹窗回调
                 */
                @Override
                public void onSelectAllShowCustomPop() {
                    postShowCustomPop();
                }

                /**
                 * 重置回调
                 */
                @Override
                public void onReset() {
                    SelectTextEventBus.getDefault().dispatchDismissOperatePop();
                }

                /**
                 * 解除自定义弹窗回调
                 */
                @Override
                public void onDismissCustomPop() {
                    SelectTextEventBus.getDefault().dispatchDismissOperatePop();
                }

                /**
                 * 是否正在滚动回调
                 */
                @Override
                public void onScrolling() {
                    removeShowSelectView();
                }
            });

            // 注册
            if (!SelectTextEventBus.getDefault().isRegistered(this)) {
                SelectTextEventBus.getDefault().register(this);
            }
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

        private boolean isShowTime(ChatMsg chat, ChatMsg previousChat) {
            if (previousChat != null) {
                int interval = DateUtil.getSeconds(previousChat.timestamp, chat.timestamp);
                return interval > 2 * 60;
            }
            return true;
        }

        /**
         * 全选
         */
        private void selectAll() {
            SelectTextEventBus.getDefault().dispatchDismissAllPop();
            if (null != mSelectableTextHelper) {
                mSelectableTextHelper.selectAll();
            }
        }

        /**
         * 延迟显示CustomPop
         * 防抖
         */
        private void postShowCustomPop() {
            textView.removeCallbacks(mShowCustomPopRunnable);
            textView.postDelayed(mShowCustomPopRunnable, 100);
        }

        private final Runnable mShowCustomPopRunnable =
                () -> showCustomPop(textContainer);

        /**
         * 延迟重置
         * 为了支持滑动不重置
         */
        private void postReset() {
            textView.removeCallbacks(mShowSelectViewRunnable);
            textView.postDelayed(mShowSelectViewRunnable, 120);
        }

        private void removeShowSelectView() {
            textView.removeCallbacks(mShowSelectViewRunnable);
        }

        private final Runnable mShowSelectViewRunnable =
                () -> {
                    if (mSelectableTextHelper != null) {
                        mSelectableTextHelper.reset();
                    }
                };

        /**
         * 自定义弹窗
         *
         * @param targetView 目标View
         */
        private void showCustomPop(View targetView) {
            CustomPop msgPop = new CustomPop(mContext, targetView, true);
            msgPop.addItem(0, R.string.operation_copy, () ->
                    copyText(mSelectableTextHelper, selectedText));
            // 设置每个item自适应
//             msgPop.setItemWrapContent();
            // 设置背景 和 箭头
            // msgPop.setPopStyle(R.drawable.shape_color_666666_radius_8, R.drawable.ic_arrow_666);
            msgPop.show();
        }

        /**
         * 自定义SelectTextEvent 隐藏 光标
         */
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void handleSelector(SelectTextEvent event) {
            if (null == mSelectableTextHelper) {
                return;
            }
            String type = event.getType();
            if (TextUtils.isEmpty(type)) {
                return;
            }
            switch (type) {
                case SelectTextEventBus.DISMISS_ALL_POP:
                    mSelectableTextHelper.reset();
                    break;
                case SelectTextEventBus.DISMISS_ALL_POP_DELAYED:
                    postReset();
                    break;
            }
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

    /**
     * 复制
     */
    private void copyText(SelectTextHelper mSelectableTextHelper, String selectedText) {
        SelectTextEventBus.getDefault().dispatchDismissAllPop();
        CopyManager.copyText(selectedText);
        ToastUtils.showShortToast(R.string.copy_successfully);
        if (null != mSelectableTextHelper) {
            mSelectableTextHelper.reset();
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.mSelectableTextHelper.destroy();
        holder.mSelectableTextHelper.setSelectListener(null);
        holder.mSelectableTextHelper = null;
        SelectTextEventBus.getDefault().unregister(holder);
        super.onViewRecycled(holder);
    }

    public interface ClickListener {
        void onMsgLogsClicked(ChatMsg msg);
        void onResendClicked(ChatMsgAndLog msg);
        void onUserClicked(ChatMsg msg);
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
        SelectTextEventBus.getDefault().unregister();
        this.listener = null;
    }
}
