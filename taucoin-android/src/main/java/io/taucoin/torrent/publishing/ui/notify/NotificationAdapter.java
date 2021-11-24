package io.taucoin.torrent.publishing.ui.notify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemNotifyListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class NotificationAdapter extends ListAdapter<NotificationAndUser,
        NotificationAdapter.ViewHolder> {
    private List<NotificationAndUser> selectedList = new ArrayList<>();

    NotificationAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNotifyListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_notify_list,
                parent,
                false);

        return new ViewHolder(binding, selectedList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    List<NotificationAndUser> getSelectedList() {
        return selectedList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemNotifyListBinding binding;
        private Context context;
        private List<NotificationAndUser> selectedList;

        ViewHolder(ItemNotifyListBinding binding, List<NotificationAndUser> selectedList) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.selectedList = selectedList;
        }

        void bind(ViewHolder holder, NotificationAndUser notify) {
            if(null == holder || null == notify){
                return;
            }
            holder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedList.remove(notify);
                if(isChecked){
                    selectedList.add(notify);
                }
            });
            holder.binding.cbSelect.setChecked(selectedList.contains(notify));
            String showName = UsersUtil.getUserName(notify.user, notify.senderPk);
            showName = context.getString(R.string.notifications_from, showName);
            holder.binding.tvUserName.setText(showName);

            String communityName = ChainIDUtil.getName(notify.chainID);
            holder.binding.tvCommunityName.setText(communityName);

            String time = DateUtil.formatTime(notify.timestamp, DateUtil.pattern6);
            holder.binding.tvTime.setText(communityName);

            String firstLetters = StringUtil.getFirstLettersOfName(communityName);
            holder.binding.leftView.setText(firstLetters);
            holder.binding.leftView.setBgColor(Utils.getGroupColor(notify.senderPk));
        }
    }

    private static final DiffUtil.ItemCallback<NotificationAndUser> diffCallback = new DiffUtil.ItemCallback<NotificationAndUser>() {
        @Override
        public boolean areContentsTheSame(@NonNull NotificationAndUser oldItem, @NonNull NotificationAndUser newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull NotificationAndUser oldItem, @NonNull NotificationAndUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
