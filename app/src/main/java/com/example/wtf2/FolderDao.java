package com.example.wtf2;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {
    @Query("SELECT * FROM folders")
    List<Folder> getAllFolders();

    @Insert
    void insertFolder(Folder folder);

    @Update
    void update(Folder folder); // Новый метод для обновления

    @Query("SELECT COUNT(*) FROM notes WHERE folder = :folderName")
    int getNoteCountByFolder(String folderName);

    @Query("UPDATE folders SET isPinned = :isPinned WHERE id IN (:ids)")
    void updatePinnedStatus(List<Integer> ids, boolean isPinned);

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    void deleteFolders(List<Integer> ids);
}