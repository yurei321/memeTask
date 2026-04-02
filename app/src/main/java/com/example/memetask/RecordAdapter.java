package com.example.memetask;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memetask.db.MemeNoteEntity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private OnClickListener onClickListener = null;

    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }

    interface OnClickListener {
        void updateOnClick(int position, MemeNoteEntity model);
        void deleteOnClick(int position, MemeNoteEntity model);
    }

    private ArrayList<MemeNoteEntity> recordArrayList;
    private Context context;
    private Calendar weekStart, weekEnd;

    public RecordAdapter(Context context, ArrayList<MemeNoteEntity> taskArrayList, Calendar weekStart, Calendar weekEnd) {
        this.recordArrayList = taskArrayList != null ? taskArrayList : new ArrayList<>();
        this.context = context;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
    }


    @NonNull
    @Override
    public RecordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.ViewHolder holder, int position) {
        ArrayList<MemeNoteEntity> weekRecords = sortNotes(weekSort(recordArrayList));
        MemeNoteEntity record = weekRecords.get(position);
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
        return weekSort(recordArrayList).size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordDate;
        ImageView recordImage;
        TextView recordMood;
        ImageButton recordEdit;
        ImageButton recordDelete;

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
    private ArrayList<MemeNoteEntity> weekSort(ArrayList<MemeNoteEntity> recordArrayList) {
        ArrayList<MemeNoteEntity> newMemeNoteArrayList = new ArrayList<>();
        if (recordArrayList == null || recordArrayList.isEmpty()) {
            return newMemeNoteArrayList;
        }
        if (weekStart == null || weekEnd == null) {
            newMemeNoteArrayList.addAll(recordArrayList);
            return newMemeNoteArrayList;
        }

        Calendar weekStartBoundary = (Calendar) weekStart.clone();
        weekStartBoundary.set(Calendar.HOUR_OF_DAY, 0);
        weekStartBoundary.set(Calendar.MINUTE, 0);
        weekStartBoundary.set(Calendar.SECOND, 0);
        weekStartBoundary.set(Calendar.MILLISECOND, 0);

        Calendar weekEndBoundary = (Calendar) weekEnd.clone();
        weekEndBoundary.set(Calendar.HOUR_OF_DAY, 23);
        weekEndBoundary.set(Calendar.MINUTE, 59);
        weekEndBoundary.set(Calendar.SECOND, 59);
        weekEndBoundary.set(Calendar.MILLISECOND, 999);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        simpleDateFormat.setLenient(false);
        for (MemeNoteEntity i : recordArrayList) {
            try {
                Date currentDate = simpleDateFormat.parse(i.getDate());
                if (currentDate != null
                        && !currentDate.before(weekStartBoundary.getTime())
                        && !currentDate.after(weekEndBoundary.getTime())) {
                    newMemeNoteArrayList.add(i);
                }
            } catch (ParseException e) {
                Log.w("RecordAdapter", "Skip invalid date: " + i.getDate(), e);
            }
        }
        return newMemeNoteArrayList;
    }
    private ArrayList<MemeNoteEntity> sortNotes(ArrayList<MemeNoteEntity> recordArrayList){
        if (recordArrayList == null || recordArrayList.size() < 2) {
            return recordArrayList;
        }

        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
        Collections.sort(recordArrayList, new Comparator<MemeNoteEntity>() {
            @Override

            public int compare(MemeNoteEntity o1, MemeNoteEntity o2) {
                try {
                    return dfm.parse(o2.getDate()).compareTo(dfm.parse(o1.getDate()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return recordArrayList;
    }

}
