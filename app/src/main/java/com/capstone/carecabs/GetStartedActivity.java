package com.capstone.carecabs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.databinding.ActivityGetStartedBinding;
import com.capstone.carecabs.databinding.DialogExitAppBinding;
import com.google.firebase.FirebaseApp;

public class GetStartedActivity extends AppCompatActivity {
	private Intent intent;
	private boolean isLocationPermissionGranted, isCameraPermissionGranted, isStoragePermissionGranted;
	private AlertDialog exitAppDialog;
	private AlertDialog.Builder builder;
	private ActivityGetStartedBinding binding;

	@Override
	protected void onPause() {
		super.onPause();

		closeExitConfirmationDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		closeExitConfirmationDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		FirebaseApp.initializeApp(this);

		checkLocationPermission();
		checkCameraPermission();
		checkStoragePermission();

		if (FirebaseMain.getUser() != null) {
			intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
		}

		binding.getStartedBtn.setOnClickListener(v -> {
			if (isLocationPermissionGranted && isStoragePermissionGranted && isCameraPermissionGranted) {
				intent = new Intent(this, LoginOrRegisterActivity.class);
			} else {
				intent = new Intent(this, RequestPermissionActivity.class);
			}
			startActivity(intent);
			finish();
		});
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		showExitConfirmationDialog();
	}

	private void checkLocationPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED)
			isLocationPermissionGranted = true;
	}

	private void checkCameraPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED)
			isCameraPermissionGranted = true;

	}

	private void checkStoragePermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED)
			isStoragePermissionGranted = true;
	}

	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(this);

		DialogExitAppBinding dialogExitAppBinding =
				DialogExitAppBinding.inflate(getLayoutInflater());
		View dialogView = dialogExitAppBinding.getRoot();

		dialogExitAppBinding.exitBtn.setOnClickListener(v -> finish());

		dialogExitAppBinding.cancelBtn.setOnClickListener(v -> closeExitConfirmationDialog());

		builder.setView(dialogView);

		exitAppDialog = builder.create();
		exitAppDialog.show();
	}

	private void closeExitConfirmationDialog() {
		if (exitAppDialog != null && exitAppDialog.isShowing()) {
			exitAppDialog.dismiss();
		}
	}
}