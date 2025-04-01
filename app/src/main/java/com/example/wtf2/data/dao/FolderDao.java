package com.example.wtf2.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wtf2.data.model.Folder;

import java.util.List;

@Dao
public interface FolderDao {
    @Query("SELECT * FROM folders")
    List<Folder> getAllFolders();

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    Folder getFolderByName(String name);

    @Insert
    long insertFolder(Folder folder);

    @Update
    void updateFolder(Folder folder);

    @Delete
    void deleteFolders(List<Folder> folders);

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    void deleteFoldersByIds(List<Long> ids); // Изменяем с List<Integer> на List<Long>

    @Query("UPDATE folders SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(List<Long> ids, boolean isPinned); // Изменяем с List<Integer> на List<Long>
}