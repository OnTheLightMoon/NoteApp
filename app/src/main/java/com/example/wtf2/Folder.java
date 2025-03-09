package com.example.wtf2;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class Folder {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "name") // ✅ Указываем название столбца
    private String name;

    public Folder(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
}
