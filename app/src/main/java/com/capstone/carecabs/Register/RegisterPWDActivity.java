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
import android.util.TypedValue;
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
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanIDActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
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

public class RegisterPWDActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "RegisterPWDActivity";
	private ActivityRegisterPwdBinding binding;
	private final String userType = "Person with Disabilities (PWD)";
	private String profilePictureURL = "default";
	private String fontSize = StaticDataPasser.storeFontSize;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private boolean isEditing = false;
	private final String[] disabilityItem = {
			"Communication Disability",
			"Vision Impairment",
			"Deaf or hard of hearing",
			"Mental health conditions",
			"Intellectual disability",
			"Acquired brain injury",
			"Autism spectrum disorder",
			"Physical disability"
	};
	private final String[] monthItem = {
			"January",
			"February",
			"March",
			"April",
			"May",
			"June",
			"July",
			"August",
			"September",
			"October",
			"November",
			"December"
	};

	private final String[] sexItem = {"Male", "Female"};
	private String sex, birthDate, month, disability;
	private int age;
	private Uri profilePictureUri;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private DocumentReference userReference;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private Intent intent, galleryIntent, cameraIntent;
	private Calendar selectedDate;
	private VoiceAssistant voiceAssistant;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, registerFailedDialog,
			cancelRegisterDialog, idNotScannedDialog, idScanInfoDialog,
			enterBirthdateDialog, cameraGalleryOptionsDialog,
			pleaseWaitDialog;
	private NetworkChangeReceiver networkChangeReceiver;

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
		checkCameraAndStoregePermission();

		FirebaseApp.initializeApp(this);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);

			binding.firstnameEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Firstname"));
			binding.lastnameEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Lastname"));
			binding.birthdateBtn.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Birthdate"));
			binding.ageEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Age"));
			binding.sexDropDownMenu.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Sex"));
			binding.disabilityDropDownMenu.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Disability"));
		}

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

		ArrayAdapter<String> disabilityAdapter =
				new ArrayAdapter<>(this, R.layout.item_dropdown, disabilityItem);
		binding.disabilityDropDownMenu.setAdapter(disabilityAdapter);
		binding.disabilityDropDownMenu.setOnItemClickListener((parent, view, position, id) ->
				disability = parent.getItemAtPosition(position).toString());

		ArrayAdapter<String> sexAdapter =
				new ArrayAdapter<>(this, R.layout.item_dropdown, sexItem);
		binding.sexDropDownMenu.setAdapter(sexAdapter);
		binding.sexDropDownMenu.setOnItemClickListener((parent, view, position, id) ->
				sex = parent.getItemAtPosition(position).toString());

		binding.birthdateBtn.setOnClickListener(v -> showEnterBirthdateDialog());

		binding.nextBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			showPleaseWaitDialog();

			String firstname = binding.firstnameEditText.getText().toString().trim();
			String lastname = binding.lastnameEditText.getText().toString().trim();
			age = Integer.parseInt(binding.ageEditText.getText().toString());

			if (firstname.isEmpty() ||
					lastname.isEmpty()
					|| birthDate == null
					|| age == 0
					|| sex == null
					|| disability == null) {

				closePleaseWaitDialog();
				Toast.makeText(this, "Please complete your Info", Toast.LENGTH_LONG).show();
				binding.progressBarLayout.setVisibility(View.GONE);

			} else {
				updateUserRegistrationToFireStore(firstname, lastname);
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
		fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

		float textSizeSP;
		float textHeaderSizeSP;
		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;

			binding.firstnameLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.lastnameLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.birthdateLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.ageLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.sexLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.disabilityLayout.setHelperTextTextAppearance(R.style.LargeHelperText);

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;

			binding.firstnameLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.lastnameLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.birthdateLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.ageLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.sexLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.disabilityLayout.setHelperTextTextAppearance(R.style.LargeHelperText);

		}

		binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.firstnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.lastnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.birthdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.ageEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.sexDropDownMenu.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityDropDownMenu.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
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

	private void updateUserRegistrationToFireStore(String firstname, String lastname) {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			userReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

			Map<String, Object> registerUser = new HashMap<>();
			registerUser.put("firstname", firstname);
			registerUser.put("lastname", lastname);
			registerUser.put("disability", disability);
			registerUser.put("age", age);
			registerUser.put("birthdate", birthDate);
			registerUser.put("sex", sex);
			registerUser.put("userType", userType);
			registerUser.put("isRegisterComplete", true);
			registerUser.put("totalTrips", 0);

			userReference.update(registerUser)
					.addOnSuccessListener(unused -> {
						closePleaseWaitDialog();
						binding.progressBarLayout.setVisibility(View.GONE);

						if (profilePictureUri != null) {
							uploadProfilePictureToFirebaseStorage(userID, profilePictureUri);
						}

						intent = new Intent(RegisterPWDActivity.this, ScanIDActivity.class);
						intent.putExtra("userType", userType);
						startActivity(intent);
						finish();

					})
					.addOnFailureListener(e -> {
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

	private void updateCancelledRegistration(String userID) {

		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		userReference.update(updateRegister)
				.addOnSuccessListener(unused -> {

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterPWDActivity.this, LoginOrRegisterActivity.class);
					startActivity(intent);
					finish();

				})
				.addOnFailureListener(e -> {

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterPWDActivity.this, LoginOrRegisterActivity.class);
					startActivity(intent);
					finish();

					Log.e(TAG, "updateCancelledRegistration: " + e.getMessage());
				});
	}

	private void updateInterruptedCancelledRegister(String userID) {
		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		userReference.update(updateRegister)
				.addOnSuccessListener(unused -> {

					FirebaseMain.signOutUser();

				})
				.addOnFailureListener(e -> {

					FirebaseMain.signOutUser();

					Log.e(TAG, e.getMessage());
				});
	}

	private void checkCameraAndStoregePermission() {
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

		if (fontSize.equals("large")) {
			float TEXT_SIZE = 20;
			dialogEnterBirthdateBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogEnterBirthdateBinding.dayEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.yearEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.monthTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.dayTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.yearTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			dialogEnterBirthdateBinding.doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);

			voiceAssistant.speak("Enter you Birthdate");

			dialogEnterBirthdateBinding.dayEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Day"));
			dialogEnterBirthdateBinding.monthTextView.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Month"));
			dialogEnterBirthdateBinding.yearEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Year"));

		}


		ArrayAdapter<String> monthAdapter =
				new ArrayAdapter<>(this, R.layout.item_dropdown, monthItem);
		dialogEnterBirthdateBinding.monthDropDownMenu.setAdapter(monthAdapter);
		dialogEnterBirthdateBinding.monthDropDownMenu.setOnItemClickListener((parent, view, position, id)
				-> {
			month = parent.getItemAtPosition(position).toString();
			dialogEnterBirthdateBinding.monthTextView.setText(month);
		});

		dialogEnterBirthdateBinding.doneBtn.setOnClickListener(view -> {
			String year = dialogEnterBirthdateBinding.yearEditText.getText().toString();
			String day = dialogEnterBirthdateBinding.dayEditText.getText().toString();

			if (month == null
					|| year.isEmpty()
					|| day.isEmpty()) {

				Toast.makeText(RegisterPWDActivity.this, "Please complete your Birthdate", Toast.LENGTH_SHORT).show();

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
				binding.ageEditText.setText(String.valueOf(age));

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
			updateUserRegistrationToFireStore(firstname, lastname);
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
			updateCancelledRegistration(FirebaseMain.getUser().getUid());
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
			binding.ageEditText.setText("Age: " + age);
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

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Please wait");
		}

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
		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", profilePictureURL);

		userReference.update(profilePicture)
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