package com.example.memetask.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Database(entities = {MemeNoteEntity.class, MemeEntity.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String SEED_SQL_ASSET = "meme_seed.sql";
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `memes` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`image_file` TEXT NOT NULL, " +
                            "`image_link` TEXT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`tags_json` TEXT NOT NULL)"
            );
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_memes_image_file` ON `memes` (`image_file`)");
        }
    };
    private static volatile AppDatabase INSTANCE;

    public abstract MemeNoteDao memeNoteDao();
    public abstract MemeDao memeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            "meme_db"
                    )
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    runSeedSql(appContext, db);
                                }
                            })
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    runSeedSql(appContext, db);
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void runSeedSql(Context context, SupportSQLiteDatabase db) {
        try (InputStream is = context.getAssets().open(SEED_SQL_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sqlTextBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlTextBuilder.append(line).append('\n');
            }

            String sqlText = sqlTextBuilder.toString();
            if (!sqlText.isEmpty() && sqlText.charAt(0) == '\uFEFF') {
                sqlText = sqlText.substring(1);
            }

            sqlText = sqlText.replaceAll("(?m)^\\s*--.*$", "");
            String[] statements = sqlText.split(";");
            for (String statement : statements) {
                String sql = statement.trim();
                if (!sql.isEmpty()) {
                    db.execSQL(sql);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read seed SQL from assets", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute seed SQL", e);
        }
    }
}
