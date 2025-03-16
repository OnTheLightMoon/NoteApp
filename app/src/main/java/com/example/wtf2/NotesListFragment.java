package com.example.wtf2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
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
        notesAdapter = new NoteAdapter(notesList, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openNoteEditor(note);
                }
            }

            @Override
            public void onNoteLongClick(Note note) {
                startSelectionMode();
                notesAdapter.toggleSelection(note);
                updateSelectionCount();
            }

            @Override
            public void onSelectionChanged() {
                updateSelectionCount();
            }
        });
        recyclerView.setAdapter(notesAdapter);

        db = AppDatabase.getInstance(getContext());

        selectedFolder = getArguments() != null ? getArguments().getString("folder_name") : null;
        reloadNotes();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadNotes();
    }

    private void reloadNotes() {
        if (selectedFolder != null) {
            loadNotesByFolder(selectedFolder);
        } else {
            loadAllNotes();
        }
    }

    private void loadAllNotes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = db.noteDao().getAllNotes();
            notes.sort(Comparator.comparing(Note::isPinned).reversed()
                    .thenComparing(Note::getDate, Comparator.reverseOrder()));
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
            notes.sort(Comparator.comparing(Note::isPinned).reversed()
                    .thenComparing(Note::getDate, Comparator.reverseOrder()));
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

    public void filterNotes(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> filteredNotes;
            if (selectedFolder != null) {
                filteredNotes = new ArrayList<>();
                for (Note note : db.noteDao().searchNotes("%" + query + "%")) {
                    if (note.getFolder() != null && note.getFolder().equals(selectedFolder)) {
                        filteredNotes.add(note);
                    }
                }
            } else {
                filteredNotes = db.noteDao().searchNotes("%" + query + "%");
            }
            filteredNotes.sort(Comparator.comparing(Note::isPinned).reversed()
                    .thenComparing(Note::getDate, Comparator.reverseOrder()));

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(filteredNotes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    public void sortNotes(int which) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> sortedNotes = (selectedFolder != null)
                    ? db.noteDao().getNotesByFolder(selectedFolder)
                    : db.noteDao().getAllNotes();

            Comparator<Note> baseComparator = Comparator.comparing(Note::isPinned).reversed();
            Comparator<Note> finalComparator;

            switch (which) {
                case 0: // Date Modified Asc
                    finalComparator = baseComparator.thenComparing(Note::getDate);
                    break;
                case 1: // Date Modified Desc
                    finalComparator = baseComparator.thenComparing(Note::getDate, Comparator.reverseOrder());
                    break;
                case 2: // Title Asc
                    finalComparator = baseComparator.thenComparing(Note::getTitle);
                    break;
                case 3: // Title Desc
                    finalComparator = baseComparator.thenComparing(Note::getTitle, Comparator.reverseOrder());
                    break;
                default:
                    finalComparator = baseComparator.thenComparing(Note::getDate, Comparator.reverseOrder());
            }

            sortedNotes.sort(finalComparator);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesList.clear();
                    notesList.addAll(sortedNotes);
                    notesAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    public void refreshNotes() {
        reloadNotes();
    }

    private void startSelectionMode() {
        if (getActivity() instanceof MainActivity) {
            notesAdapter.setSelectionMode(true);
            ((MainActivity) getActivity()).enterSelectionMode(notesAdapter.getSelectedNotes().size());
        }
    }

    public void exitSelectionMode() {
        notesAdapter.setSelectionMode(false);
    }

    private void updateSelectionCount() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectionCount(notesAdapter.getSelectedNotes().size());
        }
    }

    // Геттер для доступа к адаптеру
    public NoteAdapter getNotesAdapter() {
        return notesAdapter;
    }
}