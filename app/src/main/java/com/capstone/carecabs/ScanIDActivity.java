package com.capstone.carecabs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Register.RegisterDriverActivity;
import com.capstone.carecabs.Register.RegisterPWDActivity;
import com.capstone.carecabs.Register.RegisterSeniorActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityScanIdBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanIDActivity extends AppCompatActivity {
	private final String TAG = "ScanID";
	private final String userID = FirebaseMain.getUser().getUid();
	private String idPictureURL = "none";
	private Uri idPictureUri;
	private String getUserType;
	private boolean isUserVerified = false;
	private AlertDialog.Builder builder;
	private AlertDialog optionsDialog, cancelScanIDDialog,
			noInternetDialog, idNotScannedDialog;
	private TextRecognizer textRecognizer;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private Intent intent;
	private NetworkChangeReceiver networkChangeReceiver;
	private StorageReference storageRef;
	private StorageReference imagesRef;
	private StorageReference fileRef;
	private RequestManager requestManager;
	private ActivityScanIdBinding binding;

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closeOptionsDialog();
		closeIDNotScannedDialog();
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
		closeIDNotScannedDialog();
	}

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityScanIdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.idAlreadyScannedLayout.setVisibility(View.GONE);
		binding.backBtn.setVisibility(View.GONE);
		binding.doneBtn.setVisibility(View.GONE);

		initializeNetworkChecker();
		checkPermission();
		checkIfUserIsVerified();

		if (getIntent() != null && getIntent().hasExtra("userType")) {
			intent = getIntent();
			getUserType = intent.getStringExtra("userType");

			if (getUserType.equals("From Main")) {
				binding.imgBackBtn.setOnClickListener(view -> {
					goToMainActivity();
				});

			} else {
				binding.imgBackBtn.setOnClickListener(view -> {
					showCancelScanIDDialog();
				});

				switch (getUserType) {
					case "Driver":
						binding.scanYourIDTypeTextView.setText("Scan your Driver's License");

						break;

					case "Senior Citizen":
						binding.scanYourIDTypeTextView.setText("Scan your Senior Citizen ID that is valid by OSCA");

						break;

					case "Persons with Disability (PWD)":
						binding.scanYourIDTypeTextView.setText("Scan your valid PWD ID");

						break;
				}
			}
		}

		binding.doneBtn.setOnClickListener(v -> {
			if (idPictureUri != null) {
				uploadIDPictureToFirebaseStorage(idPictureUri);

			} else {
				showIDNotScannedDialog();
			}

		});

		binding.idScanLayout.setOnClickListener(v -> {
			ImagePicker.with(ScanIDActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start();
		});

		binding.backBtn.setOnClickListener(v -> goToMainActivity());

		binding.idAlreadyScannedLayout.setOnClickListener(v -> {
			ImagePicker.with(ScanIDActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start();
		});
	}

	@Override
	public void onBackPressed() {
		if (isUserVerified) {
			goToMainActivity();
		} else {
			showCancelScanIDDialog();
		}
	}

	private void checkIfUserIsVerified() {
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						Boolean getVerificationStatus = documentSnapshot.getBoolean("isVerified");

						getUserType = documentSnapshot.getString("userType");

						if (getVerificationStatus) {
							isUserVerified = true;

							binding.idAlreadyScannedLayout.setVisibility(View.VISIBLE);
							binding.backBtn.setVisibility(View.VISIBLE);
							binding.idScanLayout.setVisibility(View.GONE);
							binding.scanYourIDTypeTextView.setVisibility(View.GONE);

							binding.imgBackBtn.setOnClickListener(view -> {
								goToMainActivity();
							});
						}
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
	}

	private void goToMainActivity() {
		intent = new Intent(ScanIDActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
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
			startActivity(intent);
			finish();

			closeCancelScanIDDialog();

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

	private void uploadImageToFirebaseStorage(Uri imageUri) {
		fileRef = imagesRef.child(imageUri.getLastPathSegment());
		fileRef.putFile(imageUri).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {
				fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

					// Save the download URL in Firestore
					Map<String, Object> data = new HashMap<>();
					data.put("profilePicUrl", uri.toString());
					binding.idImageView.setImageURI(imageUri);

				}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
			} else {
				Log.e(TAG, String.valueOf(task.getException()));
			}
		});
	}

	private void uploadIDPictureToFirebaseStorage(Uri idPictureUri) {
		FirebaseStorage firebaseStorage = FirebaseMain.getFirebaseStorageInstance();
		StorageReference idPictureReference = firebaseStorage.getReference("images/idPictures");
		StorageReference idPicturePath = idPictureReference.child("idPictures/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		idPicturePath.putFile(idPictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					idPicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								idPictureURL = uri.toString();
								storeIDPictureURLInFireStore(idPictureURL);
							})
							.addOnFailureListener(e -> {
								Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();

								Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();

					Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
				});
	}

	private void storeIDPictureURLInFireStore(String idPictureURL) {

		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> idPicture = new HashMap<>();
		idPicture.put("idPicture", idPictureURL);

		documentReference.update(idPicture)
				.addOnSuccessListener(unused ->

						Toast.makeText(ScanIDActivity.this, "ID added successfully", Toast.LENGTH_SHORT).show())

				.addOnFailureListener(e -> {

					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "storeIDPictureURLInFireStore: " + e.getMessage());
				});
	}

	private void showIDNotScannedDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_not_scanned, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			uploadIDPictureToFirebaseStorage(idPictureUri);

			closeIDNotScannedDialog();
		});

		noBtn.setOnClickListener(v -> {
			closeIDNotScannedDialog();
		});

		builder.setView(dialogView);

		idNotScannedDialog = builder.create();
		idNotScannedDialog.show();
	}

	private void closeIDNotScannedDialog() {
		if (idNotScannedDialog != null && idNotScannedDialog.isShowing()) {
			idNotScannedDialog.dismiss();
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

	@SuppressLint("SetTextI18n")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			if (resultCode == Activity.RESULT_OK && data != null) {

				idPictureUri = data.getData();
				binding.idImageView.setImageURI(idPictureUri);

				if (isUserVerified) {
					binding.idScanLayout.setVisibility(View.VISIBLE);
					binding.scanYourIDTypeTextView.setVisibility(View.VISIBLE);
					binding.doneBtn.setVisibility(View.VISIBLE);
					binding.idAlreadyScannedLayout.setVisibility(View.GONE);
					binding.backBtn.setVisibility(View.GONE);

					switch (getUserType) {
						case "Driver":
							binding.scanYourIDTypeTextView.setText("Scan your Driver's License");

							break;

						case "Senior Citizen":
							binding.scanYourIDTypeTextView.setText("Scan your Senior Citizen ID that is valid by OSCA");

							break;

						case "Persons with Disability (PWD)":
							binding.scanYourIDTypeTextView.setText("Scan your valid PWD ID");

							break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			Log.e(TAG, "onActivityResult: " + e.getMessage());
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
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