package com.example.wtf2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotesListFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoteAdapter notesAdapter;
    private List<Note> notesList;
    private AppDatabase db;
    private String selectedFolder = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes_list, container, false);

        recyclerView = view.findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notesList = new ArrayList<>();
        notesAdapter = new NoteAdapter(notesList);
        recyclerView.setAdapter(notesAdapter); // ✅ Устанавливаем адаптер перед загрузкой

        /// Получаем имя папки из аргументов
        selectedFolder = getArguments() != null ? getArguments().getString("folder_name") : null;

        // Загружаем заметки (по папке или все)
        reloadNotes();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadNotes(); // ✅ Загружаем заметки после возврата во фрагмент
    }

    /**
     * Загружает заметки. Если выбрана папка, фильтруем по ней.
     */
    private void reloadNotes() {
        if (selectedFolder != null) {
            Log.d("DEBUG", "Загружаем заметки для папки: " + selectedFolder);
            loadNotesByFolder(selectedFolder);
        } else {
            Log.d("DEBUG", "Загружаем все заметки");
            loadAllNotes();
        }
    }

    private void loadAllNotes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = AppDatabase.getInstance(getContext()).noteDao().getAllNotes();
            Log.d("DEBUG", "Заметок в базе: " + notes.size());

            getActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
            });
        });
    }

    private void loadNotesByFolder(String folderName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = AppDatabase.getInstance(getContext()).noteDao().getNotesByFolder(folderName);
            Log.d("DEBUG", "Заметок в папке '" + folderName + "': " + notes.size());

            getActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
            });
        });
    }
    
}
