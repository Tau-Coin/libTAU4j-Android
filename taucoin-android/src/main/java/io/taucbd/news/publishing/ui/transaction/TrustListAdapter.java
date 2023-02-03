package io.taucbd.news.publishing.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.databinding.ItemTrustHashBinding;

/**
 * 用户Trust列表的Adapter
 */
public class TrustListAdapter extends ListAdapter<Tx, TrustListAdapter.ViewHolder> {

    TrustListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTrustHashBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_trust_hash,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemTrustHashBinding binding;
        private Context context;

        ViewHolder(ItemTrustHashBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
        }

        void bind(ViewHolder holder, Tx tx) {
            if(null == holder || null == tx){
                return;
            }
            holder.binding.tvHash.setText(tx.txID);
        }
    }

    private static final DiffUtil.ItemCallback<Tx> diffCallback = new DiffUtil.ItemCallback<Tx>() {
        @Override
        public boolean areContentsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
