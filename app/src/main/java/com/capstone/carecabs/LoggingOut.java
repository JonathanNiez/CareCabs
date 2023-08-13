package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoggingOut extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private static final long SPLASH_SCREEN_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging_out);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        new Handler().postDelayed(() -> {

            auth.signOut();

            Intent intent = new Intent(LoggingOut.this, Login.class);
            startActivity(intent);
            finish();

        }, SPLASH_SCREEN_DELAY);
    }
}