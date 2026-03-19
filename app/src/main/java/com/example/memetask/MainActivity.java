package com.example.memetask;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.example.memetask.db.MemeNoteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    Button btn_add;
    ImageView meme_img;
    RecyclerView recyclerView;
    TextView date_text;
    RecordAdapter adapter;
    private final ArrayList<MemeNoteEntity> records = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

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
        recyclerView = findViewById(R.id.recyclerview);
        date_text = findViewById(R.id.date_text);
        adapter = new RecordAdapter(this, records);
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
                                // Удаляем элемент из списка и обновляем базу данных
                                // Получаем объект для удаления
                                records.remove(position);

                                // Здесь также можно вызвать метод для обновления БД
                                deleteFromDatabase(model);
                                adapter.notifyDataSetChanged(); // Обновляем представление

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



    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecordsFromDb();
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
                }
            });
        }
    });
}
    private void loadRecordsFromDb() {
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<MemeNoteEntity> notes = AppDatabase
                        .getInstance(getApplicationContext())
                        .memeNoteDao()
                        .getAll();

                ArrayList<MemeNoteEntity> loaded = new ArrayList<>();
                for (MemeNoteEntity note : notes) {
                    loaded.add(new MemeNoteEntity(note.id ,note.date, note.imageId, note.notes));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        records.clear();
                        records.addAll(loaded);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
