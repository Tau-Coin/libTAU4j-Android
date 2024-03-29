package io.taucbd.news.publishing.ui.transaction;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.TxLogStatus;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemLeftNoteBinding;
import io.taucbd.news.publishing.databinding.ItemRightNoteBinding;
import io.taucbd.news.publishing.databinding.TxLeftViewBinding;
import io.taucbd.news.publishing.ui.customviews.AutoLinkTextView;

/**
 * 消息/交易列表显示的Adapter
 */
public class NotesListAdapter extends ListAdapter<UserAndTx, NotesListAdapter.ViewHolder> {

    enum ViewType {
        ITEM_LEFT,
        ITEM_RIGHT
    }

    private ClickListener listener;
    private String chainID;
    private boolean isShowBan;

    NotesListAdapter(ClickListener listener, String chainID, boolean isShowBan) {
        super(diffCallback);
        this.listener = listener;
        this.chainID = chainID;
        this.isShowBan = isShowBan;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if (viewType == ViewType.ITEM_RIGHT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_right_note, parent, false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_left_note, parent, false);
        }
        return new ViewHolder(binding, listener, chainID, isShowBan);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        long previousTime = 0;
        if (position > 0) {
            previousTime = getItem(position - 1).timestamp;
        }
        UserAndTx currentTx = getItem(position);
        boolean isShowTime = DateUtil.isShowTime(currentTx.timestamp, previousTime);
        holder.bind(holder, currentTx, isShowTime);
    }

    @Override
    public int getItemViewType(int position) {
        UserAndTx tx = getItem(position);
        String userPk = MainApplication.getInstance().getPublicKey();
        if (tx != null) {
            if (StringUtil.isEquals(userPk, tx.senderPk)) {
                return ViewType.ITEM_RIGHT.ordinal();
            } else {
                return ViewType.ITEM_LEFT.ordinal();
            }
        }
        return ViewType.ITEM_LEFT.ordinal();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private String chainID;
        private boolean isShowBan;

        ViewHolder(ViewDataBinding binding, ClickListener listener, String chainID, boolean isShowBan) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.chainID = chainID;
            this.isShowBan = isShowBan;
        }

        void bind(ViewHolder holder, UserAndTx tx, boolean isShowTime) {
            if(null == binding || null == holder || null == tx || StringUtil.isEmpty(chainID)){
                return;
            }
            AutoLinkTextView tvMsg;
            TextView tvTime;
            TxLeftViewBinding headView;
            if (binding instanceof ItemRightNoteBinding) {
                ItemRightNoteBinding rightBinding = (ItemRightNoteBinding) holder.binding;
                tvMsg = rightBinding.tvMsg;
                tvTime = rightBinding.tvTime;
                headView = rightBinding.leftView;
                rightBinding.leftView.tvBlacklist.setVisibility(View.GONE);
                rightBinding.ivStatus.setVisibility(isShowBan ? View.VISIBLE : View.GONE);
                if (isShowBan) {
                    rightBinding.ivStatus.setImageResource(parseStatusReid(tx));
                    rightBinding.ivStatus.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTxLogClick(tx.txID, tx.version);
                        }
                    });
                }
            } else {
                ItemLeftNoteBinding leftBinding = (ItemLeftNoteBinding) holder.binding;
                tvMsg = leftBinding.tvMsg;
                tvTime = leftBinding.tvTime;
//                headView = leftBinding.leftView;
//                leftBinding.leftView.tvBlacklist.setVisibility(isShowBan ? View.VISIBLE : View.GONE);
                leftBinding.tvName.setText(UsersUtil.getShowName(tx.sender));
                setEditNameClickListener(leftBinding.tvName, tx);
            }

            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(tx.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);

//            headView.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NOTES));
            // 添加link解析
            Linkify.addLinks(tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(tvMsg, friend, null);

            setClickListener(tvMsg, tx);
//            setLeftViewClickListener(headView, tx);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(tvMsg, tx);
                }
            });
        }

        private static int parseStatusReid(UserAndTx msg) {
            List<TxLog> logs = msg.logs;
            if (null == logs || logs.size() <= 0) {
                return R.mipmap.icon_msg_waitting;
            }
            TxLog log = msg.logs.get(0);
            if (log.status == TxLogStatus.ARRIVED_SWARM.getStatus()) {
                return R.mipmap.icon_msg_swarm;
            } else {
                return R.mipmap.icon_msg_waitting;
            }
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

        private void setClickListener(AutoLinkTextView tvMsg, UserAndTx tx) {
            tvMsg.setAutoLinkListener(new AutoLinkTextView.AutoLinkListener() {
                @Override
                public void onClick(AutoLinkTextView view) {
                    if (listener != null) {
                        listener.onItemClicked(tvMsg, tx);
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

    public interface ClickListener {
        void onItemClicked(TextView view, UserAndTx tx);
        void onUserClicked(String publicKey);
        void onEditNameClicked(String publicKey);
        void onBanClicked(UserAndTx tx);
        void onItemLongClicked(TextView view, UserAndTx tx);
        void onLinkClick(String link);
        void onTxLogClick(String txID, int version);
    }

    private static boolean isSameConfirms(List<TxLog> oldList, List<TxLog> newList) {
        if (null == oldList && null == newList) {
            return true;
        } else if ((null == oldList) || (null == newList) ) {
            return false;
        } else {
            return oldList.size() == newList.size();
        }
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<UserAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            boolean isSame = false;
            if (null == oldItem.sender && null == newItem.sender) {
                isSame = true;
            } else if(null != oldItem.sender && null != newItem.sender) {
                isSame = StringUtil.isEquals(oldItem.sender.nickname, newItem.sender.nickname);
                if (isSame) {
                    isSame = StringUtil.isEquals(oldItem.sender.remark, newItem.sender.remark);
                }
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
            if (isSame && !isSameConfirms(oldItem.logs, newItem.logs)) {
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
