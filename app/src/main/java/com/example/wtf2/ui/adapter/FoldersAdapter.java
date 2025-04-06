package com.example.wtf2.ui.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.ui.main.MainActivity;
import com.example.wtf2.ui.main.NotesFragment;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения списка папок в RecyclerView.
 * Поддерживает анимации, выбор элементов и переход в папку.
 */
public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {
    private static final String TAG = "FoldersAdapter";
    private static final String PROTECTED_FOLDER_NAME = "Неотсортированные";
    private List<Folder> folders = new ArrayList<>();
    private final MainViewModel viewModel;

    public FoldersAdapter(MainViewModel viewModel) {
        this.viewModel = viewModel;
        Log.d(TAG, "FoldersAdapter initialized");
    }

    /**
     * Устанавливает новый список папок с использованием DiffUtil.
     */
    public void setFolders(List<Folder> folders) {
        if (folders == null) folders = new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FolderDiffCallback(this.folders, folders));
        this.folders = new ArrayList<>(folders);
        diffResult.dispatchUpdatesTo(this);
        Log.d(TAG, "Set folders: " + folders.size() + " items");
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
        holder.bind(folder);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    /**
     * ViewHolder для элемента папки.
     */
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

            setupClickListeners();
        }

        /**
         * Настраивает обработчики кликов для элемента.
         */
        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Folder folder = folders.get(position);
                    handleItemClick(folder, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Folder folder = folders.get(position);
                    return handleItemLongClick(folder, position);
                }
                return false;
            });
        }

        /**
         * Обрабатывает клик по элементу.
         */
        private void handleItemClick(Folder folder, int position) {
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            if (isProtectedFolder(folder) && isSelectionMode) {
                showProtectedFolderToast();
                return;
            }
            if (isSelectionMode) {
                viewModel.toggleFolderSelection(folder);
                notifyItemChanged(position);
                Log.d(TAG, "Toggled selection for folder: " + folder.getName());
            } else {
                viewModel.setIsInFolder(true);
                viewModel.setCurrentFolderId(folder.getId());
                NotesFragment notesFragment = new NotesFragment();
                Bundle bundle = new Bundle();
                bundle.putString("folder_name", folder.getName());
                notesFragment.setArguments(bundle);
                // TODO: Рекомендуется вынести в MainActivity через callback
                ((MainActivity) itemView.getContext()).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, notesFragment)
                        .addToBackStack(null)
                        .commit();
                Log.d(TAG, "Navigated to notes in folder: " + folder.getName());
            }
        }

        /**
         * Обрабатывает долгий клик по элементу.
         */
        private boolean handleItemLongClick(Folder folder, int position) {
            if (isProtectedFolder(folder)) {
                showProtectedFolderToast();
                return false;
            }
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            if (!isSelectionMode) {
                viewModel.enterSelectionMode();
                viewModel.toggleFolderSelection(folder);
                notifyItemChanged(position);
                Log.d(TAG, "Entered selection mode, selected folder: " + folder.getName());
                return true;
            }
            return false;
        }

        /**
         * Привязывает данные папки к UI-элементам.
         */
        public void bind(Folder folder) {
            folderNameTextView.setText(folder.getName());
            folderCountTextView.setText(String.valueOf(folder.getNoteCount()));
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(viewModel.isFolderSelected(folder));
            pinIcon.setVisibility(folder.isPinned() ? View.VISIBLE : View.GONE);

            int folderColor = Color.parseColor(folder.getColor());
            animateBackground(folder, folderColor);
        }

        /**
         * Анимирует изменение фона элемента при выборе/снятии выбора.
         */
        private void animateBackground(Folder folder, int folderColor) {
            int colorFrom = viewModel.isFolderSelected(folder) ? folderColor : Color.parseColor("#E0F7FA");
            int colorTo = viewModel.isFolderSelected(folder) ? Color.parseColor("#E0F7FA") : folderColor;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animator -> container.setBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }

        /**
         * Проверяет, является ли папка защищенной ("Неотсортированные").
         */
        private boolean isProtectedFolder(Folder folder) {
            return folder.getName().equals(PROTECTED_FOLDER_NAME);
        }

        /**
         * Показывает сообщение о защищенной папке.
         */
        private void showProtectedFolderToast() {
            Toast.makeText(itemView.getContext(), "Папка '" + PROTECTED_FOLDER_NAME + "' защищена от выбора", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback для DiffUtil, сравнивает старые и новые папки.
     */
    private static class FolderDiffCallback extends DiffUtil.Callback {
        private final List<Folder> oldFolders;
        private final List<Folder> newFolders;

        public FolderDiffCallback(List<Folder> oldFolders, List<Folder> newFolders) {
            this.oldFolders = oldFolders;
            this.newFolders = newFolders;
        }

        @Override
        public int getOldListSize() { return oldFolders.size(); }
        @Override
        public int getNewListSize() { return newFolders.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldFolders.get(oldItemPosition).getId() == newFolders.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Folder oldFolder = oldFolders.get(oldItemPosition);
            Folder newFolder = newFolders.get(newItemPosition);
            return oldFolder.getName().equals(newFolder.getName()) &&
                    oldFolder.getColor().equals(newFolder.getColor()) &&
                    oldFolder.getNoteCount() == newFolder.getNoteCount() &&
                    oldFolder.isPinned() == newFolder.isPinned();
        }
    }
}