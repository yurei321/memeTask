package com.example.memetask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memetask.db.AppDatabase;
import com.example.memetask.db.MemeEntity;
import com.example.memetask.db.MemeNoteEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    ImageButton btn_add;
    ImageView meme_img;
    ProgressBar meme_loading;
    RecyclerView recyclerView;
    TextView date_text;
    TextView today_meme_text;
    RecordAdapter adapter;
    private final ArrayList<MemeNoteEntity> records = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService memeExecutor = Executors.newSingleThreadExecutor();
    private final Animation memeLoadingAnimation = new AlphaAnimation(0.45f, 1.0f);
    private final AtomicInteger loadRequestVersion = new AtomicInteger(0);
    private Calendar monday;
    private Calendar sunday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AiLogger.i(TAG, "onCreate started");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_add = findViewById(R.id.btn_add);
        meme_img = findViewById(R.id.meme_img);
        meme_loading = findViewById(R.id.meme_loading);
        recyclerView = findViewById(R.id.recyclerview);
        date_text = findViewById(R.id.date_text);
        today_meme_text = findViewById(R.id.today_meme_text);

        updateCurrentWeekRangeText();
        adapter = new RecordAdapter(this, records, monday, sunday);
        adapter.setOnClickListener(new RecordAdapter.OnClickListener() {
            @Override
            public void updateOnClick(int position, MemeNoteEntity model) {
                AiLogger.i(TAG, "Edit requested for noteId=" + model.getId());
                Intent intent = new Intent(getApplicationContext(), EditState.class);
                intent.putExtra("DATA_FROM_ITEM", model);
                startActivity(intent);
            }

            @Override
            public void deleteOnClick(int position, MemeNoteEntity model) {
                AiLogger.i(TAG, "Delete confirmation opened for noteId=" + model.getId());
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Удалить запись?")
                        .setMessage("Вы хотите удалить эту запись?")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFromDatabase(model);
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AiLogger.i(TAG, "Add screen opened");
                Intent intent = new Intent(getApplicationContext(), AddState.class);
                startActivity(intent);
            }
        });

        memeLoadingAnimation.setDuration(650);
        memeLoadingAnimation.setRepeatCount(Animation.INFINITE);
        memeLoadingAnimation.setRepeatMode(Animation.REVERSE);
        AiLogger.i(TAG, "onCreate finished; UI initialized");
    }

    @Override
    protected void onResume() {
        super.onResume();
        AiLogger.i(TAG, "onResume triggered");
        updateCurrentWeekRangeText();
        loadRecordsFromDb();
    }

    private void updateCurrentWeekRangeText() {
        monday = Calendar.getInstance();
        int dayOfWeek = monday.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek + 5) % 7;
        monday.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);

        DateFormat deviceDateFormat = android.text.format.DateFormat.getDateFormat(this);
        String weekRange = deviceDateFormat.format(monday.getTime())
                + " - "
                + deviceDateFormat.format(sunday.getTime());
        date_text.setText(weekRange);
        AiLogger.d(TAG, "Current week range updated to " + weekRange);
    }

    private void deleteFromDatabase(MemeNoteEntity model) {
        AiLogger.i(TAG, "deleteFromDatabase requested for noteId=" + model.getId());
        removeRecordFromUi(model.getId());
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long deletedCount = AppDatabase
                        .getInstance(getApplicationContext())
                        .memeNoteDao()
                        .deleteNote(model);
                AiLogger.d(TAG, "deleteFromDatabase DAO result=" + deletedCount + " for noteId=" + model.getId());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (deletedCount > 0) {
                            Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                            loadRecordsFromDb();
                        }
                    }
                });
            }
        });
    }

    private void loadRecordsFromDb() {
        AiLogger.i(TAG, "loadRecordsFromDb scheduled");
        int requestVersion = loadRequestVersion.incrementAndGet();
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long startedAtNs = System.nanoTime();
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<MemeNoteEntity> notes = db.memeNoteDao().getAll();
                List<MemeEntity> allMemes = db.memeDao().getAllMemes();
                AiLogger.i(TAG, "loadRecordsFromDb fetched notesCount=" + notes.size());

                ArrayList<MemeNoteEntity> loaded = new ArrayList<>();
                for (MemeNoteEntity note : notes) {
                    loaded.add(new MemeNoteEntity(note.id, note.date, note.imageId, note.notes));
                }

                JSONObject weeklyJson = buildWeeklyEntriesJson(loaded);
                List<String> availableTags = collectAvailableMemeTags(allMemes);
                AiLogger.d(TAG, "weeklyEntriesJson prepared=" + weeklyJson);
                AiLogger.d(TAG, "availableMemeTags=" + availableTags);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        records.clear();
                        records.addAll(loaded);
                        adapter.notifyDataSetChanged();
                        AiLogger.i(TAG, "RecyclerView updated with recordsCount=" + loaded.size());
                        startMemeLoadingEffect();
                    }
                });

                memeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        MemePickResult pickResult = pickMemeByAiTags(db, weeklyJson, availableTags);
                        MemeEntity selectedMeme = pickResult.meme != null ? pickResult.meme : db.memeDao().getFirstMeme();
                        if (pickResult.meme == null) {
                            AiLogger.w(TAG, "AI did not produce a matching meme; fallback first meme="
                                    + describeMeme(selectedMeme));
                        } else {
                            AiLogger.i(TAG, "AI selected meme=" + describeMeme(pickResult.meme)
                                    + ", tag=" + pickResult.tagUsed);
                        }

                        Bitmap memeBitmap = loadMemeBitmapFromAssets(selectedMeme != null ? selectedMeme.getImageFile() : null);
                        long durationMs = (System.nanoTime() - startedAtNs) / 1_000_000L;
                        AiLogger.i(TAG, "loadRecordsFromDb completed in " + durationMs + " ms");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (requestVersion != loadRequestVersion.get()) {
                                    AiLogger.i(TAG, "Ignoring stale meme result for requestVersion=" + requestVersion);
                                    return;
                                }

                                stopMemeLoadingEffect();
                                if (selectedMeme != null) {
                                    if (memeBitmap != null) {
                                        meme_img.setImageBitmap(memeBitmap);
                                        AiLogger.d(TAG, "Meme bitmap applied to UI for " + describeMeme(selectedMeme));
                                    } else {
                                        meme_img.setImageResource(R.drawable.ic_launcher_background);
                                        AiLogger.w(TAG, "Meme bitmap missing, fallback drawable used for "
                                                + describeMeme(selectedMeme));
                                    }
                                } else {
                                    meme_img.setImageResource(R.drawable.ic_launcher_background);
                                    AiLogger.w(TAG, "No meme found in database; placeholder drawable applied");
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void removeRecordFromUi(long noteId) {
        boolean removed = false;
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).getId() == noteId) {
                records.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            adapter.notifyDataSetChanged();
            AiLogger.i(TAG, "Record removed from UI immediately for noteId=" + noteId);
        } else {
            AiLogger.w(TAG, "Record not found in current UI list for noteId=" + noteId);
        }
    }

    private MemePickResult pickMemeByAiTags(AppDatabase db, JSONObject weeklyJson, List<String> availableTags) {
        MemePickResult result = new MemePickResult();
        try {
            AiLogger.i(TAG, "pickMemeByAiTags started");
            if (availableTags == null || availableTags.isEmpty()) {
                AiLogger.w(TAG, "pickMemeByAiTags skipped because availableTags is empty");
                return result;
            }
            OpenRouterClient client = new OpenRouterClient(BuildConfig.OPENROUTER_API_KEY);
            List<String> tags = client.requestTags(weeklyJson, availableTags);
            AiLogger.i(TAG, "pickMemeByAiTags received tags=" + tags);
            for (String tag : tags) {
                MemeEntity memeByTag = db.memeDao().getFirstMemeByTag(tag);
                AiLogger.d(TAG, "Lookup meme by tag '" + tag + "' => " + describeMeme(memeByTag));
                if (memeByTag != null) {
                    result.meme = memeByTag;
                    result.tagUsed = tag;
                    AiLogger.i(TAG, "pickMemeByAiTags matched tag '" + tag + "'");
                    return result;
                }
            }
            AiLogger.w(TAG, "pickMemeByAiTags found no meme for returned tags");
        } catch (Exception e) {
            AiLogger.w(TAG, "AI tag selection failed, using fallback meme", e);
        }
        return result;
    }

    private List<String> collectAvailableMemeTags(List<MemeEntity> memes) {
        Set<String> tags = new LinkedHashSet<>();
        if (memes == null) {
            return new ArrayList<>();
        }
        for (MemeEntity meme : memes) {
            if (meme == null || meme.getTagsJson() == null || meme.getTagsJson().trim().isEmpty()) {
                continue;
            }
            try {
                JSONArray tagArray = new JSONArray(meme.getTagsJson());
                for (int index = 0; index < tagArray.length(); index++) {
                    String tag = tagArray.optString(index, "").trim().toLowerCase(Locale.US);
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
            } catch (JSONException exception) {
                AiLogger.w(TAG, "Failed to parse meme tagsJson for memeId=" + meme.getId(), exception);
            }
        }
        return new ArrayList<>(tags);
    }

    private Bitmap loadMemeBitmapFromAssets(String imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            AiLogger.w(TAG, "loadMemeBitmapFromAssets skipped because imageFile is empty");
            return null;
        }
        String assetPath = "toMoveToDb/" + imageFile;
        AiLogger.d(TAG, "Loading meme asset from " + assetPath);
        try (InputStream inputStream = getAssets().open(assetPath)) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            AiLogger.e(TAG, "Failed to load meme asset from " + assetPath, e);
            return null;
        }
    }

    private void startMemeLoadingEffect() {
        AiLogger.d(TAG, "startMemeLoadingEffect");
        meme_loading.setVisibility(View.VISIBLE);
        meme_img.setImageResource(R.drawable.ic_launcher_background);
        meme_img.startAnimation(memeLoadingAnimation);
    }

    private void stopMemeLoadingEffect() {
        AiLogger.d(TAG, "stopMemeLoadingEffect");
        meme_loading.setVisibility(View.GONE);
        meme_img.clearAnimation();
    }

    private JSONObject buildWeeklyEntriesJson(List<MemeNoteEntity> allEntries) {
        JSONObject result = new JSONObject();
        JSONArray entries = new JSONArray();
        JSONObject moodSummary = new JSONObject();

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        Calendar startCal = (Calendar) endCal.clone();
        startCal.add(Calendar.DAY_OF_MONTH, -6);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        DateFormat deviceDateFormat = android.text.format.DateFormat.getDateFormat(this);
        Map<String, Integer> counters = new HashMap<>();

        try {
            for (MemeNoteEntity entry : allEntries) {
                Date parsedDate = parseEntryDate(entry.getDate(), deviceDateFormat);
                if (parsedDate == null) {
                    AiLogger.w(TAG, "Skipping entry with unparsable date. id=" + entry.getId()
                            + ", rawDate=" + entry.getDate());
                    continue;
                }
                long timestamp = parsedDate.getTime();
                if (timestamp < startCal.getTimeInMillis() || timestamp > endCal.getTimeInMillis()) {
                    AiLogger.d(TAG, "Skipping entry outside 7-day window. id=" + entry.getId()
                            + ", rawDate=" + entry.getDate());
                    continue;
                }

                JSONObject entryJson = new JSONObject();
                entryJson.put("id", entry.getId());
                entryJson.put("date", entry.getDate());
                entryJson.put("moodType", entry.getImageId());
                entryJson.put("description", entry.getNotes());
                entries.put(entryJson);

                String mood = entry.getImageId() == null ? "unknown" : entry.getImageId();
                counters.put(mood, counters.getOrDefault(mood, 0) + 1);
            }

            for (Map.Entry<String, Integer> moodEntry : counters.entrySet()) {
                moodSummary.put(moodEntry.getKey(), moodEntry.getValue());
            }

            result.put("periodStart", deviceDateFormat.format(startCal.getTime()));
            result.put("periodEnd", deviceDateFormat.format(endCal.getTime()));
            result.put("entriesCount", entries.length());
            result.put("entries", entries);
            result.put("moodSummary", moodSummary);
            AiLogger.i(TAG, "buildWeeklyEntriesJson finished entriesCount=" + entries.length()
                    + ", moods=" + moodSummary);
        } catch (JSONException e) {
            AiLogger.e(TAG, "Failed to build weekly JSON", e);
        }

        return result;
    }

    private Date parseEntryDate(String dateText, DateFormat deviceDateFormat) {
        if (dateText == null || dateText.trim().isEmpty()) {
            AiLogger.w(TAG, "parseEntryDate received empty value");
            return null;
        }
        try {
            Date parsedDate = deviceDateFormat.parse(dateText);
            AiLogger.d(TAG, "parseEntryDate parsed with device format: " + dateText);
            return parsedDate;
        } catch (ParseException ignored) {
            AiLogger.d(TAG, "parseEntryDate device format failed for " + dateText);
        }

        String[] fallbackPatterns = {"dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd"};
        for (String pattern : fallbackPatterns) {
            try {
                SimpleDateFormat fallback = new SimpleDateFormat(pattern, Locale.getDefault());
                fallback.setLenient(false);
                Date parsedDate = fallback.parse(dateText);
                AiLogger.d(TAG, "parseEntryDate parsed with fallback pattern " + pattern + ": " + dateText);
                return parsedDate;
            } catch (ParseException ignored) {
                AiLogger.d(TAG, "parseEntryDate fallback failed pattern=" + pattern + ", value=" + dateText);
            }
        }
        AiLogger.w(TAG, "parseEntryDate failed for value=" + dateText);
        return null;
    }

    private String describeMeme(MemeEntity meme) {
        if (meme == null) {
            return "<null>";
        }
        return "id=" + meme.getId()
                + ", title=" + meme.getTitle()
                + ", imageFile=" + meme.getImageFile()
                + ", tagsJson=" + meme.getTagsJson();
    }

    private static class MemePickResult {
        MemeEntity meme;
        String tagUsed;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AiLogger.i(TAG, "onDestroy called; shutting down executor");
        dbExecutor.shutdown();
        memeExecutor.shutdown();
    }
}
