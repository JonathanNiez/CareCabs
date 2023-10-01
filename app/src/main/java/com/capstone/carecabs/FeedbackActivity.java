package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.capstone.carecabs.databinding.ActivityFeedbackBinding;

public class FeedbackActivity extends AppCompatActivity {
	private ActivityFeedbackBinding binding;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());


	}
}