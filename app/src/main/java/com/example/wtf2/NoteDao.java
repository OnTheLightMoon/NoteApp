package com.example.wtf2;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE folder = :folderName")
    List<Note> getNotesByFolder(String folderName);

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query")
    List<Note> searchNotes(String query);

    @Query("SELECT * FROM notes ORDER BY date ASC")
    List<Note> getAllNotesSortedByCreatedAsc();

    @Query("SELECT * FROM notes ORDER BY date DESC")
    List<Note> getAllNotesSortedByCreatedDesc();

    @Query("SELECT * FROM notes ORDER BY title ASC")
    List<Note> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM notes ORDER BY title DESC")
    List<Note> getAllNotesSortedByTitleDesc();

    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    // Новый метод для массового обновления isPinned
    @Query("UPDATE notes SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(List<Integer> ids, boolean isPinned);

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    void deleteNotes(List<Integer> ids);
}