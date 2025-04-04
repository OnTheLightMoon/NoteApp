package com.example.wtf2.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.note.NoteEditActivity;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes = new ArrayList<>();
    private final MainViewModel viewModel;
    private Map<Long, String> folderIdToColor = new HashMap<>(); // Изменяем с Integer на Long

    public NotesAdapter(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        Log.d("NotesAdapter", "Set notes: " + notes.size() + " items, notes = " + notes.toString());
        notifyDataSetChanged();
    }

    public void setFolderColors(Map<Long, String> colors) { // Изменяем с Integer на Long
        this.folderIdToColor = colors != null ? colors : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final CheckBox checkBox;
        private final ImageView pinIcon;
        private final View colorIndicator;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title);
            dateTextView = itemView.findViewById(R.id.note_date);
            checkBox = itemView.findViewById(R.id.note_checkbox);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            colorIndicator = itemView.findViewById(R.id.color_indicator);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Note note = notes.get(position);
                    if (viewModel.getIsSelectionMode().getValue()) {
                        viewModel.toggleNoteSelection(note);
                        notifyItemChanged(position);
                    } else {
                        Intent intent = new Intent(itemView.getContext(), NoteEditActivity.class);
                        intent.putExtra("NOTE_ID", note.getId());
                        itemView.getContext().startActivity(intent);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Note note = notes.get(position);
                    if (!viewModel.getIsSelectionMode().getValue()) {
                        viewModel.enterSelectionMode();
                        viewModel.toggleNoteSelection(note);
                        notifyItemChanged(position);
                        return true;
                    }
                }
                return false;
            });
        }

        public void bind(Note note) {
            String displayText = note.getTitle().isEmpty() ? note.getContent() : note.getTitle();
            titleTextView.setText(displayText.isEmpty() ? "Без названия" : displayText);
            dateTextView.setText(note.getModifiedDate());
            boolean isSelectionMode = viewModel.getIsSelectionMode().getValue();
            checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(viewModel.isNoteSelected(note));
            pinIcon.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

            String folderColor = folderIdToColor.getOrDefault(note.getFolderId(), "#FFFFFF");
            colorIndicator.setBackgroundColor(Color.parseColor(folderColor));

            View container = itemView.findViewById(R.id.note_container);
            int colorFrom = viewModel.isNoteSelected(note) ? Color.WHITE : Color.parseColor("#E0F7FA");
            int colorTo = viewModel.isNoteSelected(note) ? Color.parseColor("#E0F7FA") : Color.WHITE;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animator -> container.setBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }
    }

    // NotesAdapter.java
    public void removeNoteWithAnimation(Note note, Runnable onComplete) {
        int position = notes.indexOf(note);
        if (position != -1) {
            ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
            fadeOut.setDuration(300); // Анимация 300 мс
            fadeOut.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                NoteViewHolder holder = (NoteViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                if (holder != null) {
                    holder.itemView.setAlpha(alpha);
                }
            });
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    notes.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, notes.size());
                    if (onComplete != null) {
                        onComplete.run(); // Вызываем удаление из базы после анимации
                    }
                }
            });
            fadeOut.start();
        } else if (onComplete != null) {
            onComplete.run(); // Если заметка не найдена, сразу выполняем действие
        }
    }

    private RecyclerView recyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }
}