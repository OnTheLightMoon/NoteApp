package com.example.wtf2;

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface FolderDao {
    @Transaction // 🔥 Объединяем в транзакцию
    @Query("INSERT INTO folders (name) VALUES (:name)")
    void insertFolder(String name);

    @Query("SELECT * FROM folders ORDER BY id ASC")
    List<Folder> getAllFolders();
}
