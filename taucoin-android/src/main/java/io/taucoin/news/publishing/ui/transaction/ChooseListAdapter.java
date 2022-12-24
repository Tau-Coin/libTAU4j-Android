package io.taucoin.news.publishing.ui.transaction;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.databinding.ItemCoinChooseBinding;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<String, ChooseListAdapter.ViewHolder> {
    private String coin;
    ChooseListAdapter(String coin) {
        super(diffCallback);
        this.coin = coin;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCoinChooseBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_coin_choose,
                parent,
                false);

        return new ViewHolder(binding, coin);
    }

    @Override
    public void onBindViewHolder(@NonNull ChooseListAdapter.ViewHolder holder, int position) {
        String coin = getItem(position);
        holder.bindCoin(coin);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCoinChooseBinding binding;
        private String coin;

        ViewHolder(ItemCoinChooseBinding binding, String coin) {
            super(binding.getRoot());
            this.binding = binding;
            this.coin = coin;
        }

        /**
         * 绑定社区数据
         */
        void bindCoin(String coin) {
            binding.tvName.setText(coin);
            boolean isChoose = StringUtil.isEquals(this.coin, coin);
            int colorRes = binding.getRoot().getResources().getColor(isChoose ? R.color.color_yellow :
                    R.color.color_black);
            binding.tvName.setTextColor(colorRes);
        }
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
