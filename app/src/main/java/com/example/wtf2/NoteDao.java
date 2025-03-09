package com.example.wtf2;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(int noteId);

    @Query("SELECT * FROM notes WHERE folder = :folderName")
    List<Note> getNotesByFolderId(int folderName);

    @Query("SELECT * FROM notes ORDER BY date DESC")
    List<Note> getAllNotes();

    @Query("SELECT COUNT(*) FROM notes WHERE folder = :folderName")
    int getNotesCountByFolder(int folderName);

    @Query("SELECT * FROM notes WHERE folder = :folderName ORDER BY date DESC") // ✅ Используем "folder"
    List<Note> getNotesByFolder(String folderName);

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query")
    List<Note> searchNotes(String query);

    @Query("SELECT * FROM notes ORDER BY id ASC")
    List<Note> getAllNotesSortedByCreatedAsc();

    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotesSortedByCreatedDesc();

    @Query("SELECT * FROM notes ORDER BY title ASC")
    List<Note> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM notes ORDER BY title DESC")
    List<Note> getAllNotesSortedByTitleDesc();
}
