package com.example.detectorv1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickDetectionByPhoto(View view) {
        Intent detectionByPhotoIntent = new Intent(this, DetectionByPhoto.class);
        startActivity(detectionByPhotoIntent);
    }

    public void onClickDetectionByVideo(View view) {
        Toast toast =
                Toast.makeText(
                        getApplicationContext(), "In development.", Toast.LENGTH_SHORT);
        toast.show();
    }
}