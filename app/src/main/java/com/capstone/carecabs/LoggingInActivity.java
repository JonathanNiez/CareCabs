package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.capstone.carecabs.Firebase.FirebaseMain;

public class LoggingInActivity extends AppCompatActivity {
	private static final long SPLASH_SCREEN_DELAY = 2000; // 2 seconds
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logging_in);

		new Handler().postDelayed(() -> {

			if (FirebaseMain.getUser() != null) {
				intent = new Intent(LoggingInActivity.this, MainActivity.class);
			} else {
				intent = new Intent(LoggingInActivity.this, LoginActivity.class);
			}
			startActivity(intent);
			finish();

		}, SPLASH_SCREEN_DELAY);
	}
}