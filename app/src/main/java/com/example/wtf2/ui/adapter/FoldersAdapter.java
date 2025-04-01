package com.example.wtf2.ui.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.ui.main.MainActivity;
import com.example.wtf2.ui.main.NotesFragment;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {

    private List<Folder> folders = new ArrayList<>();
    private final MainViewModel viewModel;

    public FoldersAdapter(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
        Log.d("FoldersAdapter", "Setting folders: " + folders.size() + " items");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.bind(folder);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView folderNameTextView;
        private final TextView folderCountTextView;
        private final CheckBox checkBox;
        private final ImageView pinIcon;
        private final View container;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folder_name);
            folderCountTextView = itemView.findViewById(R.id.folder_count);
            checkBox = itemView.findViewById(R.id.folder_checkbox);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            container = itemView.findViewById(R.id.folder_container);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Folder folder = folders.get(position);
                    if (folder.getName().equals("Неотсортированные") && viewModel.getIsSelectionMode().getValue()) {
                        Toast.makeText(itemView.getContext(), "Папка 'Неотсортированные' защищена от выбора", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (viewModel.getIsSelectionMode().getValue()) {
                        viewModel.toggleFolderSelection(folder);
                        notifyItemChanged(position);
                        Log.d("FoldersAdapter", "Toggled selection for folder: " + folder.getName() + ", selected: " + viewModel.isFolderSelected(folder));
                    } else {
                        viewModel.setIsInFolder(true);
                        Bundle bundle = new Bundle();
                        bundle.putString("folder_name", folder.getName()); // Оставляем имя для отображения
                        NotesFragment notesFragment = new NotesFragment();
                        notesFragment.setArguments(bundle);
                        ((MainActivity) itemView.getContext()).getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, notesFragment)
                                .addToBackStack(null)
                                .commit();
                        Log.d("FoldersAdapter", "Navigated to notes in folder: " + folder.getName());
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Folder folder = folders.get(position);
                    if (folder.getName().equals("Неотсортированные")) {
                        Toast.makeText(itemView.getContext(), "Папка 'Неотсортированные' защищена от выбора", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (!viewModel.getIsSelectionMode().getValue()) {
                        viewModel.enterSelectionMode();
                        viewModel.toggleFolderSelection(folder);
                        notifyItemChanged(position);
                        Log.d("FoldersAdapter", "Entered selection mode, selected folder: " + folder.getName());
                        return true;
                    }
                }
                return false;
            });
        }

        public void bind(Folder folder) {
            folderNameTextView.setText(folder.getName());
            folderCountTextView.setText(String.valueOf(folder.getNoteCount()));
            boolean isSelectionMode = viewModel.getIsSelectionMode().getValue();
            checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(viewModel.isFolderSelected(folder));
            pinIcon.setVisibility(folder.isPinned() ? View.VISIBLE : View.GONE);

            int folderColor = android.graphics.Color.parseColor(folder.getColor());
            container.setBackgroundColor(folderColor);

            int colorFrom = viewModel.isFolderSelected(folder) ? folderColor : android.graphics.Color.parseColor("#E0F7FA");
            int colorTo = viewModel.isFolderSelected(folder) ? android.graphics.Color.parseColor("#E0F7FA") : folderColor;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animator -> container.setBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }
    }
}