package io.taucbd.news.publishing.ui.setting;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemFavoriteBinding;
import io.taucbd.news.publishing.ui.customviews.AutoLinkTextView;
import io.taucbd.news.publishing.ui.transaction.CommunityTabFragment;
import io.taucbd.news.publishing.ui.transaction.TxUtils;

/**
 * 收藏列表显示的Adapter
 */
public class FavoriteListAdapter extends PagedListAdapter<UserAndTx,
        FavoriteListAdapter.ViewHolder> {
    private ClickListener listener;

    FavoriteListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFavoriteBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_favorite,
                    parent,
                    false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFavoriteBinding binding;
        private Context context;
        private ClickListener listener;

        ViewHolder(ItemFavoriteBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }

            String time = DateUtil.getWeekTime(tx.favoriteTime);
            String communityName = ChainIDUtil.getName(tx.chainID);
            String userName = UsersUtil.getShowName(tx.sender);
            SpannableStringBuilder memo = TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NOTES);

            ItemFavoriteBinding favoriteBinding = holder.binding;
            favoriteBinding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            favoriteBinding.tvUserName.setText(userName);
            favoriteBinding.tvCommunityName.setText(communityName);
            favoriteBinding.tvTime.setText(time);

            favoriteBinding.tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NOTES));
            // 添加link解析
            Linkify.addLinks(favoriteBinding.tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(favoriteBinding.tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(favoriteBinding.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(favoriteBinding.tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(favoriteBinding.tvMsg, friend, null);

            favoriteBinding.ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });

            favoriteBinding.tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {
                @Override
                public void onClick(AutoLinkTextView view) {

                }

                @Override
                public void onLongClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onItemLongClicked(favoriteBinding.tvMsg, tx);
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

    public interface ClickListener {
        void onUserClicked(String publicKey);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onLinkClick(String link);
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<UserAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            boolean isSame = false;
            if (null == oldItem.sender && null == newItem.sender) {
                isSame = true;
            } else if(null != oldItem.sender && null != newItem.sender) {
                isSame =  StringUtil.isEquals(oldItem.sender.nickname, newItem.sender.nickname);
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
