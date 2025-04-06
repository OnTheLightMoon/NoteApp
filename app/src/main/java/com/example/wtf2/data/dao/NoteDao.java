package com.example.wtf2.data.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wtf2.data.model.Note;

import java.util.List;

/**
 * DAO для работы с таблицей заметок в базе данных.
 */
@Dao
public interface NoteDao {
    /**
     * Получает все заметки из базы данных.
     */
    @Query("SELECT * FROM notes")
    @NonNull
    List<Note> getAllNotes();

    /**
     * Получает заметки по идентификатору папки.
     */
    @Query("SELECT * FROM notes WHERE folderId = :folderId")
    @NonNull
    List<Note> getNotesByFolderId(long folderId);

    /**
     * Ищет заметки по запросу в заголовке или содержимом.
     * @param query Поисковый запрос (чувствителен к регистру)
     */
    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query")
    @NonNull
    List<Note> searchNotes(@NonNull String query);

    /**
     * Получает заметку по идентификатору.
     */
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteById(int id);

    /**
     * Вставляет новую заметку в базу данных.
     * @return Идентификатор новой заметки
     */
    @Insert
    long insert(@NonNull Note note);

    /**
     * Обновляет существующую заметку.
     */
    @Update
    void updateNote(@NonNull Note note);

    /**
     * Удаляет список заметок.
     */
    @Delete
    void deleteNotes(@NonNull List<Note> notes);

    /**
     * Удаляет заметки по их идентификаторам.
     */
    @Query("DELETE FROM notes WHERE id IN (:ids)")
    void deleteNotesByIds(@NonNull List<Integer> ids);

    /**
     * Обновляет статус закрепления для списка заметок.
     */
    @Query("UPDATE notes SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(@NonNull List<Integer> ids, boolean isPinned);
}