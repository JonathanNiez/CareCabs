package com.capstone.carecabs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityScanIdBinding;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanIDActivity extends AppCompatActivity {
	private Uri imageUri;
	private AlertDialog.Builder builder;
	private AlertDialog optionsDialog, cancelScanIDDialog, noInternetDialog;
	private TextRecognizer textRecognizer;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private Intent intent;
	private final String TAG = "ScanID";
	private String getUserType;
	private boolean shouldExit = false;
	private boolean isIDScanningCancelled = false;
	private NetworkChangeReceiver networkChangeReceiver;
	private StorageReference storageRef;
	private StorageReference imagesRef;
	private StorageReference fileRef;
	private RequestManager requestManager;
	private ActivityScanIdBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityScanIdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		initializeNetworkChecker();
		checkPermission();

		intent = getIntent();
		getUserType = intent.getStringExtra("userType");

		binding.imgBackBtn.setOnClickListener(view -> {
			showCancelScanIDDialog();
		});

		binding.getImageBtn.setOnClickListener(v -> {
			showOptionsDialog();
		});
	}

	@Override
	public void onBackPressed() {

		if (shouldExit) {
			super.onBackPressed(); // Exit the app
		} else {
			// Show an exit confirmation dialog
			showCancelScanIDDialog();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closeOptionsDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closeOptionsDialog();
	}

	private void showCancelScanIDDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_scan_id, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			switch (getUserType) {
				case "Driver":
					intent = new Intent(ScanIDActivity.this, RegisterDriverActivity.class);

					break;

				case "Persons with Disabilities (PWD)":
					intent = new Intent(ScanIDActivity.this, RegisterPWDActivity.class);

					break;

				case "Senior Citizen":
					intent = new Intent(ScanIDActivity.this, RegisterSeniorActivity.class);

					break;

				case "From Main":
					intent = new Intent(ScanIDActivity.this, MainActivity.class);

					break;

			}

			closeCancelScanIDDialog();

			startActivity(intent);
			finish();
		});

		noBtn.setOnClickListener(v -> {
			closeCancelScanIDDialog();
		});


		builder.setView(dialogView);

		cancelScanIDDialog = builder.create();
		cancelScanIDDialog.show();
	}

	private void closeCancelScanIDDialog() {
		if (cancelScanIDDialog != null && cancelScanIDDialog.isShowing()) {
			cancelScanIDDialog.isShowing();
		}
	}

	private void checkPermission() {
		// Check for camera permission
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST);
		}

		// Check for storage permission
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST);
		}
	}

	private void openGallery() {
		Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		galleryIntent.setType("image/*");
		if (galleryIntent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
			if (optionsDialog != null && optionsDialog.isShowing()) {
				optionsDialog.dismiss();
			}

		} else {
			Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
		}
	}

	private void openCamera() {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (cameraIntent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
			if (optionsDialog != null && optionsDialog.isShowing()) {
				optionsDialog.dismiss();
			}

		} else {
			Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
		}
	}

	private void showOptionsDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_camera_gallery, null);

		Button openCameraBtn = dialogView.findViewById(R.id.openCameraBtn);
		Button openGalleryBtn = dialogView.findViewById(R.id.openGalleryBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		openCameraBtn.setOnClickListener(v -> {
			openCamera();
		});

		openGalleryBtn.setOnClickListener(v -> {
			openGallery();
		});

		cancelBtn.setOnClickListener(v -> {
			closeOptionsDialog();
		});

		builder.setView(dialogView);

		optionsDialog = builder.create();
		optionsDialog.show();
	}

	private void closeOptionsDialog() {
		if (optionsDialog != null && optionsDialog.isShowing()) {
			optionsDialog.dismiss();
		}
	}

	private boolean matchesPattern(String text, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);
		return m.find();
	}

	private Uri getImageUri(Context context, Bitmap bitmap) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
		return Uri.parse(path);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			if (resultCode == RESULT_OK) {
				if (requestCode == CAMERA_REQUEST_CODE) {
					if (data != null) {

						Bundle extras = data.getExtras();
						Bitmap imageBitmap = (Bitmap) extras.get("data");

						imageUri = getImageUri(getApplicationContext(), imageBitmap);
						binding.idPreview.setImageURI(imageUri);

						uploadImageToFirebaseStorage(imageUri);

						Toast.makeText(this, "Image is Loaded from Camera", Toast.LENGTH_LONG).show();


					} else {
						Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
					}

				} else if (requestCode == GALLERY_REQUEST_CODE) {
					if (data != null) {

						imageUri = data.getData();
						binding.idPreview.setImageURI(imageUri);

						Toast.makeText(this, "Image is Loaded from Gallery", Toast.LENGTH_LONG).show();

					} else {
						Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
					}
				}
			} else {
				Toast.makeText(this, "Image is not Selected", Toast.LENGTH_SHORT).show();
				Log.e(TAG + ":ERROR", "Failed to Extract Text");

				Toast.makeText(this, "Failed to Extract Text", Toast.LENGTH_LONG).show();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void uploadImageToFirebaseStorage(Uri imageUri) {
		fileRef = imagesRef.child(imageUri.getLastPathSegment());
		fileRef.putFile(imageUri).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {
				fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

					// Save the download URL in Firestore
					Map<String, Object> data = new HashMap<>();
					data.put("profilePicUrl", uri.toString());
					binding.idPreview.setImageURI(imageUri);

				}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
			} else {
				Log.e(TAG, String.valueOf(task.getException()));
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CAMERA_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG, "Camera Permission Granted");
			}
		} else if (requestCode == STORAGE_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG, "Gallery Permission Granted");
			}
		} else {
			Log.e(TAG, "Permission Denied");

		}
	}

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

		Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

		tryAgainBtn.setOnClickListener(v -> {
			closeNoInternetDialog();
		});

		builder.setView(dialogView);

		noInternetDialog = builder.create();
		noInternetDialog.show();
	}

	private void closeNoInternetDialog() {
		if (noInternetDialog != null && noInternetDialog.isShowing()) {
			noInternetDialog.dismiss();

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
			updateConnectionStatus(isConnected);

		}
	}

	private void initializeNetworkChecker() {
		networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkChangeListener() {
			@Override
			public void onNetworkChanged(boolean isConnected) {
				updateConnectionStatus(isConnected);
			}
		});

		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
		updateConnectionStatus(isConnected);

	}

	private void updateConnectionStatus(boolean isConnected) {
		if (isConnected) {
			if (noInternetDialog != null && noInternetDialog.isShowing()) {
				noInternetDialog.dismiss();
			}
		} else {
			showNoInternetDialog();
		}
	}
}