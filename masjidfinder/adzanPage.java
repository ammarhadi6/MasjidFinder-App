package com.example.masjidfinder;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class adzanPage extends AppCompatActivity {
    MediaPlayer mysound1, mysound2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adzan_page);

        mysound1 = MediaPlayer.create(adzanPage.this, R.raw.azanhijaz);
        mysound2 = MediaPlayer.create(adzanPage.this, R.raw.azanmadinah);

        // Set onClickListeners for play and pause buttons
        findViewById(R.id.play_button1).setOnClickListener(v -> playit1());
        findViewById(R.id.pause_button1).setOnClickListener(v -> pauseit1());
        findViewById(R.id.play_button2).setOnClickListener(v -> playit2());
        findViewById(R.id.pause_button2).setOnClickListener(v -> pauseit2());

        ImageButton backadzan = findViewById(R.id.back_adzan);

        backadzan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(adzanPage.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    public void playit1() {
        if (mysound1 != null && !mysound1.isPlaying()) {
            mysound1.start();
        }
    }


    public void pauseit1() {
        if (mysound1 != null && mysound1.isPlaying()) {
            mysound1.pause();
        }
    }

    public void playit2() {
        if (mysound2 != null && !mysound2.isPlaying()) {
            mysound2.start();
        }
    }

    public void pauseit2() {
        if (mysound2 != null && mysound2.isPlaying()) {
            mysound2.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mysound1 != null) {
            mysound1.release();
            mysound1 = null;
        }
        if (mysound2 != null) {
            mysound2.release();
            mysound2 = null;
        }
    }
}