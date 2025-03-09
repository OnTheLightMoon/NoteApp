package com.example.wtf2;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY date DESC")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE folder = :folderName ORDER BY date DESC") // ✅ Используем "folder"
    List<Note> getNotesByFolder(String folderName);


}
