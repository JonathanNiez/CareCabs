package com.capstone.carecabs.Register;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanIDActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityRegisterPwdBinding;
import com.capstone.carecabs.databinding.DialogEnterBirthdateBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterPWDActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "RegisterPWDActivity";
	private final String userType = "Person with Disabilities (PWD)";
	private String profilePictureURL = "default";
	private String sex, birthDate, month, disability;
	private int age;
	private Uri profilePictureUri;
	private DocumentReference documentReference;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private Intent intent, galleryIntent, cameraIntent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, registerFailedDialog,
			cancelRegisterDialog, idNotScannedDialog, idScanInfoDialog,
			enterBirthdateDialog, cameraGalleryOptionsDialog,
			pleaseWaitDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityRegisterPwdBinding binding;

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeNoInternetDialog();
		closeEnterBirthdateDialog();
		closePleaseWaitDialog();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeNoInternetDialog();
		closeEnterBirthdateDialog();
		closePleaseWaitDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterPwdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		initializeNetworkChecker();
		checkPermission();

		FirebaseApp.initializeApp(this);

		binding.imgBtnProfilePic.setOnClickListener(view -> {
			ImagePicker.with(RegisterPWDActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start();
		});

		binding.backFloatingBtn.setOnClickListener(v -> showCancelRegisterDialog());

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), TAG);
		});


		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this,
				R.array.sex_options,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerSex.setAdapter(adapter);
		binding.spinnerSex.setSelection(0);
		binding.spinnerSex.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					binding.spinnerSex.setSelection(0);
				} else {
					sex = parent.getItemAtPosition(position).toString();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				binding.spinnerSex.setSelection(0);
			}
		});

		ArrayAdapter<CharSequence> disabilityAdapter = ArrayAdapter.createFromResource(
				this,
				R.array.disability_type,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerDisability.setAdapter(disabilityAdapter);
		binding.spinnerDisability.setSelection(0);
		binding.spinnerDisability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					binding.spinnerDisability.setSelection(0);
				} else {
					disability = parent.getItemAtPosition(position).toString();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				binding.spinnerDisability.setSelection(0);
			}
		});

		binding.birthdateBtn.setOnClickListener(v -> {
			showEnterBirthdateDialog();
		});

		binding.nextBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			showPleaseWaitDialog();

			String firstname = binding.firstnameEditText.getText().toString().trim();
			String lastname = binding.lastnameEditText.getText().toString().trim();

			if (firstname.isEmpty() ||
					lastname.isEmpty()
					|| birthDate == null
					|| age == 0
					|| sex == null
					|| disability == null) {

				Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();

				binding.progressBarLayout.setVisibility(View.GONE);

			} else {
				updateUserRegisterToFireStore(firstname, lastname);
			}
		});
	}

	@Override
	public void onBackPressed() {
		boolean shouldExit = false;
		if (shouldExit) {
			super.onBackPressed();
		} else {
			showCancelRegisterDialog();
		}
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		String fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

	}

	private void showIDScanInfoDialog() {
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_scan_info, null);

		builder = new AlertDialog.Builder(this);
		builder.setView(dialogView);

		Button okBtn = dialogView.findViewById(R.id.okBtn);
		Button nextBtn = dialogView.findViewById(R.id.nextBtn);

		okBtn.setOnClickListener(v -> {
			closeIDScanInfoDialog();
		});

		nextBtn.setOnClickListener(v -> {

		});

		builder.setView(dialogView);

		idScanInfoDialog = builder.create();
		idScanInfoDialog.show();
	}

	private void closeIDScanInfoDialog() {
		if (idScanInfoDialog != null & idScanInfoDialog.isShowing()) {
			idScanInfoDialog.dismiss();
		}
	}

	private void updateUserRegisterToFireStore(String firstname, String lastname) {
		if (FirebaseMain.getUser() != null) {

			String userID = FirebaseMain.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			Map<String, Object> registerUser = new HashMap<>();
			registerUser.put("firstname", firstname);
			registerUser.put("lastname", lastname);
			registerUser.put("disability", disability);
			registerUser.put("age", age);
			registerUser.put("birthdate", binding);
			registerUser.put("sex", sex);
			registerUser.put("userType", userType);
			registerUser.put("isRegisterComplete", true);
			registerUser.put("totalTrips", 0);

			documentReference.update(registerUser).addOnSuccessListener(unused -> {
				binding.progressBarLayout.setVisibility(View.GONE);

				closePleaseWaitDialog();
				if (profilePictureUri != null) {
					uploadProfilePictureToFirebaseStorage(userID, profilePictureUri);
				}

				intent = new Intent(RegisterPWDActivity.this, ScanIDActivity.class);
				intent.putExtra("userType", userType);
				startActivity(intent);
				finish();

			}).addOnFailureListener(e -> {
				closePleaseWaitDialog();
				showRegisterFailedDialog();

				binding.progressBarLayout.setVisibility(View.GONE);

				Log.e(TAG, "updateUserRegisterToFireStore: " + e.getMessage());
			});

		} else {
			Log.e(TAG, "updateUserRegisterToFireStore: user in null");

			intent = new Intent(RegisterPWDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void updateCancelledRegister(String userID) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister).addOnSuccessListener(unused -> {

			FirebaseMain.signOutUser();

			intent = new Intent(RegisterPWDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();

		}).addOnFailureListener(e -> {

			FirebaseMain.signOutUser();

			Log.e(TAG, e.getMessage());

			intent = new Intent(getApplicationContext(), LoginActivity.class);
			startActivity(intent);
			finish();
		});
	}

	private void updateInterruptedCancelledRegister(String userID) {
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister).addOnSuccessListener(unused -> {

			FirebaseMain.signOutUser();

		}).addOnFailureListener(e -> {

			FirebaseMain.signOutUser();

			Log.e(TAG, e.getMessage());
		});
	}

	private void checkPermission() {
		// Check for camera permission
		if (ContextCompat.checkSelfPermission(RegisterPWDActivity.this,
				android.Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterPWDActivity.this,
					new String[]{android.Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST);
		}

		// Check for storage permission
		if (ContextCompat.checkSelfPermission(RegisterPWDActivity.this,
				android.Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterPWDActivity.this,
					new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST);
		}
	}

	private void openGallery() {
		galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		galleryIntent.setType("image/*");
		if (galleryIntent.resolveActivity(this.getPackageManager()) != null) {
			startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(this, "Opened Gallery", Toast.LENGTH_LONG).show();

		} else {
			Toast.makeText(this, "No gallery app found", Toast.LENGTH_LONG).show();
		}
	}

	private void openCamera() {
		cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (cameraIntent.resolveActivity(this.getPackageManager()) != null) {
			startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(this, "Opened Camera", Toast.LENGTH_LONG).show();


		} else {
			Toast.makeText(this, "No camera app found", Toast.LENGTH_LONG).show();
		}
	}

	private Uri getImageUri(Bitmap bitmap) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "Title", null);
		return Uri.parse(path);
	}

	private void showCameraOrGalleryOptionsDialog() {
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
			closeCameraOrGalleryOptionsDialog();
		});

		builder.setView(dialogView);

		cameraGalleryOptionsDialog = builder.create();
		cameraGalleryOptionsDialog.show();
	}

	private void closeCameraOrGalleryOptionsDialog() {
		if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
			cameraGalleryOptionsDialog.dismiss();
		}
	}

	private void showEnterBirthdateDialog() {
		builder = new AlertDialog.Builder(this);

		DialogEnterBirthdateBinding dialogEnterBirthdateBinding =
				DialogEnterBirthdateBinding.inflate(getLayoutInflater());
		View dialogView = dialogEnterBirthdateBinding.getRoot();

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this,
				R.array.month,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dialogEnterBirthdateBinding.spinnerMonth.setAdapter(adapter);
		dialogEnterBirthdateBinding.spinnerMonth.setSelection(0);
		dialogEnterBirthdateBinding.spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					dialogEnterBirthdateBinding.spinnerMonth.setSelection(0);
				} else {
					month = parent.getItemAtPosition(position).toString();
					dialogEnterBirthdateBinding.monthTextView.setText(month);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				dialogEnterBirthdateBinding.spinnerMonth.setSelection(0);
			}
		});

		dialogEnterBirthdateBinding.dayEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				dialogEnterBirthdateBinding.dayTextView.setText(enteredText);
			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		dialogEnterBirthdateBinding.yearEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				dialogEnterBirthdateBinding.yearTextView.setText(enteredText);

			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		dialogEnterBirthdateBinding.doneBtn.setOnClickListener(view -> {
			String year = dialogEnterBirthdateBinding.yearEditText.getText().toString();
			String day = dialogEnterBirthdateBinding.dayEditText.getText().toString();

			if (month == null
					|| year.isEmpty()
					|| day.isEmpty()) {

				Toast.makeText(RegisterPWDActivity.this, "Please enter your Date of Birth", Toast.LENGTH_SHORT).show();

			} else {
				birthDate = month + "-" + day + "-" + year;

				//Calculate age
				Calendar today = Calendar.getInstance();
				age = today.get(Calendar.YEAR) - Integer.parseInt(year);

				// Check if the user's birthday has already happened this year or not
				if (today.get(Calendar.DAY_OF_YEAR) < Integer.parseInt(year)) {
					age--;
				}

				binding.birthdateBtn.setText(birthDate);
				binding.ageBtn.setText(String.valueOf(age));

				closeEnterBirthdateDialog();
			}
		});

		dialogEnterBirthdateBinding.cancelBtn.setOnClickListener(v -> closeEnterBirthdateDialog());

		builder.setView(dialogView);

		enterBirthdateDialog = builder.create();
		enterBirthdateDialog.show();
	}

	private void closeEnterBirthdateDialog() {
		if (enterBirthdateDialog != null && enterBirthdateDialog.isShowing()) {
			enterBirthdateDialog.dismiss();
		}
	}

	private void showIDNotScannedDialog(String firstname, String lastname) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_not_scanned, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			updateUserRegisterToFireStore(firstname, lastname);
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

	private void showCancelRegisterDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_register, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			updateCancelledRegister(FirebaseMain.getUser().getUid());
		});

		noBtn.setOnClickListener(v -> {
			closeCancelRegisterDialog();
		});


		builder.setView(dialogView);

		cancelRegisterDialog = builder.create();
		cancelRegisterDialog.show();
	}

	private void closeCancelRegisterDialog() {
		if (cancelRegisterDialog != null && cancelRegisterDialog.isShowing()) {
			cancelRegisterDialog.dismiss();
		}
	}

	@SuppressLint("SetTextI18n")
	private void calculateAge() {
		if (selectedDate != null) {
			// Calculate the age based on the selected birthdate
			Calendar today = Calendar.getInstance();
			int age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR);

			// Check if the user's birthday has already happened this year or not
			if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
				age--;
			}

			// Update the ageTextView with the calculated age
			binding.ageBtn.setText("Age: " + age);
			StaticDataPasser.storeCurrentAge = age;
		}
	}

	private void showRegisterSuccessNotification() {
		String channelId = "registration_channel_id"; // Change this to your desired channel ID
		String channelName = "CareCabs"; // Change this to your desired channel name
		int notificationId = 3; // Change this to a unique ID for each notification

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle("Registration Successful")
				.setContentText("You have successfully registered as a PWD");

		Notification notification = builder.build();
		notificationManager.notify(notificationId, notification);
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

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(this);

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

	private void showRegisterFailedDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeRegisterFailedDialog();
		});

		builder.setView(dialogView);

		registerFailedDialog = builder.create();
		registerFailedDialog.show();

	}

	private void closeRegisterFailedDialog() {
		if (registerFailedDialog != null && registerFailedDialog.isShowing()) {
			registerFailedDialog.dismiss();
		}
	}

	private void uploadProfilePictureToFirebaseStorage(String userID, Uri profilePictureUri) {

		StorageReference storageReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		StorageReference profilePicturePath = storageReference.child("images/profilePictures/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		UploadTask uploadTask = profilePicturePath.putFile(profilePictureUri);
		uploadTask.addOnSuccessListener(taskSnapshot -> profilePicturePath.getDownloadUrl()
				.addOnSuccessListener(uri -> {

					profilePictureURL = uri.toString();
					storeProfilePictureUrlInFireStore(userID, profilePictureURL);

				}).addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Log.e(TAG, "uploadImageToFirebaseStorage: " + e.getMessage());

				})).addOnFailureListener(e -> {
			closePleaseWaitDialog();

			Log.e(TAG, "uploadImageToFirebaseStorage: " + e.getMessage());

		});
	}

	private void storeProfilePictureUrlInFireStore(String userID, String profilePictureURL) {
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", profilePictureURL);

		documentReference.update(profilePicture)
				.addOnSuccessListener(unused ->
						Log.i(TAG, "storeProfilePictureUrlInFireStore: addOnSuccessListener"))
				.addOnFailureListener(e -> {
					Log.e(TAG, "storeImageUrlInFireStore: " + e.getMessage());
				});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && data != null) {

			profilePictureUri = data.getData();
			binding.imgBtnProfilePic.setImageURI(profilePictureUri);


//			if (requestCode == CAMERA_REQUEST_CODE) {
//				if (data != null) {
//
//					Bundle extras = data.getExtras();
//					Bitmap imageBitmap = (Bitmap) extras.get("data");
//
//					imageUri = getImageUri(imageBitmap);
//					StaticDataPasser.storeUri = imageUri;
//					binding.imgBtnProfilePic.setImageURI(imageUri);
//
//					Toast.makeText(this, "Image is Loaded from Camera", Toast.LENGTH_LONG).show();
//
//				} else {
//					Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
//				}
//
//			} else if (requestCode == GALLERY_REQUEST_CODE) {
//				if (data != null) {
//
//					imageUri = data.getData();
//					StaticDataPasser.storeUri = imageUri;
//					binding.imgBtnProfilePic.setImageURI(imageUri);
//
//					Toast.makeText(this, "Image is Loaded from Gallery", Toast.LENGTH_LONG).show();
//
//				} else {
//					Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
//				}
//			}

		} else {
			Log.d(TAG, "onActivityResult: image is not selected");
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

}