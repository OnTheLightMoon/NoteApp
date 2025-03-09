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
import java.util.stream.Collectors;

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
        recyclerView.setAdapter(notesAdapter);

        db = AppDatabase.getInstance(getContext()); // Инициализируем базу данных

        // Получаем имя папки из аргументов
        selectedFolder = getArguments() != null ? getArguments().getString("folder_name") : null;

        // Загружаем заметки (по папке или все)
        reloadNotes();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadNotes();
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
            List<Note> notes = db.noteDao().getAllNotes();
            Log.d("DEBUG", "Заметок в базе: " + notes.size());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void loadNotesByFolder(String folderName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = db.noteDao().getNotesByFolder(folderName);
            Log.d("DEBUG", "Заметок в папке '" + folderName + "': " + notes.size());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // Метод для фильтрации заметок по запросу
    public void filterNotes(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> filteredNotes;
            if (selectedFolder != null) {
                // Если выбрана папка, фильтруем только внутри нее
                filteredNotes = db.noteDao().searchNotes("%" + query + "%").stream()
                        .filter(note -> note.getFolder().equals(selectedFolder))
                        .collect(Collectors.toList());
            } else {
                // Иначе ищем по всем заметкам
                filteredNotes = db.noteDao().searchNotes("%" + query + "%");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(filteredNotes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    // Метод для сортировки заметок
    public void sortNotes(int which) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> sortedNotes;
            NoteDao noteDao = db.noteDao();
            if (selectedFolder != null) {
                // Если выбрана папка, сортируем только ее заметки
                sortedNotes = noteDao.getNotesByFolder(selectedFolder);
            } else {
                // Иначе сортируем все заметки
                sortedNotes = noteDao.getAllNotes();
            }

            switch (which) {
                case 0: // Date Created (Asc)
                    sortedNotes = noteDao.getAllNotesSortedByCreatedAsc();
                    break;
                case 1: // Date Created (Desc)
                    sortedNotes = noteDao.getAllNotesSortedByCreatedDesc();
                    break;
                case 2: // Date Modified (Asc)
                    sortedNotes = noteDao.getAllNotesSortedByCreatedAsc(); // Предполагаю, что date — это modified
                    break;
                case 3: // Date Modified (Desc)
                    sortedNotes = noteDao.getAllNotesSortedByCreatedDesc(); // Предполагаю, что date — это modified
                    break;
                case 4: // Title (Asc)
                    sortedNotes = noteDao.getAllNotesSortedByTitleAsc();
                    break;
                case 5: // Title (Desc)
                    sortedNotes = noteDao.getAllNotesSortedByTitleDesc();
                    break;
            }

            if (getActivity() != null) {
                List<Note> finalSortedNotes = sortedNotes;
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(finalSortedNotes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }
}