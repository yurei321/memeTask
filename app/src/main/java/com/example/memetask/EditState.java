package com.example.memetask;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.memetask.db.AppDatabase;
import com.example.memetask.db.MemeNoteEntity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditState extends AppCompatActivity {
    Button btn_goback, btn_send_edit;
    ImageButton img_happy, img_angry, img_sad, img_tired;
    ImageButton[] imageButtons;
    EditText enter_notes;
    TextView date_const;
    private int selectedMoodButtonId = View.NO_ID;
    ArrayList<MemeNoteEntity> recordArrayList = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    MemeNoteEntity memeNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_state);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Bundle arguments = getIntent().getExtras();
        if(arguments!=null) {
            memeNote = (MemeNoteEntity) arguments.getSerializable("DATA_FROM_ITEM");


        }


        btn_send_edit = findViewById(R.id.btn_send_edit);
        btn_goback = findViewById(R.id.btn_goback);
        img_happy = findViewById(R.id.img_happy);
        img_angry = findViewById(R.id.img_angry);
        img_sad = findViewById(R.id.img_sad);
        img_tired = findViewById(R.id.img_tired);
        date_const = findViewById(R.id.date_const);
        enter_notes = findViewById(R.id.enter_notes);
        imageButtons = new ImageButton[]{img_happy, img_sad, img_angry, img_tired};
        for (ImageButton imageButton : imageButtons) {
            imageButton.setOnClickListener(this::onMoodSelected);
        }

        date_const.setText(memeNote.getDate());
        enter_notes.setText(memeNote.getNotes());
        if ("happy".equals(memeNote.getImageId())) {
            selectedMoodButtonId = R.id.img_happy;
            onMoodSelected(img_happy);
        } else if ("sad".equals(memeNote.getImageId())) {
            selectedMoodButtonId = R.id.img_sad;
            onMoodSelected(img_sad);
        } else if ("angry".equals(memeNote.getImageId())) {
            selectedMoodButtonId = R.id.img_angry;
            onMoodSelected(img_angry);
        } else if ("tired".equals(memeNote.getImageId())) {
            selectedMoodButtonId = R.id.img_tired;
            onMoodSelected(img_tired);
        }
        long id = memeNote.getId();

        btn_goback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        btn_send_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = enter_notes.getText().toString();
//                String date = enter_date.getText().toString();
                String mood;
                if (selectedMoodButtonId == R.id.img_happy) {
                    mood = "happy";
                } else if (selectedMoodButtonId == R.id.img_sad) {
                    mood = "sad";
                } else if (selectedMoodButtonId == R.id.img_angry) {
                    mood = "angry";
                } else if (selectedMoodButtonId == R.id.img_tired) {
                    mood = "tired";
                } else {
                    mood = "";
                }
                if(mood.equals("") || note.equals("")){
                    Toast.makeText(getApplicationContext(), "no data?", Toast.LENGTH_SHORT).show();
                }
                else {
                    String date = date_const.getText().toString();

                    memeNote.setId(id);
                    memeNote.setNotes(note);
                    memeNote.setDate(date);
                    memeNote.setImageId(mood);

                    dbExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            long insertedId = AppDatabase
                                    .getInstance(getApplicationContext())
                                    .memeNoteDao()
                                    .updateNote(memeNote);

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
                imageButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#b9d1a3")));
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
