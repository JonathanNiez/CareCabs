package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.capstone.carecabs.databinding.ActivityHelpBinding;

public class HelpActivity extends AppCompatActivity {
	private ActivityHelpBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityHelpBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.imgBackBtn.setOnClickListener(view -> {
			finish();
		});
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}