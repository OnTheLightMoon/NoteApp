package com.example.wtf2.data.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wtf2.data.model.Folder;

import java.util.List;

/**
 * DAO для работы с таблицей папок в базе данных.
 */
@Dao
public interface FolderDao {
    /**
     * Получает все папки из базы данных.
     */
    @Query("SELECT * FROM folders")
    @NonNull
    List<Folder> getAllFolders();

    /**
     * Получает папку по имени.
     */
    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    Folder getFolderByName(@NonNull String name);

    /**
     * Вставляет новую папку в базу данных.
     * @return Идентификатор новой папки
     */
    @Insert
    long insertFolder(@NonNull Folder folder);

    /**
     * Обновляет существующую папку.
     */
    @Update
    void updateFolder(@NonNull Folder folder);

    /**
     * Удаляет список папок.
     */
    @Delete
    void deleteFolders(@NonNull List<Folder> folders);

    /**
     * Удаляет папки по их идентификаторам.
     */
    @Query("DELETE FROM folders WHERE id IN (:ids)")
    void deleteFoldersByIds(@NonNull List<Long> ids);

    /**
     * Обновляет статус закрепления для списка папок.
     */
    @Query("UPDATE folders SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(@NonNull List<Long> ids, boolean isPinned);
}