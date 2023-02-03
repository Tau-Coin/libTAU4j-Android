package io.taucbd.news.publishing.ui.setting;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.HashUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.databinding.ItemBlacklistBinding;
import io.taucbd.news.publishing.databinding.ItemUserBlacklistBinding;
import io.taucbd.news.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class BlackListAdapter extends ListAdapter<Parcelable, BlackListAdapter.ViewHolder>
        implements Selectable<Parcelable> {
    private List<Parcelable> dataList = new ArrayList<>();
    private String type;
    private ClickListener listener;

    BlackListAdapter(ClickListener listener, String type) {
        super(diffCallback);
        this.listener = listener;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if (StringUtil.isNotEquals(type, BlacklistActivity.TYPE_COMMUNITIES)) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_user_blacklist,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_blacklist,
                    parent,
                    false);
        }

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull BlackListAdapter.ViewHolder holder, int position) {
        Parcelable parcelable = getItemKey(position);
        if(parcelable instanceof Community){
            holder.bindCommunity((Community)parcelable, getItemCount() != position + 1, position);
        }else if(parcelable instanceof User){
            holder.bindUser((User)parcelable, getItemCount() != position + 1, position);
        }
    }

    @Override
    public Parcelable getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Parcelable key) {
        return getCurrentList().indexOf(key);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /**
     * 设置社区列表列表数据
     * @param dataList 社区数据
     */
    void setCommunityList(List<Community> dataList) {
        this.dataList.clear();
        if(dataList != null){
            this.dataList.addAll(dataList);
        }
        notifyDataSetChanged();
    }

    /**
     * 设置用户列表列表数据
     * @param dataList 用户数据
     */
    void setUserList(List<User> dataList) {
        this.dataList.clear();
        if(dataList != null){
            this.dataList.addAll(dataList);
        }
        notifyDataSetChanged();
    }

    /**
     * 删除列表Item
     * @param pos 位置索引
     */
    void deleteItem(int pos) {
        if(pos >= 0 && pos < dataList.size()){
            dataList.remove(pos);
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(Community community, boolean isShowLine, int position) {
            if(null == community){
                return;
            }
            ItemBlacklistBinding communityBinding = (ItemBlacklistBinding) binding;
            communityBinding.tvName.setText(community.communityName);
            String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
            communityBinding.leftView.setText(firstLetters);
            communityBinding.leftView.setBgColor(Utils.getGroupColor(firstLetters));
            communityBinding.line.setVisibility(isShowLine ? View.VISIBLE : View.GONE);
            communityBinding.tvUnblock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblock(position);
                }
            });
        }

        /**
         * 绑定用户数据
         */
        void bindUser(User user, boolean isShowLine, int position) {
            if(null == user){
                return;
            }
            ItemUserBlacklistBinding userBinding = (ItemUserBlacklistBinding) binding;

            String showName = UsersUtil.getShowName(user);
            userBinding.leftView.setImageBitmap(UsersUtil.getHeadPic(user));

            userBinding.tvName.setText(showName);
            String publicKey = HashUtil.hashMiddleHide(user.publicKey);
            publicKey = binding.getRoot().getResources().getString(R.string.common_parentheses, publicKey);
            userBinding.tvPublicKey.setText(publicKey);
            userBinding.line.setVisibility(isShowLine ? View.VISIBLE : View.GONE);
            userBinding.tvUnblock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblock(position);
                }
            });
        }
    }

    public interface ClickListener {
        void onUnblock(int pos);
    }

    private static final DiffUtil.ItemCallback<Parcelable> diffCallback = new DiffUtil.ItemCallback<Parcelable>() {
        @Override
        public boolean areContentsTheSame(@NonNull Parcelable oldItem, @NonNull Parcelable newItem) {
            if(oldItem instanceof Community){
                return ((Community)oldItem).equals(newItem);
            }else if(oldItem instanceof User){
                return ((User)oldItem).equals(newItem);
            }
            return true;
        }

        @Override
        public boolean areItemsTheSame(@NonNull Parcelable oldItem, @NonNull Parcelable newItem) {
            if(oldItem instanceof Community){
                return ((Community)oldItem).equals(newItem);
            }else if(oldItem instanceof User){
                return ((User)oldItem).equals(newItem);
            }
            return oldItem.equals(newItem);
        }
    };
}
