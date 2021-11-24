package io.taucoin.torrent.publishing.ui.community;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemFilterChooseBinding;

/**
 * 过滤选择列表的Adapter
 */
public class FilterListAdapter extends ListAdapter<String, FilterListAdapter.ViewHolder> {
    private Set<String> selectedSet;
    private Callback callback;
    FilterListAdapter(Callback callback, Set<String> selectedSet) {
        super(diffCallback);
        this.callback = callback;
        this.selectedSet = selectedSet;
    }

    Set<String> getSelectedSet() {
        return selectedSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFilterChooseBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_filter_choose,
                parent,
                false);

        return new ViewHolder(binding, getCurrentList(), selectedSet, callback);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterListAdapter.ViewHolder holder, int position) {
        String filter = getItem(position);
        holder.bindFilter(filter);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFilterChooseBinding binding;
        private List<String> currentList;
        private Set<String> selectedSet;
        private Callback callback;

        ViewHolder(ItemFilterChooseBinding binding, List<String> currentList, Set<String> selectedSet,
                   Callback callback) {
            super(binding.getRoot());
            this.binding = binding;
            this.currentList = currentList;
            this.selectedSet = selectedSet;
            this.callback = callback;
        }

        void bindFilter(String filter) {
            int index = StringUtil.getIntString(filter);
            int nameRes = CommunityTabs.getNameByIndex(index);
            binding.cbSelect.setText(binding.getRoot().getResources().getString(nameRes));
            binding.cbSelect.setChecked(selectedSet.contains(filter));
            binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSet.add(filter);
                } else {
                    selectedSet.remove(filter);
                }
                if (callback != null) {
                    callback.onCheckedChangeListener();
                }
            });
        }
    }

    interface Callback {
        void onCheckedChangeListener();
    }

    private static final DiffUtil.ItemCallback<String> diffCallback = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };
}
