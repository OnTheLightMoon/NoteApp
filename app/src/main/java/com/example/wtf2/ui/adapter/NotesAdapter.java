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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.note.NoteEditActivity;
import com.example.wtf2.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Адаптер для отображения списка заметок в RecyclerView.
 * Поддерживает анимации, выбор элементов и отображение цветов папок.
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private static final String TAG = "NotesAdapter";
    private List<Note> notes = new ArrayList<>();
    private final MainViewModel viewModel;
    private Map<Long, String> folderIdToColor = new HashMap<>();
    private RecyclerView recyclerView;

    public NotesAdapter(MainViewModel viewModel) {
        this.viewModel = viewModel;
        Log.d(TAG, "NotesAdapter initialized");
    }

    /**
     * Устанавливает новый список заметок с использованием DiffUtil.
     */
    public void setNotes(List<Note> notes) {
        if (notes == null) notes = new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteDiffCallback(this.notes, notes));
        this.notes = new ArrayList<>(notes);
        diffResult.dispatchUpdatesTo(this);
        Log.d(TAG, "Set notes: " + notes.size() + " items");
    }

    /**
     * Устанавливает цвета папок для отображения в заметках.
     */
    public void setFolderColors(Map<Long, String> colors) {
        this.folderIdToColor = colors != null ? colors : new HashMap<>();
        notifyDataSetChanged();
        Log.d(TAG, "Folder colors updated: " + folderIdToColor.size() + " entries");
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
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        Log.d(TAG, "Attached to RecyclerView");
    }

    /**
     * Удаляет заметку с анимацией затухания.
     * @param note Заметка для удаления
     * @param onComplete Действие после завершения анимации (например, удаление из базы)
     */
    public void removeNoteWithAnimation(Note note, Runnable onComplete) {
        int position = notes.indexOf(note);
        if (position == -1) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
        fadeOut.setDuration(300);
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
                if (onComplete != null) onComplete.run();
                Log.d(TAG, "Note removed at position " + position);
            }
        });
        fadeOut.start();
    }

    /**
     * ViewHolder для элемента заметки.
     */
    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final CheckBox checkBox;
        private final ImageView pinIcon;
        private final View colorIndicator;
        private final View container;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title);
            dateTextView = itemView.findViewById(R.id.note_date);
            checkBox = itemView.findViewById(R.id.note_checkbox);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            container = itemView.findViewById(R.id.note_container);

            setupClickListeners();
        }

        /**
         * Настраивает обработчики кликов для элемента.
         */
        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Note note = notes.get(position);
                    handleItemClick(note, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Note note = notes.get(position);
                    return handleItemLongClick(note, position);
                }
                return false;
            });
        }

        /**
         * Обрабатывает клик по элементу.
         */
        private void handleItemClick(Note note, int position) {
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            if (isSelectionMode) {
                viewModel.toggleNoteSelection(note);
                notifyItemChanged(position);
                Log.d(TAG, "Toggled selection for note: " + note.getTitle());
            } else {
                Intent intent = new Intent(itemView.getContext(), NoteEditActivity.class);
                intent.putExtra("NOTE_ID", note.getId());
                itemView.getContext().startActivity(intent);
                Log.d(TAG, "Opening note for edit: " + note.getTitle());
            }
        }

        /**
         * Обрабатывает долгий клик по элементу.
         */
        private boolean handleItemLongClick(Note note, int position) {
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            if (!isSelectionMode) {
                viewModel.enterSelectionMode();
                viewModel.toggleNoteSelection(note);
                notifyItemChanged(position);
                Log.d(TAG, "Entered selection mode, selected note: " + note.getTitle());
                return true;
            }
            return false;
        }

        /**
         * Привязывает данные заметки к UI-элементам.
         */
        public void bind(Note note) {
            String displayText = note.getTitle().isEmpty() ? note.getContent() : note.getTitle();
            titleTextView.setText(displayText.isEmpty() ? "Без названия" : displayText);
            dateTextView.setText(note.getModifiedDate() != null ? note.getModifiedDate() : "");
            boolean isSelectionMode = Boolean.TRUE.equals(viewModel.getIsSelectionMode().getValue());
            checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(viewModel.isNoteSelected(note));
            pinIcon.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

            String folderColor = folderIdToColor.getOrDefault(note.getFolderId(), "#FFFFFF");
            colorIndicator.setBackgroundColor(Color.parseColor(folderColor));

            animateBackground(note);
        }

        /**
         * Анимирует изменение фона элемента при выборе/снятии выбора.
         */
        private void animateBackground(Note note) {
            int colorFrom = viewModel.isNoteSelected(note) ? Color.WHITE : Color.parseColor("#E0F7FA");
            int colorTo = viewModel.isNoteSelected(note) ? Color.parseColor("#E0F7FA") : Color.WHITE;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animator -> container.setBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }
    }

    /**
     * Callback для DiffUtil, сравнивает старые и новые заметки.
     */
    private static class NoteDiffCallback extends DiffUtil.Callback {
        private final List<Note> oldNotes;
        private final List<Note> newNotes;

        public NoteDiffCallback(List<Note> oldNotes, List<Note> newNotes) {
            this.oldNotes = oldNotes;
            this.newNotes = newNotes;
        }

        @Override
        public int getOldListSize() { return oldNotes.size(); }
        @Override
        public int getNewListSize() { return newNotes.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldNotes.get(oldItemPosition).getId() == newNotes.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Note oldNote = oldNotes.get(oldItemPosition);
            Note newNote = newNotes.get(newItemPosition);
            return oldNote.getTitle().equals(newNote.getTitle()) &&
                    oldNote.getContent().equals(newNote.getContent()) &&
                    oldNote.getModifiedDate().equals(newNote.getModifiedDate()) &&
                    oldNote.isPinned() == newNote.isPinned() &&
                    oldNote.getFolderId() == newNote.getFolderId();
        }
    }
}