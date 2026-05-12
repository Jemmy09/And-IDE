package com.AndIde.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {

    private List<Template> templates;
    private OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }

    public TemplateAdapter(List<Template> templates, OnTemplateClickListener listener) {
        this.templates = new ArrayList<>(templates);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = templates.get(position);
        holder.tvName.setText(template.getName());
        holder.tvDesc.setText(template.getDescription());
        holder.btnUse.setOnClickListener(v -> listener.onTemplateClick(template));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public void updateTemplates(List<Template> newTemplates) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TemplateDiffCallback(this.templates, newTemplates));
        this.templates = new ArrayList<>(newTemplates);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class TemplateDiffCallback extends DiffUtil.Callback {
        private final List<Template> oldList;
        private final List<Template> newList;

        TemplateDiffCallback(List<Template> oldList, List<Template> newList) {
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
            return oldList.get(oldItemPosition).getName().equals(newList.get(newItemPosition).getName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        Button btnUse;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTemplateName);
            tvDesc = itemView.findViewById(R.id.tvTemplateDesc);
            btnUse = itemView.findViewById(R.id.btnUseTemplate);
        }
    }
}