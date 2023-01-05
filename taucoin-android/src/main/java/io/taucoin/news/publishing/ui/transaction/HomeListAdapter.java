package io.taucoin.news.publishing.ui.transaction;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.model.data.UserAndTxReply;
import io.taucoin.news.publishing.core.utils.BitmapUtil;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.DateUtil;
import io.taucoin.news.publishing.core.utils.DrawablesUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.Logarithm;
import io.taucoin.news.publishing.core.utils.SpanUtils;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ItemHomeNewsBinding;
import io.taucoin.news.publishing.ui.customviews.AutoLinkTextView;

/**
 * Market 列表显示的Adapter
 */
public class HomeListAdapter extends ListAdapter<UserAndTxReply, HomeListAdapter.ViewHolder> {
    private final ClickListener listener;

    HomeListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemHomeNewsBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_home_news, parent, false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHomeNewsBinding binding;
        private final ClickListener listener;
        private final int nameColor;
        private final int linkDrawableSize;
        private final Drawable drawable;

        ViewHolder(ItemHomeNewsBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.nameColor = binding.getRoot().getResources().getColor(R.color.color_black);
            this.linkDrawableSize = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.widget_size_14);
            this.drawable = DrawablesUtil.getDrawable(binding.getRoot().getContext(),
                    R.mipmap.icon_share_link, linkDrawableSize, linkDrawableSize);
        }

        void bind(UserAndTxReply tx) {
            if(null == binding || null == tx){
                return;
            }
            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));

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
            binding.ivBan.setImageResource(isMyself ? R.mipmap.icon_ban_disabled : R.mipmap.icon_ban_gray);
            binding.ivBan.setEnabled(!isMyself);

            double showPower = Logarithm.log2(2 + tx.power);
            String power = FmtMicrometer.formatThreeDecimal(showPower);
            String balance = FmtMicrometer.fmtBalance(tx.getInterimBalance());
            binding.tvBalance.setText(balance);
            binding.tvPower.setText(power);

            SpannableStringBuilder msg = new SpannableStringBuilder()
                    .append(tx.memo)
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
            Linkify.addLinks(binding.tvMsg, LinkUtil.REFERRAL, null);
            Linkify.addLinks(binding.tvMsg, LinkUtil.AIRDROP, null);
            Linkify.addLinks(binding.tvMsg, LinkUtil.CHAIN, null);
            Linkify.addLinks(binding.tvMsg, LinkUtil.FRIEND, null);
            binding.tvMsg.requestLayout();

            if (isShowLink) {
                DrawablesUtil.setUnderLine(binding.tvLink);
                DrawablesUtil.setEndDrawable(binding.tvLink, drawable);
                binding.tvLink.requestLayout();
            }
            binding.tvMsg.setTag(tx);
            binding.tvMsg.setAutoLinkListener(autoLinkListener);

            boolean isHaveReply = StringUtil.isNotEmpty(tx.replyTxID);
            binding.llBottomReply.setVisibility(isHaveReply ? View.VISIBLE : View.GONE);
            if (isHaveReply) {
                binding.ivReplyHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.replySender));
                String replyUserName = UsersUtil.getShowName(tx.replySender);
                replyUserName = null == replyUserName ? "" : replyUserName;
                SpanUtils replyName = new SpanUtils()
                        .append(replyUserName)
                        .setForegroundColor(nameColor)
                        .append("(")
                        .append(UsersUtil.getLastPublicKey(tx.replySenderPk, 4))
                        .append(")")
                        .append("@")
                        .append(communityName)
                        .append("(").append(communityCode).append(")")
                        .append(" · ").append(DateUtil.getNewsTime(tx.replyTimestamp));
                binding.tvReplyName.setText(replyName.create());

                String replyPower = FmtMicrometer.formatThreeDecimal(Logarithm.log2(2 + tx.replyPower));
                String replyBalance = FmtMicrometer.fmtBalance(tx.getReplyInterimBalance());
                binding.tvReplyBalance.setText(replyBalance);
                binding.tvReplyPower.setText(replyPower);

                binding.tvReplyMsg.setText(tx.replyMemo);
                // 添加link解析
                binding.tvReplyMsg.setAutoLinkMask(0);
                Linkify.addLinks(binding.tvReplyMsg, Linkify.WEB_URLS);
                Linkify.addLinks(binding.tvReplyMsg, LinkUtil.REFERRAL, null);
                Linkify.addLinks(binding.tvReplyMsg, LinkUtil.AIRDROP, null);
                Linkify.addLinks(binding.tvReplyMsg, LinkUtil.CHAIN, null);
                Linkify.addLinks(binding.tvReplyMsg, LinkUtil.FRIEND, null);
                binding.tvReplyMsg.requestLayout();

                binding.tvReplyMsg.setTag(tx);
                binding.tvReplyMsg.setAutoLinkListener(autoLinkListener);

                boolean isShowReplyLink = StringUtil.isNotEmpty(tx.replyLink);
                binding.tvReplyLink.setText(tx.replyLink);
                binding.tvReplyLink.setVisibility(isShowReplyLink ? View.VISIBLE : View.GONE);
                if (isShowLink) {
                    DrawablesUtil.setUnderLine(binding.tvReplyLink);
                    DrawablesUtil.setEndDrawable(binding.tvReplyLink, drawable);
                    binding.tvReplyLink.requestLayout();
                }
            }
        }

        AutoLinkTextView.AutoLinkListener autoLinkListener = new AutoLinkTextView.AutoLinkListener() {

            @Override
            public void onClick(AutoLinkTextView view) {
                if (listener != null) {
                    Object tag = view.getTag();
                    if (tag != null) {
                        UserAndTxReply tx = (UserAndTxReply) view.getTag();
                        listener.onItemClicked(tx.txID);
                    }
                }
            }

            @Override
            public void onLongClick(AutoLinkTextView view) {
                if (view.getId() != R.id.tv_msg) {
                    return;
                }
                Object tag = view.getTag();
                if (tag != null) {
                    UserAndTxReply tx = (UserAndTxReply) view.getTag();
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

        private void setClickListener(ItemHomeNewsBinding binding, UserAndTxReply tx) {
            binding.ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });
            binding.ivReplyHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.replySenderPk);
                }
            });

            binding.ivRetweet.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onRetweetClicked(tx);
                }
            });
            binding.ivReply.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onReplyClicked(tx);
                }
            });
            binding.ivChat.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onChatClicked(tx);
                }
            });
            binding.ivDelete.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onDeleteClicked(tx);
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
            binding.tvReplyLink.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onLinkClick(tx.replyLink);
                }
            });
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(tx.txID);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClicked(binding.tvMsg, tx);
                }
                return false;
            });
            binding.llBottomReply.setOnLongClickListener(v -> {
                if (listener != null) {
//                    listener.onItemLongClicked(binding.tvMsg, tx);
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

    public interface ClickListener {
        void onRetweetClicked(UserAndTx tx);
        void onReplyClicked(UserAndTx tx);
        void onChatClicked(UserAndTx tx);
        void onDeleteClicked(UserAndTx tx);
        void onItemClicked(String txID);
        void onUserClicked(String publicKey);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onLinkClick(String link);
        void onBanClicked(UserAndTx tx);
    }

    private static final DiffUtil.ItemCallback<UserAndTxReply> diffCallback = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTxReply oldItem, @NonNull UserAndTxReply newItem) {
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
                return false;
            }
            if (isSame && oldItem.power != newItem.power) {
                return false;
            }
            if (isSame && oldItem.repliesNum != newItem.repliesNum) {
                return false;
            }
            if (isSame && oldItem.chatsNum != newItem.chatsNum) {
                return false;
            }
            if (isSame && oldItem.pinnedTime != newItem.pinnedTime) {
                return false;
            }
            if (isSame && oldItem.favoriteTime != newItem.favoriteTime) {
                return false;
            }
            if (isSame && StringUtil.isNotEquals(oldItem.replyTxID, newItem.replyTxID)) {
                return false;
            }
            if (isSame && oldItem.replyBalance != newItem.replyBalance) {
                return false;
            }
            if (isSame && oldItem.replyPower != newItem.replyPower) {
                return false;
            }
            if (isSame && oldItem.replyTimestamp != newItem.replyTimestamp) {
                return false;
            }
            if (null == oldItem.replySender && null == newItem.replySender) {
                return true;
            }
            if(null != oldItem.replySender && null != newItem.replySender) {
                isSame =  StringUtil.isEquals(oldItem.replySender.nickname, newItem.replySender.nickname);
                if (isSame) {
                    isSame = StringUtil.isEquals(oldItem.replySender.remark, newItem.replySender.remark);
                }
                return isSame;
            } else {
                return false;
            }
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndTxReply oldItem, @NonNull UserAndTxReply newItem) {
            return oldItem.equals(newItem);
        }
    };
}
