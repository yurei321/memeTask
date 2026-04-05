package com.example.memetask.db;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemeDao {

    @Query("SELECT * FROM memes ORDER BY id ASC")
    List<MemeEntity> getAllMemes();

    @Query("SELECT * FROM memes ORDER BY id ASC LIMIT 1")
    MemeEntity getFirstMeme();

    @Query("SELECT * FROM memes WHERE LOWER(tags_json) LIKE '%' || LOWER(:tag) || '%' ORDER BY id ASC LIMIT 1")
    MemeEntity getFirstMemeByTag(String tag);
}
