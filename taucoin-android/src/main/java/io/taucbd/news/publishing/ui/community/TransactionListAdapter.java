package io.taucbd.news.publishing.ui.community;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ItemTransactionListBinding;

/**
 * 社区同步列表的Adapter
 */
public class TransactionListAdapter extends ListAdapter<IncomeAndExpenditure, TransactionListAdapter.ViewHolder> {

    private static final Logger logger = LoggerFactory.getLogger("TransactionListAdapter");
    private ClickListener listener;
    public TransactionListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTransactionListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_transaction_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionListAdapter.ViewHolder holder, int position) {
        IncomeAndExpenditure tx = getItem(position);
        holder.bindTransaction(tx);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionListBinding binding;
        private final Resources resources;
        private final String confirmRate;
        private final String pendingText;
        private final String myPublicKey;
        private final ClickListener listener;

        ViewHolder(ItemTransactionListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            Context context = binding.getRoot().getContext();
            resources = context.getResources();
            confirmRate = resources.getString(R.string.community_tx_confidence);
            pendingText = resources.getString(R.string.community_tx_pending);
            myPublicKey = MainApplication.getInstance().getPublicKey();
        }

        void bindTransaction(IncomeAndExpenditure entry) {
            if (null == entry) {
                return;
            }
            boolean isMyself = StringUtil.isEquals(entry.senderOrMiner, myPublicKey);
            StringBuilder stringBuilder = new StringBuilder();
            if (isMyself) {
                //txType == -1是mining rewards
                if (entry.txType == -1) {
                    binding.tvName.setText(resources.getString(R.string.community_mining_rewards));
                    binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
                    stringBuilder.append("+");
                    stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount));
                    binding.tvAmount.setTextColor(resources.getColor(R.color.color_yellow));
                } else {
                    if (entry.txType == TxType.NEWS_TX.getType()) {
                        binding.tvName.setText(resources.getString(R.string.community_post_news));
                        binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
                        stringBuilder.append("-");
                        stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.fee));
                        binding.tvAmount.setTextColor(resources.getColor(R.color.color_red));
                    } else if (entry.txType == TxType.WIRING_TX.getType()){
                        boolean isSendToSelf = StringUtil.isEquals(entry.senderOrMiner, entry.receiverPk);
                        //自己发给自己的交易, 显示交易费
                        if (isSendToSelf) {
                            binding.tvName.setText(resources.getString(R.string.community_transfer_to_self));
                            stringBuilder.append("-");
                            stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.fee));
                            binding.tvAmount.setTextColor(resources.getColor(R.color.color_red));
                        } else {
                            String name = UsersUtil.getShowName(entry.receiver);
                            binding.tvName.setText(resources.getString(R.string.community_transfer_to, name));
                            binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.receiver));
                            stringBuilder.append("-");
                            stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount + entry.fee));
                            binding.tvAmount.setTextColor(resources.getColor(R.color.color_red));
                        }
                    }
                }
            } else {
                binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(entry.sender));
                String name = UsersUtil.getShowName(entry.sender);
                binding.tvName.setText(resources.getString(R.string.community_transfer_from, name));
                stringBuilder.append("+");
                stringBuilder.append(FmtMicrometer.fmtMiningIncome(entry.amount));
                binding.tvAmount.setTextColor(resources.getColor(R.color.color_yellow));
                logger.debug("wallet tx list received coins txid::{}", entry.hash);
            }
            binding.tvAmount.setText(stringBuilder.toString());
            if (entry.onlineStatus >= Constants.TX_STATUS_ON_CHAIN) {
                binding.tvConfirmRate.setVisibility(View.VISIBLE);
                long currentTime = DateUtil.getTime();
				//注意：state block之前的情况
                double rate = (currentTime - entry.onlineTime) * 1.0f / 60 / 180;
                rate = Math.min(1, rate);
                rate = Math.max(0, rate);
                rate = Math.sqrt(rate) * 100;
                rate = Math.max(1, rate);  // 保证从最终从1%开始
                String confirmRateStr = String.format(confirmRate, (int)rate) + "%";
                binding.tvConfirmRate.setText(Html.fromHtml(confirmRateStr));
                binding.tvConfirmRate.setTextColor(resources.getColor(R.color.gray_dark));
            } else {
                binding.tvConfirmRate.setText(pendingText);
                binding.tvConfirmRate.setTextColor(resources.getColor(R.color.color_blue_dark));
            }
            binding.tvTime.setText(DateUtil.formatTime(entry.createTime, DateUtil.pattern13));

            binding.getRoot().setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemClicked(entry);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(IncomeAndExpenditure entry);
    }

    private static final DiffUtil.ItemCallback<IncomeAndExpenditure> diffCallback = new DiffUtil.ItemCallback<IncomeAndExpenditure>() {
        @Override
        public boolean areContentsTheSame(@NonNull IncomeAndExpenditure oldItem, @NonNull IncomeAndExpenditure newItem) {
            return oldItem.equals(newItem) && oldItem.onlineStatus == newItem.onlineStatus;
        }

        @Override
        public boolean areItemsTheSame(@NonNull IncomeAndExpenditure oldItem, @NonNull IncomeAndExpenditure newItem) {
            return oldItem.equals(newItem);
        }
    };
}
