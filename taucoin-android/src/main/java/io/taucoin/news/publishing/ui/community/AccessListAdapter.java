package io.taucoin.news.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ItemAccessListBinding;

/**
 * 社区同步列表的Adapter
 */
public class AccessListAdapter extends ListAdapter<String, AccessListAdapter.ViewHolder> {
    private ClickListener listener;
    public AccessListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAccessListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_access_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull AccessListAdapter.ViewHolder holder, int position) {
        String key = getItem(position);
        holder.bindKey(key);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAccessListBinding binding;
        private ClickListener listener;

        ViewHolder(ItemAccessListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bindKey(String key) {
            binding.tvKey.setText(UsersUtil.getMidHideName(key));
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClicked(key);
                }
            });
        }
    }

    public interface ClickListener {
        void onClicked(String key);
    }

    private static final DiffUtil.ItemCallback<String> diffCallback = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return StringUtil.isEquals(oldItem, newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return StringUtil.isEquals(oldItem, newItem);
        }
    };
}
