package io.taucbd.news.publishing.ui.transaction;

import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.model.data.UserAndTxReply;
import io.taucbd.news.publishing.core.utils.BitmapUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.DrawablesUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.Logarithm;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemHomeNewsBinding;
import io.taucbd.news.publishing.ui.customviews.AutoLinkTextView;

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

        public void setVisibility(View itemView, boolean isVisible) {
            RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) itemView.getLayoutParams();
            if (null == param) {
                return;
            }
            if (isVisible) {
                param.height = LinearLayout.LayoutParams.WRAP_CONTENT;// 这里注意使用自己布局的根布局类型
                param.width = LinearLayout.LayoutParams.MATCH_PARENT;// 这里注意使用自己布局的根布局类型
                itemView.setVisibility(View.VISIBLE);
            } else {
                itemView.setVisibility(View.GONE);
                param.height = 0;
                param.width = 0;
            }
            itemView.setLayoutParams(param);
        }

        void bind(UserAndTxReply tx) {
            if(null == binding || null == tx){
                return;
            }
            if (!tx.isShow) {
                setVisibility(binding.getRoot(), false);
                return;
            }
            setVisibility(binding.getRoot(), true);
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
            binding.ivBan.setVisibility(isMyself ? View.GONE : View.VISIBLE);
            boolean isHavePicture = StringUtil.isNotEmpty(tx.picturePath);
            binding.ivPicture.setVisibility(isHavePicture ? View.VISIBLE : View.INVISIBLE);

            double showPower = Logarithm.log2(2 + tx.power);
            String power = FmtMicrometer.formatThreeDecimal(showPower);
            String balance = FmtMicrometer.fmtBalance(tx.getInterimBalance());
            binding.tvBalance.setText(balance);
            binding.tvPower.setText(power);

//            SpannableStringBuilder msg = new SpannableStringBuilder()
//                    .append(tx.memo);
//                    .append(" ");
            binding.tvMsg.setEllipsizeText(tx.memo);
//            // 防止省略号闪烁
//            UserAndTxReply oldTx = (UserAndTxReply) binding.tvMsg.getTag();
//            if (null == oldTx || StringUtil.isNotEquals(oldTx.txID, tx.txID)) {
//                binding.tvMsg.setText(msg);
//                binding.tvMsg.setMaxLines(5);
//                binding.tvMsg.post(() -> {
//                    if (binding.tvMsg.getLineCount() >= 5){
//                        String text = binding.tvMsg.getText().toString();
//                        String ellipsis = "......";
//                        if (binding.tvMsg.getLineCount() == 5 && text.endsWith(ellipsis)) {
//                            return;
//                        }
//                        int lineEndIndex4 = binding.tvMsg.getLayout().getLineEnd(4);
//                        if (lineEndIndex4 < msg.length()) {
//                            int lineEndIndex3 = binding.tvMsg.getLayout().getLineEnd(3);
//                            text = text.substring(0, lineEndIndex4);
//                            String lineBreak = "\n";
//
//                            boolean suffix = text.endsWith(lineBreak);
//                            if (lineEndIndex4 - lineEndIndex3 > 7) {
//                                if (suffix) {
//                                    text = text.substring(0, text.length() - ellipsis.length() - lineBreak.length());
//                                } else {
//                                    text = text.substring(0, text.length() - ellipsis.length());
//                                }
//                            } else {
//                                if (suffix) {
//                                    text = text.substring(0, text.length() - lineBreak.length());
//                                }
//                            }
//                            text += ellipsis;
//                            binding.tvMsg.setText(text);
//                        }
//                    }
//                });
//            }

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
//            binding.tvMsg.requestLayout();

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
                binding.ivReplyHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.replySender));

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
            binding.ivPicture.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPicturePreview(tx.picturePath);
                }
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
        void onPicturePreview(String picturePath);
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
            if (null == oldItem.sender && newItem.sender != null) {
                return false;
            } else if (oldItem.sender != null && null == newItem.sender) {
                return false;
            } else if (oldItem.sender != null && newItem.sender != null){
                if (StringUtil.isNotEquals(oldItem.sender.nickname, newItem.sender.nickname)) {
                    return false;
                } else if (StringUtil.isNotEquals(oldItem.sender.remark, newItem.sender.remark)) {
                    return false;
                }
            }

            if (null == oldItem.replySender && newItem.replySender != null) {
                return false;
            } else if (oldItem.replySender != null && null == newItem.replySender) {
                return false;
            } else if (oldItem.replySender != null && newItem.replySender != null){
                if (StringUtil.isNotEquals(oldItem.replySender.nickname, newItem.replySender.nickname)) {
                    return false;
                } else if (StringUtil.isNotEquals(oldItem.replySender.remark, newItem.replySender.remark)) {
                    return false;
                }
            }

            if (oldItem.balance != newItem.balance) {
                return false;
            } else if (oldItem.power != newItem.power) {
                return false;
            } else if (oldItem.repliesNum != newItem.repliesNum) {
                return false;
            } else if (oldItem.chatsNum != newItem.chatsNum) {
                return false;
            } else if (oldItem.pinnedTime != newItem.pinnedTime) {
                return false;
            } else if (oldItem.favoriteTime != newItem.favoriteTime) {
                return false;
            } else if (StringUtil.isNotEquals(oldItem.replyTxID, newItem.replyTxID)) {
                return false;
            } else if (oldItem.replyBalance != newItem.replyBalance) {
                return false;
            } else if (oldItem.replyPower != newItem.replyPower) {
                return false;
            } else if (oldItem.replyTimestamp != newItem.replyTimestamp) {
                return false;
            } else if (StringUtil.isNotEquals(oldItem.picturePath, newItem.picturePath)) {
                return false;
            } else if (oldItem.currentTime != newItem.currentTime) {
                return false;
            } else if (oldItem.isShow != newItem.isShow) {
                return false;
            }
            return true;
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndTxReply oldItem, @NonNull UserAndTxReply newItem) {
            return oldItem.equals(newItem);
        }
    };
}
