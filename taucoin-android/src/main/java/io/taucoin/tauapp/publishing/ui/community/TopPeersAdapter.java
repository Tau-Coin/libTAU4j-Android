package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ItemChainTopBinding;

/**
 * 社区排名的Adapter
 */
public class TopPeersAdapter extends ListAdapter<Member, TopPeersAdapter.ViewHolder> {
    private final ClickListener listener;

    TopPeersAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChainTopBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_chain_top,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChainTopBinding binding;
        private final ClickListener listener;
        private Context appContext;

        ViewHolder(ItemChainTopBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.appContext = binding.getRoot().getContext().getApplicationContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, Member member, int pos) {
            if(null == holder || null == member){
                return;
            }
            holder.binding.tvCol1.setText(String.valueOf(pos + 1));
            String midHideName = UsersUtil.getMidHideName(member.publicKey, 6);
            holder.binding.tvCol2.setText(midHideName);
            holder.binding.tvCol2.setTextColor(binding.getRoot().getResources().getColor(R.color.color_yellow));

            String balance = FmtMicrometer.fmtBalance(member.balance);
            String time = DateUtil.formatTime(member.balUpdateTime, DateUtil.pattern14);
            String balanceAndTime = appContext.getResources().getString(R.string.drawer_balance_time_no_title,
                    balance, time);
            holder.binding.tvCol3.setText(balanceAndTime);

            ViewUtils.updateViewWeight(binding.tvCol2, 3);
            ViewUtils.updateViewWeight(binding.tvCol3, 4);
            holder.binding.tvCol2.setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(member.publicKey);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(String publicKey);
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
