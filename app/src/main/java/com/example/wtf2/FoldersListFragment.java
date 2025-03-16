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

public class FoldersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderAdapter folderAdapter;
    private List<Folder> folderList;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.folders, container, false);

        recyclerView = view.findViewById(R.id.folders_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(folderList, new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(Folder folder) {
                Bundle bundle = new Bundle();
                bundle.putString("folder_name", folder.getName());
                NotesListFragment notesFragment = new NotesListFragment();
                notesFragment.setArguments(bundle);
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, notesFragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFolderLongClick(Folder folder) {
                startSelectionMode();
                folderAdapter.toggleSelection(folder);
                updateSelectionCount();
            }

            @Override
            public void onSelectionChanged() {
                updateSelectionCount();
            }
        });
        recyclerView.setAdapter(folderAdapter);

        db = AppDatabase.getInstance(getContext());
        loadFolders();

        return view;
    }

    public void loadFolders() { // Сделали публичным
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders();
            for (Folder folder : folders) {
                int noteCount = db.folderDao().getNoteCountByFolder(folder.getName());
                folder.setNoteCount(noteCount);
            }
            folders.sort(Comparator.comparing(Folder::isPinned).reversed()
                    .thenComparing(Folder::getName));
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    folderList.clear();
                    folderList.addAll(folders);
                    folderAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    public void addNewFolder(String folderName, String color) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Folder folder = new Folder(folderName);
            folder.setColor(color);
            db.folderDao().insertFolder(folder);
            loadFolders();
        });
    }

    private void startSelectionMode() {
        if (getActivity() instanceof MainActivity) {
            folderAdapter.setSelectionMode(true);
            ((MainActivity) getActivity()).enterSelectionMode(folderAdapter.getSelectedFolders().size());
        }
    }

    public void exitSelectionMode() {
        folderAdapter.setSelectionMode(false);
    }

    private void updateSelectionCount() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectionCount(folderAdapter.getSelectedFolders().size());
        }
    }

    // Геттер для доступа к адаптеру
    public FolderAdapter getFolderAdapter() {
        return folderAdapter;
    }
}