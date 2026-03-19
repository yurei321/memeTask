package com.example.memetask;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddState extends AppCompatActivity {
    Button btn_send_new;


    ImageButton img_happy, img_angry, img_sad, img_tired;
    ImageButton[] imageButtons;
    EditText enter_notes, enter_date;
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
        img_happy = findViewById(R.id.img_happy);
        img_angry = findViewById(R.id.img_angry);
        img_sad = findViewById(R.id.img_sad);
        img_tired = findViewById(R.id.img_tired);
        enter_date = findViewById(R.id.enter_date);
        enter_notes = findViewById(R.id.enter_notes);
        imageButtons = new ImageButton[]{img_happy, img_sad, img_angry, img_tired};

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
                imageButton.setColorFilter(Color.argb(120, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            } else {
                imageButton.clearColorFilter();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
