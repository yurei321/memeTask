package com.example.memetask.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemeNoteDao {

    @Insert
    long insertNote(MemeNoteEntity item);

    @Update
    int updateNote(MemeNoteEntity item);

    @Query("SELECT * FROM meme_notes ORDER BY date DESC")
    List<MemeNoteEntity> getAll();



    @Delete
    int deleteNote(MemeNoteEntity item);
}
