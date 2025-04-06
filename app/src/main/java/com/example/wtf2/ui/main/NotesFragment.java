package com.example.wtf2.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.adapter.NotesAdapter;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фрагмент для отображения списка заметок.
 * Поддерживает отображение заметок из конкретной папки или всех заметок.
 */
public class NotesFragment extends Fragment {
    private static final String TAG = "NotesFragment";
    private RecyclerView recyclerView;
    private NotesAdapter notesAdapter;
    private MainViewModel viewModel;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes_list, container, false);
        Log.d(TAG, "Creating NotesFragment view");

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Настройка RecyclerView
        recyclerView = view.findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notesAdapter = new NotesAdapter(viewModel);
        recyclerView.setAdapter(notesAdapter);

        // Настройка пустого состояния
        emptyView = view.findViewById(R.id.empty_notes_view);

        setupObservers();
        handleArguments(savedInstanceState);

        return view;
    }

    /**
     * Настраивает наблюдателей за данными из ViewModel.
     */
    private void setupObservers() {
        // Наблюдение за списком заметок
        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            Log.d(TAG, "Notes updated: " + notes.size() + " items");
            notesAdapter.setNotes(notes != null ? notes : new ArrayList<>());
            notesAdapter.notifyDataSetChanged(); // TODO: Заменить на DiffUtil для оптимизации
            updateEmptyViewVisibility(notes.isEmpty());
        });

        // Наблюдение за цветами папок
        viewModel.getFolderColors().observe(getViewLifecycleOwner(), folderColors -> {
            Map<Long, String> folderIdToColor = new HashMap<>();
            for (Folder folder : viewModel.getFolders().getValue()) {
                folderIdToColor.put(folder.getId(), folder.getColor());
            }
            Log.d(TAG, "Folder colors updated: " + folderIdToColor.size() + " entries");
            notesAdapter.setFolderColors(folderIdToColor);
        });

        // Наблюдение за режимом выбора
        viewModel.getIsSelectionMode().observe(getViewLifecycleOwner(), isSelectionMode -> {
            if (!isSelectionMode) {
                notesAdapter.notifyDataSetChanged();
                Log.d(TAG, "Selection mode exited, adapter refreshed");
            }
        });
    }

    /**
     * Обрабатывает аргументы фрагмента (например, имя папки для фильтрации заметок).
     */
    private void handleArguments(Bundle savedInstanceState) {
        Bundle args = getArguments() != null ? getArguments() : savedInstanceState;
        if (args != null && args.containsKey("folder_name")) {
            String folderName = args.getString("folder_name");
            List<Folder> folders = viewModel.getFolders().getValue();
            if (folders != null) {
                for (Folder folder : folders) {
                    if (folder.getName().equals(folderName)) {
                        viewModel.setCurrentFolderId(folder.getId());
                        viewModel.loadNotes(folder.getId());
                        break;
                    }
                }
            }
        } else if (viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue()) {
            viewModel.loadNotes(viewModel.getCurrentFolderId().getValue());
        } else {
            viewModel.loadNotes(null);
        }
    }

    /**
     * Обновляет видимость текста "Нет заметок" в зависимости от состояния списка.
     */
    private void updateEmptyViewVisibility(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) emptyView.setText("Нет заметок в этой папке");
        Log.d(TAG, "Empty view visibility: " + (isEmpty ? "shown" : "hidden"));
    }

    public NotesAdapter getNotesAdapter() {
        return notesAdapter;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Сохраняем текущие аргументы, если они есть
        if (getArguments() != null) {
            outState.putAll(getArguments());
        }
    }
}