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
import java.util.Map;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter notesAdapter;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes_list, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = view.findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notesAdapter = new NotesAdapter(viewModel);
        recyclerView.setAdapter(notesAdapter);

        if (recyclerView == null) {
            Log.e("NotesFragment", "RecyclerView is null!");
            return view;
        }

        TextView emptyView = view.findViewById(R.id.empty_notes_view);
        if (emptyView == null) {
            Log.e("NotesFragment", "empty_notes_view not found in layout!");
        }

        Bundle args = getArguments();
        Long folderId = null;
        if (args != null) {
            String folderName = args.getString("folder_name");
            if (folderName != null) {
                for (Folder folder : viewModel.getFolders().getValue()) {
                    if (folder.getName().equals(folderName)) {
                        folderId = folder.getId();
                        viewModel.setCurrentFolderId(folderId);
                        break;
                    }
                }
            }
        }
        viewModel.loadNotes(folderId);
        viewModel.loadFolders();

        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            Log.d("NotesFragment", "Received notes update: " + notes.size() + " items");
            notesAdapter.setNotes(notes != null ? notes : new ArrayList<>());
            notesAdapter.notifyDataSetChanged();
            recyclerView.setVisibility(View.VISIBLE);
            if (notes.isEmpty()) {
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Нет заметок в этой папке");
                    Log.d("NotesFragment", "Showing empty view");
                }
            } else {
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                    Log.d("NotesFragment", "Hiding empty view");
                }
            }
            Log.d("NotesFragment", "RecyclerView visibility: " + recyclerView.getVisibility());
        });

        viewModel.getFolders().observe(getViewLifecycleOwner(), folders -> {
            Map<Long, String> folderIdToColor = new HashMap<>();
            for (Folder folder : folders) {
                folderIdToColor.put(folder.getId(), folder.getColor());
            }
            Log.d("NotesFragment", "Received folder colors update: " + folderIdToColor.size() + " items");
            notesAdapter.setFolderColors(folderIdToColor);
        });

        viewModel.getIsSelectionMode().observe(getViewLifecycleOwner(), isSelectionMode -> {
            if (!isSelectionMode) {
                notesAdapter.notifyDataSetChanged();
            }
        });

        return view;
    }

    public NotesAdapter getNotesAdapter() {
        return notesAdapter;
    }
}