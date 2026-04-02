package com.example.memetask;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.memetask.db.AppDatabase;
import com.example.memetask.db.MemeNoteEntity;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddState extends AppCompatActivity {
    Button btn_send_new, btn_goback_add;


    ImageButton img_happy, img_angry, img_sad, img_tired;
    ImageButton[] imageButtons;
    TextInputEditText enter_notes;
    EditText  enter_date;
    private int selectedMoodButtonId = View.NO_ID;
    ArrayList<MemeNoteEntity> recordArrayList = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_state);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        btn_send_new = findViewById(R.id.btn_send_new);
        btn_goback_add = findViewById(R.id.btn_goback_add);
        img_happy = findViewById(R.id.img_happy);
        img_angry = findViewById(R.id.img_angry);
        img_sad = findViewById(R.id.img_sad);
        img_tired = findViewById(R.id.img_tired);
        enter_date = findViewById(R.id.enter_date);
        enter_notes = findViewById(R.id.enter_notes);
        imageButtons = new ImageButton[]{img_happy, img_sad, img_angry, img_tired};
        DateFormat deviceDateFormat = android.text.format.DateFormat.getDateFormat(this);
        enter_date.setText(deviceDateFormat.format(new Date()));
        for (ImageButton imageButton : imageButtons) {
            imageButton.setOnClickListener(this::onMoodSelected);
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Choose a date")
                .build();
        enter_date.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER"));

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate  = format.format(calendar.getTime());
            enter_date.setText(formattedDate);
        });

        btn_goback_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        btn_send_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = enter_notes.getText().toString();
                String date = enter_date.getText().toString();
                String id;
                if (selectedMoodButtonId == R.id.img_happy) {
                    id = "happy";
                } else if (selectedMoodButtonId == R.id.img_sad) {
                    id = "sad";
                } else if (selectedMoodButtonId == R.id.img_angry) {
                    id = "angry";
                } else if (selectedMoodButtonId == R.id.img_tired) {
                    id = "tired";
                } else {
                    id = "";
                }
                if (id.equals("") || note.equals("") || date.equals("")) {
                    Toast.makeText(getApplicationContext(), "no data?", Toast.LENGTH_SHORT).show();
                } else {

                    MemeNoteEntity memeNote = new MemeNoteEntity(date, id, note);
                    recordArrayList.add(memeNote);
                    dbExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            long insertedId = AppDatabase
                                    .getInstance(getApplicationContext())
                                    .memeNoteDao()
                                    .insertNote(memeNote);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (insertedId > 0) {
                                        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Failed to save", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    public void onMoodSelected(View moodView) {
        selectedMoodButtonId = moodView.getId();
        for (ImageButton imageButton : imageButtons) {
            if (imageButton.getId() == selectedMoodButtonId) {
                imageButton.clearColorFilter();
                imageButton.setAlpha(0.9f);
                imageButton.setScaleX(1.18f);
                imageButton.setScaleY(1.18f);
                imageButton.setElevation(18f);
                //imageButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(R.color.select_color)));
                imageButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.select_color)));
            } else {
                imageButton.setColorFilter(Color.argb(155, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
                imageButton.setAlpha(0.48f);
                imageButton.setScaleX(1.0f);
                imageButton.setScaleY(1.0f);
                imageButton.setElevation(0f);
                imageButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A7A1C5")));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
