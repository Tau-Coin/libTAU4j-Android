package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ItemLeftNoteBinding;
import io.taucoin.torrent.publishing.databinding.ItemLeftSellBinding;
import io.taucoin.torrent.publishing.databinding.ItemRightNoteBinding;
import io.taucoin.torrent.publishing.databinding.ItemRightSellBinding;
import io.taucoin.torrent.publishing.databinding.ItemTrustBinding;
import io.taucoin.torrent.publishing.databinding.TxLeftViewBinding;

/**
 * 消息/交易列表显示的Adapter
 */
public class TxListAdapter extends ListAdapter<UserAndTx, TxListAdapter.ViewHolder> {

    enum ViewType {
        UNKNOWN,
        SELL_LEFT,
        SELL_RIGHT,
        NOTE_LEFT,
        NOTE_RIGHT,
        TRUST_ITEM,
    }

    private ClickListener listener;
    private String chainID;

    TxListAdapter(ClickListener listener, String chainID) {
        super(diffCallback);
        this.listener = listener;
        this.chainID = chainID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if (viewType == ViewType.NOTE_RIGHT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_right_note, parent, false);
        } else if (viewType == ViewType.NOTE_LEFT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_left_note, parent, false);
        } else if (viewType == ViewType.TRUST_ITEM.ordinal()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_trust, parent, false);
        } else if (viewType == ViewType.SELL_RIGHT.ordinal()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_right_sell, parent, false);
        } else if (viewType == ViewType.SELL_LEFT.ordinal()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_left_sell, parent, false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_unknown_tx, parent, false);
        }
        return new ViewHolder(binding, listener, chainID);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        UserAndTx tx = getItem(position);
        String userPk = MainApplication.getInstance().getPublicKey();
        int viewType = ViewType.UNKNOWN.ordinal();
        if (tx != null) {
            boolean isMine = StringUtil.isEquals(userPk, tx.senderPk);
            switch (TxType.valueOf(tx.txType)) {
                case NOTE_TX:
                    viewType = isMine ? ViewType.NOTE_RIGHT.ordinal() : ViewType.NOTE_LEFT.ordinal();
                    break;
                case TRUST_TX:
                    viewType = ViewType.TRUST_ITEM.ordinal();
                    break;
                case WRING_TX:
                case SELL_TX:
                    viewType = isMine ? ViewType.SELL_RIGHT.ordinal() : ViewType.SELL_LEFT.ordinal();
                    break;
            }
        }
        return viewType;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private Context context;
        private String chainID;

        ViewHolder(ViewDataBinding binding, ClickListener listener, String chainID) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
            this.listener = listener;
            this.chainID = chainID;
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx || StringUtil.isEmpty(chainID)){
                return;
            }
            TextView tvMsg;
            TextView tvBlacklist;
            TextView tvTime;
            boolean isMine;
            TxLeftViewBinding headView;
            if (binding instanceof ItemRightNoteBinding) {
                isMine = true;
                ItemRightNoteBinding rightBinding = (ItemRightNoteBinding) holder.binding;
                tvBlacklist = rightBinding.leftView.tvBlacklist;
                tvMsg = rightBinding.tvMsg;
                tvTime = rightBinding.tvTime;
                headView = rightBinding.leftView;
            } else if (binding instanceof ItemLeftNoteBinding) {
                isMine = false;
                ItemLeftNoteBinding rightBinding = (ItemLeftNoteBinding) holder.binding;
                tvBlacklist = rightBinding.leftView.tvBlacklist;
                tvMsg = rightBinding.tvMsg;
                tvTime = rightBinding.tvTime;
                headView = rightBinding.leftView;
            } else if (binding instanceof ItemRightSellBinding) {
                isMine = true;
                ItemRightSellBinding rightBinding = (ItemRightSellBinding) holder.binding;
                tvBlacklist = rightBinding.leftView.tvBlacklist;
                tvMsg = rightBinding.tvMsg;
                tvTime = rightBinding.tvTime;
                headView = rightBinding.leftView;

                boolean isShowTrust = tx.txType == TxType.SELL_TX.getType();
                rightBinding.tvTrust.setVisibility(isShowTrust ? View.VISIBLE : View.GONE);
                rightBinding.ivTrust.setVisibility(isShowTrust ? View.VISIBLE : View.GONE);
                if (isShowTrust) {
                    rightBinding.tvTrust.setText(FmtMicrometer.fmtLong(tx.trusts));
                    setTrustClickListener(rightBinding.ivTrust, tx);
                }
            } else if (binding instanceof ItemLeftSellBinding) {
                isMine = false;
                ItemLeftSellBinding leftBinding = (ItemLeftSellBinding) holder.binding;
                tvBlacklist = leftBinding.leftView.tvBlacklist;
                tvMsg = leftBinding.tvMsg;
                tvTime = leftBinding.tvTime;
                headView = leftBinding.leftView;
                leftBinding.tvName.setText(UsersUtil.getShowName(tx.sender));
                setEditNameClickListener(leftBinding.tvName, tx);

                boolean isShowTrust = tx.txType == TxType.SELL_TX.getType();
                leftBinding.tvTrust.setVisibility(isShowTrust ? View.VISIBLE : View.GONE);
                leftBinding.ivTrust.setVisibility(isShowTrust ? View.VISIBLE : View.GONE);
                if (isShowTrust) {
                    leftBinding.tvTrust.setText(FmtMicrometer.fmtLong(tx.trusts));
                    setTrustClickListener(leftBinding.ivTrust, tx);
                }
            }  else if (binding instanceof ItemTrustBinding) {
                ItemTrustBinding trustBinding = (ItemTrustBinding) holder.binding;
                String time = DateUtil.getWeekTime(tx.timestamp);
                String senderName = UsersUtil.getShowName(tx.sender);
                String receiverName = UsersUtil.getShowName(tx.receiver);
                trustBinding.tvTrust.setText(context.getString(R.string.tx_give_trust_info, time,
                        senderName, receiverName));
                return;
            } else {
                return;
            }
            tvBlacklist.setVisibility(isMine ? View.GONE : View.VISIBLE);
            String time = DateUtil.getWeekTime(tx.timestamp);
            tvTime.setText(time);
            headView.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            tvMsg.setText(TxUtils.createTxSpan(tx));

            setOnLongClickListener((View) tvMsg.getParent(), tx, tx.memo);
            setLeftViewClickListener(headView, tx);
        }

        private void setTrustClickListener(ImageView ivTrust, UserAndTx tx) {
            ivTrust.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onTrustClicked(tx.sender);
                }
            });
        }

        private void setLeftViewClickListener(TxLeftViewBinding binding, UserAndTx tx) {
            binding.ivHeadPic.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });
            binding.tvBlacklist.setOnClickListener(view -> {
                if(listener != null){
                    listener.onBanClicked(tx);
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

        private void setOnLongClickListener(View llMsg, UserAndTx tx, String msg) {
//            llMsg.setOnLongClickListener(view ->{
//                if(listener != null){
//                    listener.onItemLongClicked(tx, msg);
//                }
//                return false;
//            });

            if (tx.txType == TxType.SELL_TX.getType()) {
                llMsg.setOnClickListener(view ->{
                    if(listener != null){
                        listener.onItemClicked(tx);
                    }
                });
            }
        }
    }

    public interface ClickListener {
        void onTrustClicked(User user);
        void onUserClicked(String publicKey);
        void onEditNameClicked(String publicKey);
        void onBanClicked(UserAndTx tx);
        void onItemLongClicked(UserAndTx tx, String msg);
        void onItemClicked(UserAndTx tx);
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<UserAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            boolean isSame = false;
            if (null == oldItem.sender && null == newItem.sender) {
                isSame = true;
            } else if(null != oldItem.sender && null != newItem.sender){
                isSame =  StringUtil.isEquals(oldItem.sender.nickname, newItem.sender.nickname);
            }
            if(isSame && oldItem.trusts != newItem.trusts){
                isSame = false;
            }
            if(isSame && oldItem.txStatus != newItem.txStatus){
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
