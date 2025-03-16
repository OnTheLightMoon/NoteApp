package com.example.wtf2;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> notes;
    private OnNoteClickListener listener;
    private List<Note> selectedNotes = new ArrayList<>();
    private boolean isSelectionMode = false;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);

        void onSelectionChanged();
    }

    public NoteAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        String title = note.getTitle().trim();

        if (title.isEmpty()) {
            title = note.getContent().length() > 30 ? note.getContent().substring(0, 30) + "..." : note.getContent();
        }

        holder.noteTitle.setText(title);
        holder.noteDate.setText("Ред: " + note.getDate());
        holder.pinIcon.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        // Устанавливаем цвет индикатора
        AppDatabase db = AppDatabase.getInstance(holder.itemView.getContext());
        String folderName = note.getFolder();
        if (folderName != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                List<Folder> folders = db.folderDao().getAllFolders();
                for (Folder folder : folders) {
                    if (folder.getName().equals(folderName)) {
                        int color = Color.parseColor(folder.getColor());
                        holder.itemView.post(() -> holder.colorIndicator.setBackgroundColor(color));
                        break;
                    }
                }
            });
        } else {
            holder.colorIndicator.setBackgroundColor(Color.parseColor("#FFFFFF")); // Белый по умолчанию
        }

        holder.itemView.setActivated(selectedNotes.contains(note));

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(note);
                if (listener != null) listener.onSelectionChanged();
            } else if (listener != null) {
                listener.onNoteClick(note);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && !isSelectionMode) {
                listener.onNoteLongClick(note);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedNotes.clear();
        }
        notifyDataSetChanged();
    }

    public List<Note> getSelectedNotes() {
        return selectedNotes;
    }

    public void toggleSelection(Note note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note);
        } else {
            selectedNotes.add(note);
        }
        notifyItemChanged(notes.indexOf(note));
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteDate;
        ImageView pinIcon;
        View colorIndicator; // Добавляем индикатор

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.note_title);
            noteDate = itemView.findViewById(R.id.note_date);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
        }
    }
}