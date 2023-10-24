package com.capstone.carecabs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
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
import com.capstone.carecabs.ml.IdScanV2;
import com.capstone.carecabs.ml.IdScanner;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.ktx.Firebase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.text.TextRecognizer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanIDActivity extends AppCompatActivity {
	private final String TAG = "ScanID";
	private String idPictureURL = "none";
	private Uri idPictureUri = null;
	private String getUserType;
	private final int imageSize = 224;
	private boolean isUserVerified = false;
	private AlertDialog.Builder builder;
	private AlertDialog optionsDialog, cancelScanIDDialog, notAnIDDialog,
			noInternetDialog, uploadClearIDPictureDialog, pleaseWaitDialog;
	private TextRecognizer textRecognizer;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private Intent intent;
	private NetworkChangeReceiver networkChangeReceiver;
	private DocumentReference documentReference;
	private ActivityScanIdBinding binding;

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closeOptionsDialog();
		closePleaseWaitDialog();
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
		closePleaseWaitDialog();
	}

	@Override
	protected void onStart() {
		super.onStart();
		initializeNetworkChecker();

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
		binding.verifiedText.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		checkIfUserIsVerified();
		checkCameraAndStoragePermission();

		if (getIntent() != null) {
			if (getIntent().hasExtra("userType")) {
				intent = getIntent();
				getUserType = intent.getStringExtra("userType");

				if (getUserType != null) {

					binding.imgBackBtn.setOnClickListener(v -> {
						if (isUserVerified) {
							goToMainActivity();
						} else {
							showCancelScanIDDialog();
						}
					});

					binding.backBtn.setOnClickListener(v -> {
						if (isUserVerified) {
							goToMainActivity();
						} else {
							showCancelScanIDDialog();
						}
					});

					switch (getUserType) {
						case "Driver":
							binding.scanYourIDTypeTextView.setText("Scan your Driver's license");

							break;

						case "Senior Citizen":
							binding.scanYourIDTypeTextView.setText("Scan your Senior Citizen ID that is validated by OSCA");

							break;

						case "Person with Disabilities (PWD)":
							binding.scanYourIDTypeTextView.setText("Scan your PWD ID");

							break;
					}

				}
			}
		}

		binding.doneBtn.setOnClickListener(v -> {
			if (idPictureUri != null) {
				updateVerificationStatus(idPictureUri);
			}
		});


		binding.idScanLayout.setOnClickListener(v -> {
			ImagePicker.with(ScanIDActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start();
		});

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
			super.onBackPressed();

		} else {
			showCancelScanIDDialog();
		}
	}

	@SuppressLint("DefaultLocale")
	private void classifyID(Bitmap bitmap) {
		try {
			IdScanV2 model = IdScanV2.newInstance(ScanIDActivity.this);

			// Creates inputs for reference.
			TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
			byteBuffer.order(ByteOrder.nativeOrder());

			int[] intValues = new int[imageSize * imageSize];
			bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
			int pixel = 0;
			for (int n = 0; n < imageSize; n++) {
				for (int i = 0; i < imageSize; i++) {
					int val = intValues[pixel++];
					byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
					byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
					byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
				}
			}
			inputFeature0.loadBuffer(byteBuffer);

			// Runs model inference and gets result.
			IdScanV2.Outputs outputs = model.process(inputFeature0);
			TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
			float[] confidences = outputFeature0.getFloatArray();
			int maxPos = 0;
			float maxConfidence = 0;
			float confidenceThreshold = 0.8F;

			for (int i = 0; i < confidences.length; i++) {
				if (confidences[i] > maxConfidence) {
					maxConfidence = confidences[i];
					maxPos = i;
				}
			}

			String[] classes = {"Driver's License", "Senior Citizen ID", "PWD ID", "Not an ID"};

			if (maxConfidence > confidenceThreshold) {
				String predictedClass = classes[maxPos];

				switch (getUserType) {
					case "Driver":
						handleDriverLicense(predictedClass);

						break;

					case "Senior Citizen":
						handleSeniorCitizenID(predictedClass);

						break;

					case "Person with Disabilities (PWD)":
						handlePWDID(predictedClass);

						break;
				}
			} else {
				showUploadClearIDPictureDialog();
			}

			StringBuilder s = new StringBuilder();
			for (int i = 0; i < classes.length; i++) {
				s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100));
			}

//			binding.confidenceTextView.setText(s.toString());

			// Releases model resources if no longer used.
			model.close();

		} catch (IOException e) {
			Log.e(TAG, "classifyID: " + e.getMessage());
		}
	}

	private void handleDriverLicense(String predictedClass) {
		switch (predictedClass) {
			case "Driver's License":

				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.verifiedText.setVisibility(View.VISIBLE);

				break;
			case "Senior Citizen ID":
			case "PWD ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;
		}
	}

	private void handleSeniorCitizenID(String predictedClass) {
		switch (predictedClass) {
			case "Driver's License":
			case "PWD ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;

			case "Senior Citizen ID":

				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.verifiedText.setVisibility(View.VISIBLE);

				break;
		}
	}

	private void handlePWDID(String predictedClass) {
		switch (predictedClass) {
			case "Driver's License":
			case "Senior Citizen ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;

			case "PWD ID":

				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.verifiedText.setVisibility(View.VISIBLE);

				break;
		}
	}

	private void updateVerificationStatus(Uri idPictureUri) {
		if (FirebaseMain.getUser() != null) {
			showPleaseWaitDialog();

			String userID = FirebaseMain.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			Map<String, Object> updateUser = new HashMap<>();
			updateUser.put("isVerified", true);

			documentReference.update(updateUser)
					.addOnSuccessListener(unused -> uploadIDPictureToFirebaseStorage(userID, idPictureUri))
					.addOnFailureListener(e -> {

						closePleaseWaitDialog();

						Log.e(TAG, "updateVerificationStatus: onFailure " + e.getMessage());

					});
		} else {
			Log.e(TAG, "updateVerificationStatus: current user is null");

			intent = new Intent(ScanIDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void updateUserToNotVerified() {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			Map<String, Object> updateUser = new HashMap<>();
			updateUser.put("isVerified", false);
			updateUser.put("idPicture", idPictureURL);

			documentReference.update(updateUser)
					.addOnSuccessListener(unused -> {

						goToMainActivity();

					})
					.addOnFailureListener(e -> {

						closePleaseWaitDialog();

						Log.e(TAG, "updateVerificationStatus: onFailure " + e.getMessage());

					});
		} else {
			Log.e(TAG, "updateVerificationStatus: current user is null");

			intent = new Intent(ScanIDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}

	}

	private void resetImageViewAndShowDialog() {
		binding.idImageView.setImageResource(R.drawable.face_id_100);
		binding.doneBtn.setVisibility(View.GONE);
		binding.verifiedText.setVisibility(View.GONE);

		idPictureUri = null;
		showNotAnIDDialog();
	}

	private void checkIfUserIsVerified() {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

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
					.addOnFailureListener(e -> Log.e(TAG, "checkIfUserIsVerified: onFailure " + e.getMessage()));
		}
	}

	private void checkCameraAndStoragePermission() {
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
			updateUserToNotVerified();
		});

		noBtn.setOnClickListener(v -> closeCancelScanIDDialog());

		builder.setView(dialogView);
		cancelScanIDDialog = builder.create();
		if (!isFinishing() && !isDestroyed()) {
			cancelScanIDDialog.show();
		}
	}

	private void closeCancelScanIDDialog() {
		if (cancelScanIDDialog != null && cancelScanIDDialog.isShowing()) {
			cancelScanIDDialog.isShowing();
		}
	}

	private void showNotAnIDDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_not_an_id, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			closeNotAnIDDialog();
		});

		builder.setView(dialogView);

		notAnIDDialog = builder.create();
		notAnIDDialog.show();
	}

	private void closeNotAnIDDialog() {
		if (notAnIDDialog != null && notAnIDDialog.isShowing()) {
			notAnIDDialog.dismiss();
		}
	}

	private void showUploadClearIDPictureDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_clear_id_picture, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			closeUploadClearIDPictureDialog();
		});

		builder.setView(dialogView);

		uploadClearIDPictureDialog = builder.create();
		uploadClearIDPictureDialog.show();
	}

	private void closeUploadClearIDPictureDialog() {
		if (uploadClearIDPictureDialog != null && uploadClearIDPictureDialog.isShowing()) {
			uploadClearIDPictureDialog.dismiss();
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

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_please_wait, null);

		builder.setView(dialogView);

		pleaseWaitDialog = builder.create();
		pleaseWaitDialog.show();
	}

	private void closePleaseWaitDialog() {
		if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
			pleaseWaitDialog.dismiss();
		}
	}

	private boolean matchesPattern(String text, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);
		return m.find();
	}

	private void uploadIDPictureToFirebaseStorage(String userID, Uri idPictureUri) {

		StorageReference idPictureReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		StorageReference idPicturePath = idPictureReference.child("images/idPictures/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		idPicturePath.putFile(idPictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					idPicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								closePleaseWaitDialog();

								idPictureURL = uri.toString();
								storeIDPictureURLInFireStore(userID, idPictureURL);

								intent = new Intent(ScanIDActivity.this, MainActivity.class);
								startActivity(intent);
								finish();

							})
							.addOnFailureListener(e -> {
								closePleaseWaitDialog();

								Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();

								Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();

					Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
				});

	}

	private void storeIDPictureURLInFireStore(String userID, String idPictureURL) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> idPicture = new HashMap<>();
		idPicture.put("idPicture", idPictureURL);

		documentReference.update(idPicture)
				.addOnSuccessListener(unused ->

						Log.d(TAG, "storeIDPictureURLInFireStore: addOnSuccessListener"))

				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "storeIDPictureURLInFireStore: " + e.getMessage());
				});
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

				Bitmap bitmap;
				try {
					bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), idPictureUri);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
				bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
				bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);

				classifyID(bitmap);

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
							binding.scanYourIDTypeTextView.setText("Scan your Senior Citizen ID that is validated by OSCA");

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