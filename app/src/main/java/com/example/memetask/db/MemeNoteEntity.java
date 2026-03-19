package com.example.memetask.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "meme_notes")
public class MemeNoteEntity  implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "image_id")
    public String imageId;

    @ColumnInfo(name = "notes")
    public String notes;
    @Ignore
    public MemeNoteEntity(long id, String date, String imageId, String notes) {
        this.id = id;
        this.date = date;
        this.imageId = imageId;
        this.notes = notes;
    }

    public MemeNoteEntity(String date, String imageId, String notes) {
        this.date = date;
        this.imageId = imageId;
        this.notes = notes;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
