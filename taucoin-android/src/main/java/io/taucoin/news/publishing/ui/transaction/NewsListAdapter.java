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
import io.taucoin.news.publishing.databinding.ItemNewsBinding;
import io.taucoin.news.publishing.ui.customviews.AutoLinkTextView;

/**
 * Market 列表显示的Adapter
 */
public class NewsListAdapter extends ListAdapter<UserAndTx, NewsListAdapter.ViewHolder> {
    private final ClickListener listener;

    NewsListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNewsBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_news, parent, false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;
        private final ClickListener listener;
        private final int nameColor;
        private final int linkDrawableSize;
        private final Drawable drawable;

        ViewHolder(ItemNewsBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.nameColor = binding.getRoot().getResources().getColor(R.color.color_black);
            this.linkDrawableSize = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.widget_size_14);
            this.drawable = DrawablesUtil.getDrawable(binding.getRoot().getContext(),
                    R.mipmap.icon_share_link, linkDrawableSize, linkDrawableSize);
        }

        void bind(UserAndTx tx) {
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
            binding.ivBan.setVisibility(isMyself ? View.GONE : View.VISIBLE);
            boolean isHavePicture = StringUtil.isNotEmpty(tx.picturePath);
            binding.ivPicture.setEnabled(isHavePicture);
            binding.ivPicture.setImageResource(isHavePicture ? R.mipmap.icon_picture_gray : R.mipmap.icon_picture_white);

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
        }

        AutoLinkTextView.AutoLinkListener autoLinkListener = new AutoLinkTextView.AutoLinkListener() {

            @Override
            public void onClick(AutoLinkTextView view) {
                if (listener != null) {
                    Object tag = view.getTag();
                    if (tag != null) {
                        UserAndTx tx = (UserAndTx) view.getTag();
                        listener.onItemClicked(tx);
                    }
                }
            }

            @Override
            public void onLongClick(AutoLinkTextView view) {
                Object tag = view.getTag();
                if (tag != null) {
                    UserAndTx tx = (UserAndTx) view.getTag();
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

        private void setClickListener(ItemNewsBinding binding, UserAndTx tx) {
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
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(tx);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClicked(binding.tvMsg, tx);
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
        void onItemClicked(UserAndTx tx);
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
            if (isSame && StringUtil.isNotEquals(oldItem.picturePath, newItem.picturePath)) {
                return false;
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
