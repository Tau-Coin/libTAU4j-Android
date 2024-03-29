package io.taucbd.news.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.databinding.ItemPopUpCommunityDialogBinding;
import io.taucbd.news.publishing.databinding.PopUpCommunityDialogBinding;
import io.taucbd.news.publishing.databinding.PopUpDialogBinding;
import io.taucbd.news.publishing.ui.Selectable;

/**
 * 弹出对话框
 */
public class CommunitiesPopUpDialog extends Dialog{

    private CommunitiesPopUpDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public static class Builder{
        private Context context;
        private List<Member> items = new ArrayList<>();
        private OnItemClickListener listener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addItems(List<Member> items) {
            this.items.addAll(items);
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
            return this;
        }

        public CommunitiesPopUpDialog create(){
            CommunitiesPopUpDialog popUpDialog = new CommunitiesPopUpDialog(context, R.style.PopUpDialog);
            PopUpCommunityDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.pop_up_community_dialog, null, false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            binding.itemRecyclerView.setLayoutManager(layoutManager);
            ItemAdapter itemAdapter = new ItemAdapter(popUpDialog, items, listener);
            binding.itemRecyclerView.setAdapter(itemAdapter);
            View root = binding.getRoot();
            popUpDialog.setContentView(root);
            Window dialogWindow = popUpDialog.getWindow();
            if(dialogWindow != null){
                dialogWindow.setGravity(Gravity.BOTTOM);
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                dialogWindow.setAttributes(lp);
            }
            binding.ivClose.setOnClickListener(v -> {
                popUpDialog.closeDialog();
            });
            return popUpDialog;
        }
    }

    static class ItemAdapter extends ListAdapter<Member, ItemAdapter.ViewHolder> implements Selectable<Member> {
        private List<Member> items;
        private OnItemClickListener listener;
        private CommunitiesPopUpDialog popUpDialog;
        private final String nameFormat;
        ItemAdapter(CommunitiesPopUpDialog popUpDialog, List<Member> items, OnItemClickListener listener) {
            super(diffCallback);
            this.popUpDialog = popUpDialog;
            this.items = items;
            this.listener = listener;
            this.nameFormat = popUpDialog.getContext().getString(R.string.main_community_name_balance);
        }

        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemPopUpCommunityDialogBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_pop_up_community_dialog,
                    parent,
                    false);
            return new ViewHolder(binding.getRoot(), binding);
        }

        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Member member = getItemKey(position);
            if(position >= 0 && position < items.size() && holder.binding != null){
                String communityName = ChainIDUtil.getName(member.chainID);
                String firstLetters = StringUtil.getFirstLettersOfName(communityName);
                holder.binding.rbCommunity.setText(firstLetters);
                int bgColor = Utils.getGroupColor(member.chainID);
                holder.binding.rbCommunity.setBgColor(bgColor);

                int configChainLogo = Utils.getConfigChainLogo(member.chainID);
                boolean isConfigChain = configChainLogo != 0;
                holder.binding.rbCommunity.setVisibility(isConfigChain ? View.GONE : View.VISIBLE);
                holder.binding.ivCommunity.setVisibility(isConfigChain ? View.VISIBLE : View.GONE);
                if (isConfigChain) {
                    holder.binding.ivCommunity.setImageRes(configChainLogo);
                }
                String communityCode = ChainIDUtil.getCode(member.chainID);
                String balance = FmtMicrometer.fmtBalance(member.getInterimBalance());
                String name = String.format(nameFormat, communityName, communityCode, balance);
                holder.binding.tvName.setText(Html.fromHtml(name));
                holder.itemView.setOnClickListener(v -> {
                    if(listener != null){
                        listener.onItemClick(popUpDialog, member);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        @Override
        public Member getItemKey(int position) {
            if(position >=0 && position < items.size()){
                return items.get(position);
            }
            return null;
        }

        @Override
        public int getItemPosition(Member key) {
            return getCurrentList().indexOf(key);
        }

        static class ViewHolder extends RecyclerView.ViewHolder{
            ItemPopUpCommunityDialogBinding binding;
            ViewHolder(@NonNull View itemView, ItemPopUpCommunityDialogBinding binding) {
                super(itemView);
                this.binding = binding;
            }
        }

        private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
                return oldItem.balance == newItem.balance;
            }

            @Override
            public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
                return oldItem.equals(newItem);
            }
        };
    }


    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }

    public interface OnItemClickListener{
        void onItemClick(Dialog dialog, Member member);
    }
}
