package com.example.wtf2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<Folder> folders;
    private OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
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
        holder.folderTitle.setText(folder.getName());
        holder.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderTitle;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderTitle = itemView.findViewById(R.id.folder_title);
        }
    }


}
