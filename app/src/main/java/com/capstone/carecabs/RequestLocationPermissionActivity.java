package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.capstone.carecabs.databinding.ActivityRequestLocationPermissinBinding;

public class RequestLocationPermissionActivity extends AppCompatActivity {
	private ActivityRequestLocationPermissinBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRequestLocationPermissinBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
	}
}