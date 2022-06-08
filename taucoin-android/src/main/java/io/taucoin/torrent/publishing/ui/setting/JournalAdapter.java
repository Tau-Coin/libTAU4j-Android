package io.taucoin.torrent.publishing.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemJournalListBinding;

/**
 * 日志列表的Adapter
 */
public class JournalAdapter extends ListAdapter<JournalAdapter.FileInfo, JournalAdapter.ViewHolder> {
    private ClickListener listener;

    JournalAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemJournalListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_journal_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), position != getItemCount() - 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemJournalListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemJournalListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, JournalAdapter.FileInfo file, boolean isShowLine) {
            if(null == holder || null == file){
                return;
            }
            binding.tvFileName.setText(file.getFileName());
            String fileSize = Formatter.formatFileSize(context, file.getFileSize());
            binding.tvFileSize.setText(fileSize.toUpperCase());

            binding.lineView.setVisibility(isShowLine ? View.VISIBLE : View.INVISIBLE);

            binding.ivShare.setOnClickListener(v -> {
                if(listener != null){
                    listener.onShareClicked(file.getFileName());
                }
            });
        }
    }

    public interface ClickListener {
        void onShareClicked(String fileName);
    }

    private static final DiffUtil.ItemCallback<JournalAdapter.FileInfo> diffCallback = new DiffUtil.ItemCallback<JournalAdapter.FileInfo>() {
        @Override
        public boolean areContentsTheSame(@NonNull JournalAdapter.FileInfo oldItem, @NonNull JournalAdapter.FileInfo newItem) {
            return StringUtil.isEquals(oldItem.getFileName(), newItem.getFileName())
                    && oldItem.getFileSize() == newItem.getFileSize();
        }

        @Override
        public boolean areItemsTheSame(@NonNull JournalAdapter.FileInfo oldItem, @NonNull JournalAdapter.FileInfo newItem) {
            return StringUtil.isEquals(oldItem.getFileName(), newItem.getFileName());
        }
    };

    static class FileInfo {
        private String fileName;
        private long fileSize;

        public FileInfo(String name, long size) {
            this.fileName = name;
            this.fileSize = size;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
