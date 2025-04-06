package com.example.wtf2.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.ui.adapter.FoldersAdapter;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;

/**
 * Фрагмент для отображения списка папок.
 */
public class FoldersFragment extends Fragment {
    private static final String TAG = "FoldersFragment";
    private RecyclerView recyclerView;
    private FoldersAdapter folderAdapter;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders_list, container, false);
        Log.d(TAG, "Creating FoldersFragment view");

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Настройка RecyclerView
        recyclerView = view.findViewById(R.id.folders_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        folderAdapter = new FoldersAdapter(viewModel);
        recyclerView.setAdapter(folderAdapter);

        setupObservers();

        // Обновляем список папок при открытии фрагмента
        viewModel.loadFolders();
        Log.d(TAG, "Requested folders update on fragment creation");

        return view;
    }

    /**
     * Вызывается, когда фрагмент становится видимым.
     * Повторно обновляет список папок для актуальности данных.
     */
    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadFolders();
        Log.d(TAG, "Requested folders update on fragment resume");
    }

    /**
     * Настраивает наблюдателей за данными из ViewModel.
     */
    private void setupObservers() {
        // Наблюдение за списком папок
        viewModel.getFolders().observe(getViewLifecycleOwner(), folders -> {
            folderAdapter.setFolders(folders != null ? folders : new ArrayList<>());
            Log.d(TAG, "Folders updated: " + folders.size() + " items");
            // Уведомляем адаптер об изменении данных (можно заменить на DiffUtil для оптимизации)
            folderAdapter.notifyDataSetChanged();
        });

        // Наблюдение за режимом выбора
        viewModel.getIsSelectionMode().observe(getViewLifecycleOwner(), isSelectionMode -> {
            if (!isSelectionMode) {
                folderAdapter.notifyDataSetChanged();
                Log.d(TAG, "Selection mode exited, adapter refreshed");
            }
        });
    }

    /**
     * Возвращает адаптер для доступа к нему извне (например, для тестов).
     */
    public FoldersAdapter getFolderAdapter() {
        return folderAdapter;
    }
}