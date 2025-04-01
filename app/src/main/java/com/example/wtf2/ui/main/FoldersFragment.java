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
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.ui.adapter.FoldersAdapter;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;

public class FoldersFragment extends Fragment {

    private RecyclerView recyclerView;
    private FoldersAdapter folderAdapter;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders_list, container, false);

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = view.findViewById(R.id.folders_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderAdapter = new FoldersAdapter(viewModel);
        recyclerView.setAdapter(folderAdapter);

        // Загружаем папки при создании фрагмента
        viewModel.loadFolders();

        // Наблюдение за списком папок
        viewModel.getFolders().observe(getViewLifecycleOwner(), folders -> {
            folderAdapter.setFolders(folders != null ? folders : new ArrayList<>());
            Log.d("FoldersFragment", "Updated folders list with " + folders.size() + " items");
        });

        // Наблюдение за режимом множественного выбора
        viewModel.getIsSelectionMode().observe(getViewLifecycleOwner(), isSelectionMode -> {
            if (!isSelectionMode) {
                folderAdapter.notifyDataSetChanged();
            }
        });

        return view;
    }

    public FoldersAdapter getFolderAdapter() {
        return folderAdapter;
    }
}