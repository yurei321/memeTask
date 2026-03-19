package com.example.memetask;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memetask.db.MemeNoteEntity;

import java.util.ArrayList;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private OnClickListener onClickListener = null;

    // Set the click listener for the adapter
    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }

    // Interface for the click listener
    interface OnClickListener {
        void updateOnClick(int position, MemeNoteEntity model);
        void deleteOnClick(int position, MemeNoteEntity model);
    }

    private ArrayList<MemeNoteEntity> recordArrayList;
    private Context context;

    public RecordAdapter(Context context, ArrayList<MemeNoteEntity> taskArrayList) {
        this.recordArrayList = taskArrayList;
        this.context = context;
    }


    @NonNull
    @Override
    public RecordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.ViewHolder holder, int position) {
        MemeNoteEntity record = recordArrayList.get(position);
        holder.recordDate.setText(record.getDate());
        holder.recordImage.setImageResource(getMoodDrawableRes(record.getImageId()));
        holder.recordMood.setText(record.getNotes());
        holder.recordEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.updateOnClick(position, record);
            }
        });
        holder.recordDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.deleteOnClick(position, record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordDate;
        ImageView recordImage;
        TextView recordMood;
        Button recordEdit;
        Button recordDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recordDate = itemView.findViewById(R.id.record_date_text);
            recordImage = itemView.findViewById(R.id.img_mood);
            recordMood = itemView.findViewById(R.id.mood_text);
            recordEdit = itemView.findViewById(R.id.btn_edit);
            recordDelete = itemView.findViewById(R.id.btn_delete);

        }
    }

    private int getMoodDrawableRes(String moodKey) {
        if ("happy".equals(moodKey)) {
            return R.drawable.img_happy;
        } else if ("sad".equals(moodKey)) {
            return R.drawable.img_sad;
        } else if ("angry".equals(moodKey)) {
            return R.drawable.img_angry;
        } else if ("tired".equals(moodKey)) {
            return R.drawable.img_tired;
        } else {
            return R.drawable.ic_launcher_background;
        }
    }

}
