package com.AndIde.app;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_DDE = 2;
    
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_DDE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_dde, parent, false);
            return new DDeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(message.getText());
        } else if (holder instanceof DDeViewHolder) {
            DDeViewHolder ddeHolder = (DDeViewHolder) holder;
            if (message.isImage()) {
                ddeHolder.tvMessage.setVisibility(View.GONE);
                ddeHolder.cardImage.setVisibility(View.VISIBLE);
                
                // Load image using Glide
                com.bumptech.glide.Glide.with(ddeHolder.ivGeneratedImage.getContext())
                        .load(message.getImageUrl())
                        .into(ddeHolder.ivGeneratedImage);

            } else {
                ddeHolder.tvMessage.setVisibility(View.VISIBLE);
                ddeHolder.cardImage.setVisibility(View.GONE);
                ddeHolder.tvMessage.setText(message.getText());

                // Allow long press to copy AI reply
                ddeHolder.tvMessage.setOnLongClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("AI Reply", message.getText());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(v.getContext(), "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatDiffCallback(this.messages, newMessages));
        this.messages = new ArrayList<>(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class ChatDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        ChatDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
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
            // Chat messages don't have IDs, so we compare content and position-based identity
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }
    }

    static class DDeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View cardImage;
        android.widget.ImageView ivGeneratedImage;

        DDeViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvDDeMessage);
            cardImage = itemView.findViewById(R.id.cardImage);
            ivGeneratedImage = itemView.findViewById(R.id.ivGeneratedImage);
        }
    }
}
