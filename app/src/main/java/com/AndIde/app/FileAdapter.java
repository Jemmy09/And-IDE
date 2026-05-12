package com.AndIde.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<File> files;
    private OnFileClickListener listener;
    private OnFileLongClickListener longClickListener;
    private String activeFileName = "";

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    public interface OnFileLongClickListener {
        void onFileLongClick(File file);
    }

    public FileAdapter(List<File> files, OnFileClickListener listener, OnFileLongClickListener longClickListener) {
        this.files = new ArrayList<>(files);
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files.get(position);
        holder.tvFileName.setText(file.getName());
        
        boolean isActive = file.getName().equals(activeFileName);
        
        holder.tvFileName.setTextColor(isActive ? 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.pro_accent) : 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.pro_on_surface_dim));
        
        holder.ivFileIcon.setColorFilter(isActive ? 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.pro_accent) : 
                ContextCompat.getColor(holder.itemView.getContext(), R.color.pro_on_surface_dim));

        holder.itemView.setOnClickListener(v -> listener.onFileClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onFileLongClick(file);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void updateFiles(List<File> newFiles) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FileDiffCallback(this.files, newFiles));
        this.files = new ArrayList<>(newFiles);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setActiveFile(String fileName) {
        if (activeFileName.equals(fileName)) return;

        String oldActive = activeFileName;
        this.activeFileName = fileName;

        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            if (name.equals(oldActive) || name.equals(fileName)) {
                notifyItemChanged(i);
            }
        }
    }

    private static class FileDiffCallback extends DiffUtil.Callback {
        private final List<File> oldList;
        private final List<File> newList;

        FileDiffCallback(List<File> oldList, List<File> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getAbsolutePath()
                    .equals(newList.get(newItemPosition).getAbsolutePath());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            File oldFile = oldList.get(oldItemPosition);
            File newFile = newList.get(newItemPosition);
            return oldFile.getName().equals(newFile.getName()) &&
                   oldFile.length() == newFile.length() &&
                   oldFile.lastModified() == newFile.lastModified();
        }
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        ImageView ivFileIcon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }
    }
}
