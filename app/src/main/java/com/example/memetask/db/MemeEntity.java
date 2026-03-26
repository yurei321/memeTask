package com.example.memetask.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "memes",
        indices = {@Index(value = {"image_file"}, unique = true)}
)
public class MemeEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "image_file")
    public String imageFile;

    @ColumnInfo(name = "image_link")
    public String imageLink;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "tags_json")
    public String tagsJson;

    public long getId() {
        return id;
    }

    public String getImageFile() {
        return imageFile;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getTitle() {
        return title;
    }

    public String getTagsJson() {
        return tagsJson;
    }
}
