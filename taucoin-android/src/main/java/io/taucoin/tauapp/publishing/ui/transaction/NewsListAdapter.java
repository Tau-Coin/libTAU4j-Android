package io.taucoin.tauapp.publishing.ui.transaction;

import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.common.StringUtils;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.core.utils.BitmapUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.databinding.ItemMarketBinding;
import io.taucoin.tauapp.publishing.databinding.ItemNewsBinding;
import io.taucoin.tauapp.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.tauapp.publishing.ui.customviews.RoundImageView;

/**
 * Market 列表显示的Adapter
 */
public class NewsListAdapter extends ListAdapter<UserAndTx, NewsListAdapter.ViewHolder> {

    private ClickListener listener;

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
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;
        private final ClickListener listener;
        private final int nameColor;

        ViewHolder(ItemNewsBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.nameColor = binding.getRoot().getResources().getColor(R.color.color_black);
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }
            boolean isSell = tx.txType == TxType.SELL_TX.getType();
            binding.ivArrow.setVisibility(isSell ? View.VISIBLE : View.INVISIBLE);

            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            setLeftViewClickListener(binding.ivHeadPic, tx);

            String userName = UsersUtil.getShowName(tx.sender);
            userName = null == userName ? "" : userName;
            String communityName = ChainIDUtil.getName(tx.chainID);
            String communityCode = ChainIDUtil.getCode(tx.chainID);
            SpannableStringBuilder name = new SpanUtils()
                    .append(userName)
                    .setForegroundColor(nameColor)
                    .append(" @")
                    .append(UsersUtil.getLastPublicKey(tx.senderPk, 4))
                    .append(" · ")
                    .append(communityName)
                    .append("(").append(communityCode).append(")")
                    .append(" · ")
                    .append(DateUtil.getNewsTime(tx.timestamp))
                    .create();
            binding.tvName.setText(name);
//            setEditNameClickListener(binding.tvName, tx);

//            binding.tvTrust.setText(FmtMicrometer.fmtLong(tx.trusts));
            setImageClickListener(binding, tx);

            binding.tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NEWS));
            // 添加link解析
            Linkify.addLinks(binding.tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, friend, null);

            setClickListener(binding.tvMsg, tx);
            binding.tvMsg.requestLayout();
        }

        private void setImageClickListener(ItemNewsBinding binding, UserAndTx tx) {
            binding.ivTrust.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onTrustClicked(tx);
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
            binding.ivLongPress.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemLongClicked(binding.tvMsg, tx);
                }
            });
        }

        private void setLeftViewClickListener(RoundImageView ivHeadPic, UserAndTx tx) {
            ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });
        }

        private void setEditNameClickListener(TextView textView, UserAndTx tx) {
            textView.setOnClickListener(view ->{
                if(listener != null){
                    listener.onEditNameClicked(tx.senderPk);
                }
            });
        }

        private void setClickListener(AutoLinkTextView tvMsg, UserAndTx tx) {
            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {
                @Override
                public void onClick(AutoLinkTextView view) {
                    if (tx.txType == TxType.SELL_TX.getType() && listener != null) {
                        listener.onItemClicked(tx);
                    }
                }

                @Override
                public void onLongClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onItemLongClicked(tvMsg, tx);
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
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        View view = holder.binding.getRoot().findViewById(R.id.iv_head_pic);
        BitmapUtil.recycleImageView(view);
        super.onViewRecycled(holder);
    }

    public interface ClickListener {
        void onTrustClicked(UserAndTx user);
        void onUserClicked(String publicKey);
        void onEditNameClicked(String publicKey);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onItemClicked(UserAndTx tx);
        void onLinkClick(String link);
        void onRetweetClicked(UserAndTx tx);
        void onReplyClicked(UserAndTx tx);
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
            if (isSame && oldItem.trusts != newItem.trusts) {
                isSame = false;
            }
            if (isSame && oldItem.txStatus != newItem.txStatus) {
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
