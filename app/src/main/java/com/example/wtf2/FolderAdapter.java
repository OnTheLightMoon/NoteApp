package com.example.wtf2;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    private List<Folder> folders;
    private List<Folder> selectedFolders = new ArrayList<>();
    private boolean isSelectionMode = false;
    private OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
        void onFolderLongClick(Folder folder);
        void onSelectionChanged();
    }

    public FolderAdapter(List<Folder> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.folderName.setText(folder.getName());
        holder.folderCount.setText(String.valueOf(folder.getNoteCount()));
        holder.pinIcon.setVisibility(folder.isPinned() ? View.VISIBLE : View.GONE);

        // Устанавливаем базовый цвет папки
        GradientDrawable baseBackground = new GradientDrawable();
        baseBackground.setColor(Color.parseColor(folder.getColor()));
        baseBackground.setCornerRadius(8f);

        // Усиливаем эффект выделения
        if (selectedFolders.contains(folder)) {
            GradientDrawable selectedBackground = new GradientDrawable();
            selectedBackground.setColor(Color.parseColor("#808080")); // Тёмно-серый фон
            selectedBackground.setStroke(2, Color.BLACK); // Чёрная обводка
            selectedBackground.setCornerRadius(8f);
            holder.container.setBackground(selectedBackground); // Применяем к container
            holder.container.setAlpha(0.9f); // Лёгкая полупрозрачность
        } else {
            holder.container.setBackground(baseBackground); // Обычный цвет папки
            holder.container.setAlpha(1.0f); // Полная яркость
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(folder);
                notifyItemChanged(position); // Обновляем только этот элемент
                if (listener != null) listener.onSelectionChanged();
            } else if (listener != null) {
                listener.onFolderClick(folder);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && !isSelectionMode) {
                listener.onFolderLongClick(folder);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedFolders.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(Folder folder) {
        if (selectedFolders.contains(folder)) {
            selectedFolders.remove(folder);
        } else {
            selectedFolders.add(folder);
        }
    }

    public List<Folder> getSelectedFolders() {
        return new ArrayList<>(selectedFolders);
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, folderCount;
        ImageView pinIcon;
        LinearLayout container;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folder_name);
            folderCount = itemView.findViewById(R.id.folder_count);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            container = itemView.findViewById(R.id.folder_container);
        }
    }
}