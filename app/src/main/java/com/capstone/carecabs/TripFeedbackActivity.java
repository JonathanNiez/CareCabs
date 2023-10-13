package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.capstone.carecabs.databinding.ActivityTripFeedbackBinding;

public class TripFeedbackActivity extends AppCompatActivity {
	private final String TAG = "TripFeedbackActivity";
	private ActivityTripFeedbackBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityTripFeedbackBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());



	}
}