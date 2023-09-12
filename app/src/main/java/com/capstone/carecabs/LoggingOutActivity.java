package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.capstone.carecabs.Firebase.FirebaseMain;

public class LoggingOutActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging_out);


        new Handler().postDelayed(() -> {

            FirebaseMain.signOutUser();

            Intent intent = new Intent(LoggingOutActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();

        }, SPLASH_SCREEN_DELAY);
    }
}