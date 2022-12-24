package io.taucoin.news.publishing.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.databinding.ItemDailyQuotaBinding;

/**
 * 每日数据流量定额Adapter
 */
public class DailyQuotaAdapter extends ListAdapter<Integer, DailyQuotaAdapter.ViewHolder> {
    private int selectPos;
    private OnCheckedChangeListener listener;
    DailyQuotaAdapter(OnCheckedChangeListener listener, int pos) {
        super(diffCallback);
        this.selectPos = pos;
        this.listener = listener;
    }

    public void updateSelectLimitPos(int pos) {
        this.selectPos = pos;
        notifyDataSetChanged();
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
        holder.bind(holder, getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemDailyQuotaBinding binding;
        private Context context;
        private DailyQuotaAdapter adapter;

        ViewHolder(ItemDailyQuotaBinding binding, DailyQuotaAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.adapter = adapter;
        }

        void bind(ViewHolder holder, Integer limit, int pos) {
            if(null == holder){
                return;
            }
            String limitStr;
            if (limit >= 1024) {
                limitStr = context.getString(R.string.setting_daily_quota_unit_g, limit / 1024);
            } else {
                limitStr = context.getString(R.string.setting_daily_quota_unit_m, limit);
            }
            holder.binding.tvData.setText(limitStr);
            boolean isSelected = adapter.selectPos == pos;
            holder.binding.ivSelector.setImageResource(isSelected ? R.mipmap.icon_radio_button_on :
                    R.mipmap.icon_radio_button_off);
            holder.binding.getRoot().setOnClickListener(v -> {
                if (!isSelected) {
                    if (adapter.listener != null) {
                        adapter.listener.onCheckedChanged(pos);
                    }
                    adapter.selectPos = pos;
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(int pos);
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
