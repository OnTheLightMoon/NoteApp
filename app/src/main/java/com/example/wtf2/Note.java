package com.example.wtf2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String content;
    private String date;
    private String folder;

    public Note(String title, String content, String date, String folder) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.folder = folder;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getFolder() {
        return folder;
    }
}

