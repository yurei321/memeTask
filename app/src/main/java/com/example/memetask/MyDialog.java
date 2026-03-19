package com.example.memetask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import com.example.memetask.db.AppDatabase;
import com.example.memetask.db.MemeNoteEntity;

public class MyDialog extends DialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        return builder
                .setTitle("Удалить запись?")
                .setMessage("Вы хотите удалить эту запись?")
                .setPositiveButton("Да", null)
                .setNegativeButton("Отмена", null)
                .create();
    }


    private OnClickListener onClickListener = null;

    OnClickListener listenerPos;
    OnClickListener listenerNeg;

    // Set the click listener for the adapter
    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }
    interface OnClickListener {
        void posAnswer(int position);
        void negAnswer();
    }
    String title, message;
    Button positive, negative;

//    public MyDialog(String title, String message, Button positive, Button negative) {
//        this.title = title;
//        this.message = message;
//        this.positive = positive;
//        this.negative = negative;
//    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Button getPositive() {
        return positive;
    }

    public void setPositive(Button positive) {
        this.positive = positive;
    }

    public Button getNegative() {
        return negative;
    }

    public void setNegative(Button negative) {
        this.negative = negative;
    }
}
