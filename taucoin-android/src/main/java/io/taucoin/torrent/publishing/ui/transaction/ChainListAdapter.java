package io.taucoin.torrent.publishing.ui.transaction;

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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.databinding.ItemChainBinding;
import io.taucoin.torrent.publishing.ui.customviews.AutoLinkTextView;

/**
 * Chain列表显示的Adapter
 */
public class ChainListAdapter extends ListAdapter<UserAndTx, ChainListAdapter.ViewHolder> {

    private ClickListener listener;
    private String chainID;

    ChainListAdapter(ClickListener listener, String chainID) {
        super(diffCallback);
        this.listener = listener;
        this.chainID = chainID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChainBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_chain, parent, false);
        return new ViewHolder(binding, listener, chainID);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemChainBinding binding;
        private ClickListener listener;
        private String chainID;

        ViewHolder(ItemChainBinding binding, ClickListener listener, String chainID) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.chainID = chainID;
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx || StringUtil.isEmpty(chainID)){
                return;
            }
            binding.tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_CHAIN));
            // 添加link解析
            Linkify.addLinks(binding.tvMsg, Linkify.WEB_URLS);
            Pattern airdrop = Pattern.compile(UrlUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(UrlUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(binding.tvMsg, chain, null);
//            Pattern hash = Pattern.compile(UrlUtil.HASH_PATTERN, 0);
//            Linkify.addLinks(binding.tvMsg, hash, null);

            setClickListener(binding.tvMsg, tx);

            boolean isResend = StringUtil.isEquals(tx.senderPk, MainApplication.getInstance().getPublicKey());
            binding.ivResend.setVisibility(isResend ? View.VISIBLE : View.GONE);
            if (isResend) {
                binding.ivResend.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onResendClick(tx.txID);
                    }
                });
            }
        }

        private void setClickListener(AutoLinkTextView tvMsg, UserAndTx tx) {
            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {
                @Override
                public void onClick(AutoLinkTextView view) {

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

    public interface ClickListener {
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onItemClicked(UserAndTx tx);
        void onLinkClick(String link);
        void onResendClick(String txID);
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
