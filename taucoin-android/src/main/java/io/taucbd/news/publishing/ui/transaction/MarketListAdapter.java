package io.taucbd.news.publishing.ui.transaction;

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
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.utils.BitmapUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemMarketBinding;
import io.taucbd.news.publishing.ui.customviews.AutoLinkTextView;
import io.taucbd.news.publishing.ui.customviews.RoundImageView;

/**
 * Market 列表显示的Adapter
 */
public class MarketListAdapter extends ListAdapter<UserAndTx, MarketListAdapter.ViewHolder> {

    private ClickListener listener;

    MarketListAdapter(ClickListener listener, String chainID) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMarketBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_news, parent, false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMarketBinding binding;
        private ClickListener listener;

        ViewHolder(ItemMarketBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }
            //boolean isSell = tx.txType == TxType.SELL_TX.getType();
            boolean isSell = false;
            binding.ivArrow.setVisibility(isSell ? View.VISIBLE : View.INVISIBLE);

            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            setLeftViewClickListener(binding.ivHeadPic, tx);

            binding.tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_MARKET));
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
        }

        private void setLeftViewClickListener(RoundImageView ivHeadPic, UserAndTx tx) {
            ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });
        }

        private void setClickListener(AutoLinkTextView tvMsg, UserAndTx tx) {
            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {
                @Override
                public void onClick(AutoLinkTextView view) {
                    /*
                    if (tx.txType == TxType.SELL_TX.getType() && listener != null) {
                        listener.onItemClicked(tx);
                    }
                    */
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
        void onUserClicked(String publicKey);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onItemClicked(UserAndTx tx);
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
            if (isSame && oldItem.repliesNum != newItem.repliesNum) {
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
