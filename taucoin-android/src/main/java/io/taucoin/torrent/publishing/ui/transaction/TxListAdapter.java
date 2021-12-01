package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemNoteBinding;
import io.taucoin.torrent.publishing.databinding.ItemNoteRightBinding;
import io.taucoin.torrent.publishing.databinding.ItemWiringTxBinding;
import io.taucoin.torrent.publishing.databinding.MsgLeftViewBinding;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;

/**
 * 消息/交易列表显示的Adapter
 */
public class TxListAdapter extends ListAdapter<UserAndTx, TxListAdapter.ViewHolder> {

    enum ViewType {
        WIRING_TX,
        LEFT_NOTE,
        RIGHT_NOTE
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
//        if (viewType == ViewType.WIRING_TX.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx, parent, false);
//        } else if (viewType == ViewType.RIGHT_NOTE.ordinal()) {
//            binding = DataBindingUtil.inflate(inflater,
//                    R.layout.item_note_right, parent, false);
//        } else {
//            binding = DataBindingUtil.inflate(inflater,
//                    R.layout.item_note, parent, false);
//        }
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
        if(tx != null){
            if (tx.txType == TxType.WRING_TX.getType()) {
                return ViewType.WIRING_TX.ordinal();
            } else if (tx.txType == TxType.CHAIN_NOTE.getType()) {
                if (StringUtil.isEquals(userPk, tx.senderPk)) {
                    return ViewType.RIGHT_NOTE.ordinal();
                } else {
                    return ViewType.LEFT_NOTE.ordinal();
                }
            }
            return tx.txType;
        }
        return -1;
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
            String time = DateUtil.getWeekTime(tx.timestamp);
            int bgColor = Utils.getGroupColor(tx.senderPk);
            String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
            SpannableStringBuilder memo = Utils.getSpannableStringUrl(tx.memo);
            String firstLettersName = StringUtil.getFirstLettersOfName(showName);
            String userName = UsersUtil.getUserName(tx.sender, tx.senderPk);
            if (binding instanceof ItemWiringTxBinding) {
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                String amount = FmtMicrometer.fmtBalance(tx.amount) + " " +
                        ChainIDUtil.getCoinName(chainID);
                txBinding.tvAmount.setText(amount);
                txBinding.tvSender.setText(tx.senderPk);
                txBinding.tvReceiver.setText(tx.receiverPk);
                txBinding.tvFee.setText(FmtMicrometer.fmtFeeValue(tx.fee));
                txBinding.tvHash.setText(tx.txID);
                txBinding.tvNonce.setText(FmtMicrometer.fmtLong(tx.nonce));
                txBinding.tvMemo.setText(memo);
                txBinding.tvTime.setText(time);
                txBinding.llBlockNumber.setVisibility(tx.txStatus == 1 ? View.VISIBLE : View.GONE);
                txBinding.tvBlockNumber.setText(FmtMicrometer.fmtLong(tx.blockNumber));
                boolean isMine = StringUtil.isEquals(tx.senderPk, MainApplication.getInstance().getPublicKey());
                txBinding.middleView.setBackgroundResource(isMine ? R.drawable.red_rect_round_bg_big_radius :
                        R.drawable.white_rect_round_bg_big_radius);
                setOnLongClickListener(txBinding.middleView, tx, tx.memo);
//                setLeftViewClickListener(txBinding.leftView, tx);
            } else if (binding instanceof ItemNoteRightBinding) {
                ItemNoteRightBinding noteBinding = (ItemNoteRightBinding) holder.binding;
                noteBinding.leftView.roundButton.setBgColor(bgColor);
                noteBinding.leftView.tvName.setText(firstLettersName);
                noteBinding.leftView.tvEditName.setText(userName);
                noteBinding.leftView.tvBalance.setText(UsersUtil.getShowBalance(tx.senderBalance));

                noteBinding.tvMsg.setText(memo);
                noteBinding.tvTime.setText(time);

                if(StringUtil.isEquals(tx.senderPk,
                        MainApplication.getInstance().getPublicKey())){
                    noteBinding.leftView.tvBlacklist.setVisibility(View.GONE);
                }
                setOnLongClickListener(noteBinding.middleView, tx, tx.memo);
                setLeftViewClickListener(noteBinding.leftView, tx);
            } else {
                ItemNoteBinding noteBinding = (ItemNoteBinding) holder.binding;
                noteBinding.leftView.roundButton.setBgColor(bgColor);
                noteBinding.leftView.tvName.setText(firstLettersName);
                noteBinding.leftView.tvEditName.setText(userName);
                noteBinding.leftView.tvBalance.setText(UsersUtil.getShowBalance(tx.senderBalance));

                noteBinding.tvMsg.setText(memo);
                noteBinding.tvTime.setText(time);

                if (StringUtil.isEquals(tx.senderPk,
                        MainApplication.getInstance().getPublicKey())) {
                    noteBinding.leftView.tvBlacklist.setVisibility(View.GONE);
                }
                setOnLongClickListener(noteBinding.middleView, tx, tx.memo);
                setLeftViewClickListener(noteBinding.leftView, tx);
            }
        }

        private void setLeftViewClickListener(MsgLeftViewBinding binding, UserAndTx tx) {
            binding.roundButton.setOnClickListener(view ->{
                if (listener != null) {
                    listener.onUserClicked(tx.senderPk);
                }
            });
            binding.tvEditName.setOnClickListener(view ->{
                if(listener != null){
                    listener.onEditNameClicked(tx.senderPk);
                }
            });
            binding.tvBlacklist.setOnClickListener(view ->{
                if(listener != null){
                    listener.onBanClicked(tx);
                }
            });
        }

        private void setOnLongClickListener(View llMsg, UserAndTx tx, String msg) {
            llMsg.setOnLongClickListener(view ->{
                if(listener != null){
                    listener.onItemLongClicked(tx, msg);
                }
                return false;
            });
        }
    }

    public interface ClickListener {
        void onUserClicked(String publicKey);
        void onEditNameClicked(String publicKey);
        void onBanClicked(UserAndTx tx);
        void onItemLongClicked(UserAndTx tx, String msg);
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
            if(isSame && oldItem.senderBalance != newItem.senderBalance){
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
