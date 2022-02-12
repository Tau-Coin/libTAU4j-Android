package io.taucoin.torrent.publishing.ui.friends;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ItemDailyQuotaBinding;

/**
 * 每日数据流量定额Adapter
 */
public class ReferralBonusAdapter extends ListAdapter<Integer, ReferralBonusAdapter.ViewHolder> {
    private int[] bonuses;
    private int selected = 0;
    ReferralBonusAdapter(int[] bonuses) {
        super(diffCallback);
        this.bonuses = bonuses;
    }

    public int getSelected() {
        return bonuses[selected];
    }

    @Override
    public int getItemCount() {
        return null == bonuses ? 0 : bonuses.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDailyQuotaBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_daily_quota,
                parent,
                false);

        return new ViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemDailyQuotaBinding binding;
        private ReferralBonusAdapter adapter;

        ViewHolder(ItemDailyQuotaBinding binding, ReferralBonusAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;
        }

        void bind(ViewHolder holder, int pos) {
            if (null == holder) {
                return;
            }
            Resources resources = binding.getRoot().getContext().getResources();
            holder.binding.radioButton.setText(resources.getString(R.string.common_percent,
                    adapter.bonuses[pos]));
            holder.binding.radioButton.setChecked(adapter.selected == pos);
            holder.binding.radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    adapter.selected = pos;
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Integer> diffCallback = new DiffUtil.ItemCallback<Integer>() {
        @Override
        public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }
    };
}
