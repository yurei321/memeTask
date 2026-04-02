package com.example.memetask;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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
    private final Animation memeLoadingAnimation = new AlphaAnimation(0.45f, 1.0f);
    private Calendar monday, sunday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                Intent intent = new Intent(getApplicationContext(), EditState.class);
                intent.putExtra("DATA_FROM_ITEM", model);
                startActivity(intent);
            }

            @Override
            public void deleteOnClick(int position, MemeNoteEntity model) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Подтверждение")
                        .setMessage("Вы уверены, что хотите удалить этот элемент?")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFromDatabase(model);

                            }
                        })
                        .setNegativeButton("Нет", null)
                        .show();

//                MyDialog dialog = new MyDialog();
//                dialog.show(getSupportFragmentManager(), "custom");
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddState.class);
                startActivity(intent);
            }
        });
        memeLoadingAnimation.setDuration(650);
        memeLoadingAnimation.setRepeatCount(Animation.INFINITE);
        memeLoadingAnimation.setRepeatMode(Animation.REVERSE);




    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }
    private void deleteFromDatabase(MemeNoteEntity model){
    dbExecutor.execute(new Runnable() {
        @Override
        public void run() {
            long insertedId = AppDatabase
                    .getInstance(getApplicationContext())
                    .memeNoteDao()
                    .deleteNote(model);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (insertedId > 0) {
                        Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                        startActivity(intent);
//                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                    loadRecordsFromDb();
                }
            });
        }
    });
}
    private void loadRecordsFromDb() {
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<MemeNoteEntity> notes = db.memeNoteDao().getAll();

                ArrayList<MemeNoteEntity> loaded = new ArrayList<>();
                for (MemeNoteEntity note : notes) {
                    loaded.add(new MemeNoteEntity(note.id ,note.date, note.imageId, note.notes));
                }
                JSONObject weeklyJson = buildWeeklyEntriesJson(loaded);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        records.clear();
                        records.addAll(loaded);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "weeklyEntriesJson=" + weeklyJson.toString());
//                        today_meme_text.setText("Meme: подбираем...");
                        startMemeLoadingEffect();
                    }
                });

                MemePickResult pickResult = pickMemeByAiTags(db, weeklyJson);
                MemeEntity selectedMeme = pickResult.meme != null ? pickResult.meme : db.memeDao().getFirstMeme();
                Bitmap memeBitmap = loadMemeBitmapFromAssets(selectedMeme != null ? selectedMeme.getImageFile() : null);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopMemeLoadingEffect();
                        if (selectedMeme != null) {
                            String tagInfo = pickResult.tagUsed == null ? "" : " [" + pickResult.tagUsed + "]";
//                            today_meme_text.setText("Meme: " + selectedMeme.getTitle() + tagInfo);
                            if (memeBitmap != null) {
                                meme_img.setImageBitmap(memeBitmap);
                            } else {
                                meme_img.setImageResource(R.drawable.ic_launcher_background);
                            }
                        } else {
//                            today_meme_text.setText("Meme: (empty)");
                            meme_img.setImageResource(R.drawable.ic_launcher_background);
                        }
                    }
                });
            }
        });
    }

    private MemePickResult pickMemeByAiTags(AppDatabase db, JSONObject weeklyJson) {
        MemePickResult result = new MemePickResult();
        try {
            OpenRouterClient client = new OpenRouterClient(BuildConfig.OPENROUTER_API_KEY);
            List<String> tags = client.requestTags(weeklyJson);
            for (String tag : tags) {
                MemeEntity memeByTag = db.memeDao().getFirstMemeByTag(tag);
                if (memeByTag != null) {
                    result.meme = memeByTag;
                    result.tagUsed = tag;
                    return result;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "AI tag selection failed, using fallback meme", e);
        }
        return result;
    }

    private Bitmap loadMemeBitmapFromAssets(String imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        String assetPath = "toMoveToDb/" + imageFile;
        try (InputStream inputStream = getAssets().open(assetPath)) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private void startMemeLoadingEffect() {
        meme_loading.setVisibility(View.VISIBLE);
        meme_img.setImageResource(R.drawable.ic_launcher_background);
        meme_img.startAnimation(memeLoadingAnimation);
    }

    private void stopMemeLoadingEffect() {
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
                    continue;
                }
                long timestamp = parsedDate.getTime();
                if (timestamp < startCal.getTimeInMillis() || timestamp > endCal.getTimeInMillis()) {
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
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build weekly JSON", e);
        }

        return result;
    }

    private Date parseEntryDate(String dateText, DateFormat deviceDateFormat) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }
        try {
            return deviceDateFormat.parse(dateText);
        } catch (ParseException ignored) {
        }

        String[] fallbackPatterns = {"dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd"};
        for (String pattern : fallbackPatterns) {
            try {
                SimpleDateFormat fallback = new SimpleDateFormat(pattern, Locale.getDefault());
                fallback.setLenient(false);
                return fallback.parse(dateText);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static class MemePickResult {
        MemeEntity meme;
        String tagUsed;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
