package io.taucoin.tauapp.publishing.ui.transaction;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.utils.BitmapUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.DrawablesUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.Logarithm;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ItemNewsBinding;
import io.taucoin.tauapp.publishing.ui.customviews.AutoLinkTextView;

/**
 * Market 列表显示的Adapter
 */
public class NewsListAdapter extends ListAdapter<UserAndTx, NewsListAdapter.ViewHolder> {

    private final BaseClickListener listener;
    private final boolean isReply;

    NewsListAdapter(BaseClickListener listener) {
        super(diffCallback);
        this.listener = listener;
        this.isReply = false;
    }

    NewsListAdapter(BaseClickListener listener, boolean isReply) {
        super(diffCallback);
        this.listener = listener;
        this.isReply = isReply;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNewsBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_news, parent, false);
        return new ViewHolder(binding, listener, isReply);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;
        private final BaseClickListener listener;
        private final int nameColor;
        private final int replyColor;
        private final int linkDrawableSize;
        private final boolean isReply;

        ViewHolder(ItemNewsBinding binding, BaseClickListener listener, boolean isReply) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.nameColor = binding.getRoot().getResources().getColor(R.color.color_black);
            this.replyColor = binding.getRoot().getResources().getColor(R.color.color_blue_link);
            this.isReply = isReply;
            this.linkDrawableSize = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.widget_size_14);
//            this.drawable = DrawablesUtil.getDrawable(binding.getRoot().getContext(),
//                    R.mipmap.icon_share_link, linkDrawableSize, linkDrawableSize);
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }
            binding.rlBottom.setVisibility(isReply ? View.GONE : View.VISIBLE);
            binding.ivArrow.setVisibility(isReply ? View.GONE : View.VISIBLE);
            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));

            boolean isShowRepliedKey = StringUtil.isNotEmpty(tx.repliedKey);
            binding.tvReply.setVisibility(isReply && isShowRepliedKey ? View.VISIBLE : View.GONE);
            if (isReply && isShowRepliedKey) {
                SpannableStringBuilder reply = new SpanUtils()
                        .append("reply")
                        .append("@" + UsersUtil.getLastPublicKey(tx.repliedKey, 4))
                        .setForegroundColor(replyColor)
                        .create();
                binding.tvReply.setText(reply);
            }

            String userName = UsersUtil.getShowName(tx.sender);
            userName = null == userName ? "" : userName;
            String communityName = ChainIDUtil.getName(tx.chainID);
            String communityCode = ChainIDUtil.getCode(tx.chainID);
            SpanUtils name = new SpanUtils()
                    .append(userName)
                    .setForegroundColor(nameColor)
                    .append("(")
                    .append(UsersUtil.getLastPublicKey(tx.senderPk, 4))
                    .append(")")
                    .append("@")
                    .append(communityName)
                    .append("(").append(communityCode).append(")")
                    .append(" · ").append(DateUtil.getNewsTime(tx.timestamp));
            binding.tvName.setText(name.create());
            setClickListener(binding, tx);

            boolean isMyself = StringUtil.isEquals(tx.senderPk, MainApplication.getInstance().getPublicKey());
            binding.ivBan.setVisibility(isMyself ? View.INVISIBLE : View.VISIBLE);

            double showPower = Logarithm.log2(2 + tx.power);
            String power = FmtMicrometer.formatThreeDecimal(showPower);
            String balance = FmtMicrometer.fmtBalance(tx.getDisplayBalance());
            binding.tvBalance.setText(balance);
            binding.tvPower.setText(power);

            SpannableStringBuilder msg = TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NEWS)
                    .append(" ");
            binding.tvMsg.setText(msg);

            boolean isShowLink = StringUtil.isNotEmpty(tx.link);
            binding.tvLink.setText(tx.link);
            binding.tvLink.setVisibility(isShowLink ? View.VISIBLE : View.GONE);

            binding.tvRepliesNum.setText(FmtMicrometer.fmtLong(tx.repliesNum));
            binding.tvChatNum.setText(FmtMicrometer.fmtLong(tx.chatsNum));

            // 添加link解析
            binding.tvMsg.setAutoLinkMask(0);
            Linkify.addLinks(binding.tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, friend, null);
            binding.tvMsg.requestLayout();

            if (isShowLink) {
                DrawablesUtil.setUnderLine(binding.tvLink);
                DrawablesUtil.setEndDrawable(binding.tvLink, R.mipmap.icon_share_link, linkDrawableSize);
                binding.tvLink.requestLayout();
            }

            setAutoLinkListener(binding.tvMsg, tx);
        }

        public void setAutoLinkListener(AutoLinkTextView autoLinkTextView, UserAndTx tx) {
            AutoLinkTextView.AutoLinkListener autoLinkListener = new AutoLinkTextView.AutoLinkListener() {

                @Override
                public void onClick(AutoLinkTextView view) {
                    if (listener != null) {
                        ((ClickListener) listener).onItemClicked(tx);
                    }
                }

                @Override
                public void onLongClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onItemLongClicked(view, tx);
                    }
                }

                @Override
                public void onLinkClick(String link) {
                    if (listener != null) {
                        listener.onLinkClick(link);
                    }
                }
            };
            autoLinkTextView.setAutoLinkListener(autoLinkListener);
        }

        private void setClickListener(ItemNewsBinding binding, UserAndTx tx) {
            binding.ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });

            binding.ivRetweet.setOnClickListener(view -> {
                if (listener != null && listener instanceof ClickListener) {
                    ((ClickListener) listener).onRetweetClicked(tx);
                }
            });
            binding.ivReply.setOnClickListener(view -> {
                if (listener != null && listener instanceof ClickListener) {
                    ((ClickListener) listener).onReplyClicked(tx);
                }
            });
            binding.ivChat.setOnClickListener(view -> {
                if (listener != null && listener instanceof ClickListener) {
                    ((ClickListener) listener).onChatClicked(tx);
                }
            });
            binding.ivLongPress.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemLongClicked(binding.tvMsg, tx);
                }
            });
            binding.ivBan.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onBanClicked(tx);
                }
            });
            binding.tvLink.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onLinkClick(tx.link);
                }
            });
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && listener instanceof ClickListener) {
                    ((ClickListener) listener).onItemClicked(tx);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClicked(binding.tvMsg, tx);
                }
                return false;
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        View view = holder.binding.getRoot().findViewById(R.id.iv_head_pic);
        BitmapUtil.recycleImageView(view);
        super.onViewRecycled(holder);
    }

    public interface ClickListener extends BaseClickListener {
        void onRetweetClicked(UserAndTx tx);
        void onReplyClicked(UserAndTx tx);
        void onChatClicked(UserAndTx tx);
        void onItemClicked(UserAndTx tx);
    }

    public interface BaseClickListener {
        void onUserClicked(String publicKey);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onLinkClick(String link);
        void onBanClicked(UserAndTx tx);
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            boolean isSame = false;
            if (null == oldItem.sender && null == newItem.sender) {
                isSame = true;
            } else if(null != oldItem.sender && null != newItem.sender) {
                isSame =  StringUtil.isEquals(oldItem.sender.nickname, newItem.sender.nickname);
                if (isSame) {
                    isSame = StringUtil.isEquals(oldItem.sender.remark, newItem.sender.remark);
                }
            }
            if (isSame && oldItem.balance != newItem.balance) {
                isSame = false;
            }
            if (isSame && oldItem.power != newItem.power) {
                isSame = false;
            }
            if (isSame && oldItem.repliesNum != newItem.repliesNum) {
                isSame = false;
            }
            if (isSame && oldItem.chatsNum != newItem.chatsNum) {
                isSame = false;
            }
            if (isSame && oldItem.pinnedTime != newItem.pinnedTime) {
                isSame = false;
            }
            if (isSame && oldItem.favoriteTime != newItem.favoriteTime) {
                isSame = false;
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
