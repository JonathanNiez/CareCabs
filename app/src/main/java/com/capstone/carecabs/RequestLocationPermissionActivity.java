package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.capstone.carecabs.databinding.ActivityRequestLocationPermissionBinding;

import org.checkerframework.checker.nullness.qual.NonNull;

public class RequestLocationPermissionActivity extends AppCompatActivity {
	private static final int REQUEST_CODE_LOCATION_PERMISSION = 123;
	private ActivityRequestLocationPermissionBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRequestLocationPermissionBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.allowLocationBtn.setOnClickListener(view -> {

			requestLocationPermission();
		});
	}

	private void requestLocationPermission() {
		String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

		// Request the location permission
		ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_LOCATION_PERMISSION);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
			// Check if the permission was granted
			Intent intent;
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				intent = new Intent(RequestLocationPermissionActivity.this, MapActivity.class);

				Toast.makeText(this, "Request Location Granted", Toast.LENGTH_SHORT).show();
			} else {
				intent = new Intent(RequestLocationPermissionActivity.this, MainActivity.class);
				Toast.makeText(this, "Request Location Denied", Toast.LENGTH_SHORT).show();

			}
			startActivity(intent);
			finish();

		}
	}

}