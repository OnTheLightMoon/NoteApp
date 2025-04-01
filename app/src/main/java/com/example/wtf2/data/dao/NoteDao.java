package com.example.wtf2.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wtf2.data.model.Note;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE folderId = :folderId")
    List<Note> getNotesByFolderId(long folderId); // Изменяем с int на long

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query")
    List<Note> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteById(int id);

    @Insert
    long insert(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNotes(List<Note> notes);

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    void deleteNotesByIds(List<Integer> ids);

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(List<Integer> ids, boolean isPinned);
}