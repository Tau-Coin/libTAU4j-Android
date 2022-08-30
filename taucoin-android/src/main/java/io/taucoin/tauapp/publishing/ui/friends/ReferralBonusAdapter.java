package io.taucoin.tauapp.publishing.ui.friends;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.databinding.ItemDailyQuotaBinding;

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
            holder.binding.tvData.setText(resources.getString(R.string.common_percent,
                    adapter.bonuses[pos]));
            boolean isSelected = adapter.selected == pos;
            holder.binding.ivSelector.setImageResource(isSelected ? R.mipmap.icon_radio_button_on :
                    R.mipmap.icon_radio_button_off);
            holder.binding.getRoot().setOnClickListener(v -> {
                if (!isSelected) {
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
