package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.capstone.carecabs.databinding.ActivityRequestLocationPermissionBinding;

import org.checkerframework.checker.nullness.qual.NonNull;

public class RequestPermissionActivity extends AppCompatActivity {
	private final String TAG = "RequestPermissionActivity";
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
	private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
	private static final int STORAGE_PERMISSION_REQUEST_CODE = 3;
	private int locationPermissionResult, cameraPermissionResult, storagePermissionResult;
	private ActivityRequestLocationPermissionBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRequestLocationPermissionBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		requestLocationPermission();
		requestStoragePermission();
		requestCameraPermission();

		binding.allowLocationBtn.setOnClickListener(view -> requestLocationPermission());
		binding.allowCameraBtn.setOnClickListener(view -> requestCameraPermission());
		binding.allowStorageBtn.setOnClickListener(view -> requestStoragePermission());

		if (locationPermissionResult != 0 && cameraPermissionResult != 0 && storagePermissionResult != 0) {
			Intent intent = new Intent(this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	private void requestLocationPermission() {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED)
			locationPermissionResult = 1;

		else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					LOCATION_PERMISSION_REQUEST_CODE);
		}
	}

	private void requestCameraPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED)
			cameraPermissionResult = 1;

		else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		}
	}

	private void requestStoragePermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED)
			storagePermissionResult = 1;

		else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST_CODE);
		}
	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@SuppressLint("LongLogTag")
	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @androidx.annotation.NonNull @NonNull String[] permissions,
	                                       int @NonNull [] grantResults) {

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				showToast("Location Permission Granted");
				locationPermissionResult = 1;
			} else
				showToast("Location Permission Denied");

		} else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				showToast("Camera Permission Granted");
				cameraPermissionResult = 1;
			} else
				showToast("Camera Permission Denied");

		} else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				showToast("Storage Permission Granted");
				storagePermissionResult = 1;
			} else
				showToast("Storage Permission Denied");

		}

		if (locationPermissionResult == 1 && cameraPermissionResult == 1 && storagePermissionResult == 1) {
			Intent intent = new Intent(this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		} else {
			Log.e(TAG, "Not all Permissions Granted");
		}
	}
}