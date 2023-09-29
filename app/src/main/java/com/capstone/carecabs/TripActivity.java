package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.capstone.carecabs.databinding.ActivityTripBinding;

public class TripActivity extends AppCompatActivity {
	private ActivityTripBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityTripBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());


	}
}