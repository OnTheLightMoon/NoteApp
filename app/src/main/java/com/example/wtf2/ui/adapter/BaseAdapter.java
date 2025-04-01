package com.example.wtf2.ui.adapter;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<T> items;
    protected List<T> selectedItems = new ArrayList<>();
    protected boolean isSelectionMode = false;

    public BaseAdapter(List<T> items) {
        this.items = items;
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(T item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyItemChanged(items.indexOf(item));
    }

    public List<T> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }
}