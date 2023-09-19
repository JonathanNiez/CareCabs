package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityRegisterSeniorBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterSeniorActivity extends AppCompatActivity {
	private DocumentReference documentReference;
	private StorageReference storageReference, imageRef;
	private FirebaseStorage firebaseStorage;
	private Uri imageUri;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private String userID;
	private final String TAG = "RegisterSeniorActivity";
	private String verificationStatus = "Not Verified";
	private boolean shouldExit = false;
	private boolean isIDScanned = false;
	private Intent intent, galleryIntent, cameraIntent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog ageReqDialog, noInternetDialog,
			registerFailedDialog, cancelRegisterDialog,
			idNotScannedDialog, birthdateInputChoiceDialog,
			cameraGalleryOptionsDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityRegisterSeniorBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterSeniorBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		initializeNetworkChecker();
		checkPermission();

		FirebaseApp.initializeApp(this);

		StaticDataPasser.storeRegisterUserType = "Senior Citizen";

		binding.imgBtnProfilePic.setOnClickListener(v -> {
			showCameraOrGalleryOptionsDialog();
		});

		binding.imgBackBtn.setOnClickListener(v -> {
			showCancelRegisterDialog();
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
					String selectedSex = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedSex = selectedSex;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				binding.spinnerSex.setSelection(0);
			}
		});

		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
				this,
				R.array.senior_citizen_medical_condition,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerMedicalCondition.setAdapter(adapter1);
		binding.spinnerMedicalCondition.setSelection(0);
		binding.spinnerMedicalCondition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					binding.spinnerMedicalCondition.setSelection(0);
				} else {
					String selectedMedicalCondition = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedMedicalCondition = selectedMedicalCondition;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				binding.spinnerMedicalCondition.setSelection(0);
			}
		});

		binding.birthdateBtn.setOnClickListener(v -> {
			showEnterBirthdateDialog();
		});

		binding.scanIDBtn.setOnClickListener(view -> {
			intent = new Intent(this, ScanIDActivity.class);
			intent.putExtra("userType", StaticDataPasser.storeRegisterUserType);
			startActivity(intent);
			finish();
		});

		binding.doneBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.doneBtn.setVisibility(View.GONE);
			binding.scanIDLayout.setVisibility(View.GONE);

			String stringFirstname = binding.firstnameEditText.getText().toString().trim();
			String stringLastname = binding.lastnameEditText.getText().toString().trim();

			if (stringFirstname.isEmpty() || stringLastname.isEmpty()
					|| StaticDataPasser.storeBirthdate == null
					|| StaticDataPasser.storeCurrentAge == 0
					|| Objects.equals(StaticDataPasser.storeSelectedSex, "Select your sex")
					|| Objects.equals(StaticDataPasser.storeSelectedMedicalCondition, "Select your Medical Condition")) {

				Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();

				binding.progressBarLayout.setVisibility(View.GONE);
				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.scanIDLayout.setVisibility(View.VISIBLE);

			} else if (StaticDataPasser.storeCurrentAge <= 60) {
				intent = new Intent(this, LoginActivity.class);
				startActivity(intent);
				finish();

				showAgeReqDialog();

				updateCancelledRegister(FirebaseMain.getUser().getUid());

				binding.progressBarLayout.setVisibility(View.GONE);
				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.scanIDLayout.setVisibility(View.VISIBLE);

			} else {
				StaticDataPasser.storeFirstName = stringFirstname;
				StaticDataPasser.storeLastName = stringLastname;

				if (!isIDScanned) {
					showIDNotScannedDialog(stringFirstname, stringLastname);
				} else {
					verificationStatus = "Verified";
					updateUserRegisterToFireStore(stringFirstname, stringLastname, verificationStatus);
				}
			}
		});
	}

	@Override
	public void onBackPressed() {

		if (shouldExit) {
			super.onBackPressed(); // Exit the app
		} else {
			// Show an exit confirmation dialog
			showCancelRegisterDialog();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeRegisterFailedDialog();
		closeEnterBirthdateDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeRegisterFailedDialog();
		closeEnterBirthdateDialog();
	}

	private void updateUserRegisterToFireStore(String firstname, String lastname,
	                                           String verificationStatus) {
		userID = FirebaseMain.getUser().getUid();
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(StaticDataPasser.userCollection).document(userID);

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("firstname", firstname);
		registerUser.put("lastname", lastname);
		registerUser.put("age", StaticDataPasser.storeCurrentAge);
		registerUser.put("birthdate", StaticDataPasser.storeBirthdate);
		registerUser.put("sex", StaticDataPasser.storeSelectedSex);
		registerUser.put("userType", StaticDataPasser.storeRegisterUserType);
		registerUser.put("medicalCondition", StaticDataPasser.storeSelectedMedicalCondition);
		registerUser.put("verificationStatus", verificationStatus);
		registerUser.put("isRegisterComplete", true);
		registerUser.put("totalTrips", 0);

		documentReference.update(registerUser).addOnSuccessListener(unused -> {
			binding.progressBarLayout.setVisibility(View.GONE);
			binding.doneBtn.setVisibility(View.VISIBLE);
			binding.scanIDLayout.setVisibility(View.VISIBLE);

			uploadImageToFirebaseStorage(StaticDataPasser.storeUri);
			showRegisterSuccessNotification();

			intent = new Intent(RegisterSeniorActivity.this, MainActivity.class);
			startActivity(intent);
			finish();

		}).addOnFailureListener(e -> {
			showRegisterFailedDialog();

			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.doneBtn.setVisibility(View.VISIBLE);
			binding.scanIDLayout.setVisibility(View.VISIBLE);

		});
	}

	private void updateCancelledRegister(String userID) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(StaticDataPasser.userCollection).document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister).addOnSuccessListener(unused -> {

			FirebaseMain.signOutUser();

			intent = new Intent(RegisterSeniorActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();

		}).addOnFailureListener(e -> {

			FirebaseMain.signOutUser();

			Log.e(TAG, e.getMessage());

			intent = new Intent(RegisterSeniorActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		});
	}

	private void checkPermission() {
		// Check for camera permission
		if (ContextCompat.checkSelfPermission(RegisterSeniorActivity.this,
				android.Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterSeniorActivity.this,
					new String[]{android.Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST);
		}

		// Check for storage permission
		if (ContextCompat.checkSelfPermission(RegisterSeniorActivity.this,
				android.Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterSeniorActivity.this,
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

	private void uploadImageToFirebaseStorage(Uri imageUri) {
		firebaseStorage = FirebaseMain.getFirebaseStorageInstance();
		storageReference = firebaseStorage.getReference();

		userID = FirebaseMain.getUser().getUid();

		imageRef = storageReference.child("images/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		UploadTask uploadTask = imageRef.putFile(imageUri);
		uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
				.addOnSuccessListener(uri -> {
					String imageUrl = uri.toString();

					storeImageUrlInFireStore(imageUrl);

				}).addOnFailureListener(e -> {
					Toast.makeText(RegisterSeniorActivity.this, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, e.getMessage());

				})).addOnFailureListener(e -> {
			Toast.makeText(RegisterSeniorActivity.this, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
			Log.e(TAG, e.getMessage());

		});
	}

	private void storeImageUrlInFireStore(String imageUrl) {
		userID = FirebaseMain.getUser().getUid();
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(StaticDataPasser.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", imageUrl);

		documentReference.update(profilePicture)
				.addOnSuccessListener(unused ->
						Toast.makeText(RegisterSeniorActivity.this, "Profile picture added successfully", Toast.LENGTH_SHORT).show())
				.addOnFailureListener(e -> {
					Toast.makeText(RegisterSeniorActivity.this, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, e.getMessage());
				});
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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_enter_birthdate, null);

		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		Button doneBtn = dialogView.findViewById(R.id.doneBtn);
		TextView monthTextView = dialogView.findViewById(R.id.monthTextView);
		TextView dayTextView = dialogView.findViewById(R.id.dayTextView);
		TextView yearTextView = dialogView.findViewById(R.id.yearTextView);
		EditText yearEditText = dialogView.findViewById(R.id.yearEditText);
		EditText dayEditText = dialogView.findViewById(R.id.dayEditText);
		Spinner spinnerMonth = dialogView.findViewById(R.id.spinnerMonth);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this,
				R.array.month,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerMonth.setAdapter(adapter);
		spinnerMonth.setSelection(0);
		spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					spinnerMonth.setSelection(0);
				} else {
					String selectedMonth = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedMonth = selectedMonth;

					monthTextView.setText(selectedMonth);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				spinnerMonth.setSelection(0);
			}
		});

		dayEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				dayTextView.setText(enteredText);
			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		yearEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				yearTextView.setText(enteredText);

			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		doneBtn.setOnClickListener(view -> {
			String year = yearEditText.getText().toString();
			String day = dayEditText.getText().toString();
			if (StaticDataPasser.storeSelectedMonth.equals("Month")
					|| year.isEmpty() || day.isEmpty()) {

				Toast.makeText(RegisterSeniorActivity.this, "Please enter your Date of Birth", Toast.LENGTH_SHORT).show();
			} else {
				String fullBirthdate = StaticDataPasser.storeSelectedMonth + "-" + day + "-" + year;

				//Calculate age
				Calendar today = Calendar.getInstance();
				int age = today.get(Calendar.YEAR) - Integer.parseInt(year);

				// Check if the user's birthday has already happened this year or not
				if (today.get(Calendar.DAY_OF_YEAR) < Integer.parseInt(year)) {
					age--;
				}

				StaticDataPasser.storeBirthdate = fullBirthdate;
				StaticDataPasser.storeCurrentAge = age;

				binding.birthdateBtn.setText(fullBirthdate);
				binding.ageBtn.setText(String.valueOf(age));

				closeEnterBirthdateDialog();
			}
		});

		cancelBtn.setOnClickListener(v -> {
			closeEnterBirthdateDialog();
		});

		builder.setView(dialogView);

		birthdateInputChoiceDialog = builder.create();
		birthdateInputChoiceDialog.show();
	}

	private void closeEnterBirthdateDialog() {
		if (birthdateInputChoiceDialog != null && birthdateInputChoiceDialog.isShowing()) {
			birthdateInputChoiceDialog.dismiss();
		}
	}


	private void showIDNotScannedDialog(String firstname, String lastname) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_not_scanned, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			verificationStatus = "Not Verified";
			updateUserRegisterToFireStore(firstname, lastname, verificationStatus);
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


	private void showAgeReqDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_you_are_not_a_senior_ciitizen, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			if (ageReqDialog != null && ageReqDialog.isShowing()) {
				ageReqDialog.dismiss();
			}
		});

		builder.setView(dialogView);

		ageReqDialog = builder.create();
		ageReqDialog.show();
	}


	private void showRegisterSuccessNotification() {
		String channelId = "registration_channel_id"; // Change this to your desired channel ID
		String channelName = "CareCabs"; // Change this to your desired channel name
		int notificationId = 2; // Change this to a unique ID for each notification

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
				.setSmallIcon(R.drawable.logo)
				.setContentTitle("Registration Successful")
				.setContentText("You have successfully registered as a Senior Citizen!");

		Notification notification = builder.build();
		notificationManager.notify(notificationId, notification);
	}

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

		Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

		tryAgainBtn.setOnClickListener(v -> {
			if (noInternetDialog != null && noInternetDialog.isShowing()) {
				noInternetDialog.dismiss();

				boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
				updateConnectionStatus(isConnected);

			}
		});

		builder.setView(dialogView);

		noInternetDialog = builder.create();
		noInternetDialog.show();
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

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CAMERA_REQUEST_CODE) {
				if (data != null) {

					Bundle extras = data.getExtras();
					Bitmap imageBitmap = (Bitmap) extras.get("data");

					imageUri = getImageUri(imageBitmap);
					StaticDataPasser.storeUri = imageUri;
					binding.imgBtnProfilePic.setImageURI(imageUri);

					Toast.makeText(this, "Image is Loaded from Camera", Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
				}

			} else if (requestCode == GALLERY_REQUEST_CODE) {
				if (data != null) {

					imageUri = data.getData();
					StaticDataPasser.storeUri = imageUri;
					binding.imgBtnProfilePic.setImageURI(imageUri);

					Toast.makeText(this, "Image is Loaded from Gallery", Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
				}
			}

		} else {
			Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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