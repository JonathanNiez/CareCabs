package com.capstone.carecabs.Register;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
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
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanIDActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityRegisterDriverBinding;
import com.capstone.carecabs.databinding.DialogEnterBirthdateBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterDriverActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "RegisterDriver";
	private ActivityRegisterDriverBinding binding;
	private final String userType = "Driver";
	private String profilePictureURL = "default";
	private String fontSize = StaticDataPasser.storeFontSize;
	private final String[] sexItem = {"Male", "Female"};
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

	private String sex, birthDate, month;
	private int age;
	private Uri profilePictureUri;
	private String vehiclePictureURL = "none";
	private Uri vehiclePictureUri;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private DocumentReference documentReference;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private static final int PROFILE_PICTURE_REQUEST_CODE = 103;
	private static final int VEHICLE_PICTURE_REQUEST_CODE = 104;
	private Intent intent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, registerFailedDialog,
			idNotScannedDialog, cancelRegisterDialog, enterBirthdateDialog,
			cameraGalleryOptionsDialog, pleaseWaitDialog;
	private NetworkChangeReceiver networkChangeReceiver;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeRegisterFailedDialog();
		closeEnterBirthdateDialog();
		closePleaseWaitDialog();
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
		closePleaseWaitDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterDriverBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		checkCameraAndStoragePermission();

		FirebaseApp.initializeApp(this);

		binding.imgBtnProfilePic.setOnClickListener(view -> {
			ImagePicker.with(RegisterDriverActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start(PROFILE_PICTURE_REQUEST_CODE);
		});

		binding.vehicleImageView.setOnClickListener(v -> {
			ImagePicker.with(RegisterDriverActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start(VEHICLE_PICTURE_REQUEST_CODE);
		});

		binding.backFloatingBtn.setOnClickListener(v -> showCancelRegisterDialog());

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), TAG);
		});

		ArrayAdapter<String> sexAdapter =
				new ArrayAdapter<>(this, R.layout.item_dropdown, sexItem);
		binding.sexDropDownMenu.setAdapter(sexAdapter);
		binding.sexDropDownMenu.setOnItemClickListener((parent, view, position, id) ->
				sex = parent.getItemAtPosition(position).toString());

		binding.birthdateBtn.setOnClickListener(v -> showEnterBirthdateDialog());

		binding.backFloatingBtn.setOnClickListener(v -> checkEditTextIfNotEmpty());

		binding.settingsFloatingBtn.setOnClickListener(v -> showSettingsBottomSheet());

		binding.nextBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();
			binding.progressBarLayout.setVisibility(View.VISIBLE);

			String firstname = binding.firstnameEditText.getText().toString().trim();
			String lastname = binding.lastnameEditText.getText().toString().trim();
			age = Integer.parseInt(binding.ageEditText.getText().toString());
			String plateNumber = binding.vehiclePlateNumberEditText.getText().toString().trim();
			String vehicleColor = binding.vehicleColorEditText.getText().toString().trim();

			if (firstname.isEmpty() ||
					lastname.isEmpty() ||
					birthDate == null ||
					age == 0 ||
					sex == null) {

				closePleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.GONE);
				Toast.makeText(this, "Please enter your info", Toast.LENGTH_LONG).show();

			} else if (plateNumber.isEmpty() || vehicleColor.isEmpty()) {

				closePleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.GONE);
				Toast.makeText(this, "Please enter your vehicle info", Toast.LENGTH_LONG).show();

			} else {
				updateUserRegistrationToFireStore(
						firstname,
						lastname,
						vehicleColor,
						plateNumber);
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

	private void checkEditTextIfNotEmpty() {
		String firstname = binding.firstnameEditText.getText().toString().trim();
		String lastname = binding.lastnameEditText.getText().toString().trim();
		String vehicleColor = binding.vehicleColorEditText.getText().toString();
		String vehiclePlateNumber = binding.vehiclePlateNumberEditText.getText().toString().trim();

		if (!firstname.isEmpty() ||
				!lastname.isEmpty() ||
				!vehicleColor.isEmpty() ||
				!vehiclePlateNumber.isEmpty() ||
				birthDate != null ||
				age == 0 ||
				sex != null) {
			showCancelRegisterDialog();
		} else {
			intent = new Intent(RegisterDriverActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void updateUserRegistrationToFireStore(String firstname,
	                                               String lastname,
	                                               String vehicleColor,
	                                               String vehiclePlateNumber) {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

			Map<String, Object> registerUser = new HashMap<>();
			registerUser.put("profilePicture", profilePictureURL);
			registerUser.put("firstname", firstname);
			registerUser.put("lastname", lastname);
			registerUser.put("age", age);
			registerUser.put("isAvailable", true);
			registerUser.put("birthdate", birthDate);
			registerUser.put("sex", sex);
			registerUser.put("userType", userType);
			registerUser.put("driverRatings", 0.0);
			registerUser.put("passengersTransported", 0);
			registerUser.put("isRegisterComplete", true);
			registerUser.put("vehicleColor", vehicleColor);
			registerUser.put("vehiclePlateNumber", vehiclePlateNumber);
			registerUser.put("vehiclePicture", vehiclePictureURL);
			registerUser.put("navigationStatus", "idle");
			registerUser.put("destinationLatitude", 0.0);
			registerUser.put("destinationLongitude", 0.0);
			registerUser.put("bookingID", "none");
			registerUser.put("tripID", "none");
			registerUser.put("usersRated", 0);

			documentReference.update(registerUser)
					.addOnSuccessListener(unused -> {
						binding.progressBarLayout.setVisibility(View.GONE);
						closePleaseWaitDialog();

						if (profilePictureUri != null) {
							uploadProfilePictureToFirebaseStorage(userID, profilePictureUri);

						} else if (vehiclePictureUri != null) {
							uploadVehiclePictureToFirebaseStorage(userID, vehiclePictureUri);
						}

						intent = new Intent(RegisterDriverActivity.this, ScanIDActivity.class);
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
			closePleaseWaitDialog();
			showRegisterFailedDialog();

			intent = new Intent(RegisterDriverActivity.this, LoginOrRegisterActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();

			Log.e(TAG, "updateUserRegisterToFireStore: user in null");
		}
	}

	private void showSettingsBottomSheet() {
		SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
		settingsBottomSheet.setFontSizeChangeListener(this);
		settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
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

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;

			binding.firstnameLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.lastnameLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.birthdateLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.ageLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.sexLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
		}

		binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.firstnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.lastnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.birthdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.ageEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.sexDropDownMenu.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView3.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView4.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView5.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleColorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehiclePlateNumberEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void updateCancelledRegistration(String userID) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister)
				.addOnSuccessListener(unused -> {

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterDriverActivity.this, LoginOrRegisterActivity.class);
					startActivity(intent);
					finish();

				})
				.addOnFailureListener(e -> {

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterDriverActivity.this, LoginOrRegisterActivity.class);
					startActivity(intent);
					finish();

					Log.e(TAG, "updateCancelledRegister: " + e.getMessage());
				});
	}

	private void updateInterruptedCancelledRegister(String userID) {
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister)
				.addOnSuccessListener(unused -> {

					FirebaseMain.signOutUser();
					Log.i(TAG, "updateInterruptedCancelledRegister: success");

				})
				.addOnFailureListener(e -> {

					FirebaseMain.signOutUser();

					Log.e(TAG, "updateInterruptedCancelledRegister: " + e.getMessage());
				});
	}

	private void showIDNotScannedDialog(String firstname,
	                                    String lastname,
	                                    String vehicleColor,
	                                    String vehiclePlateNumber) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_not_scanned, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			updateUserRegistrationToFireStore(
					firstname,
					lastname,
					vehicleColor,
					vehiclePlateNumber);
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

	private void checkCameraAndStoragePermission() {
		// Check for camera permission
		if (ContextCompat.checkSelfPermission(RegisterDriverActivity.this,
				android.Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterDriverActivity.this,
					new String[]{android.Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST);
		}

		// Check for storage permission
		if (ContextCompat.checkSelfPermission(RegisterDriverActivity.this,
				android.Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(RegisterDriverActivity.this,
					new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST);
		}
	}

	private void openGallery() {
		intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		if (intent.resolveActivity(this.getPackageManager()) != null) {
			startActivityForResult(intent, GALLERY_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(this, "Opened Gallery", Toast.LENGTH_LONG).show();

		} else {
			Toast.makeText(this, "No gallery app found", Toast.LENGTH_LONG).show();
		}
	}

	private void openCamera() {
		intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(this.getPackageManager()) != null) {
			startActivityForResult(intent, CAMERA_REQUEST_CODE);
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

	private void showCancelRegisterDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_register, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> updateCancelledRegistration(FirebaseMain.getUser().getUid()));

		noBtn.setOnClickListener(v -> closeCancelRegisterDialog());


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

	private void showDatePickerDialog() {
		final Calendar currentDate = Calendar.getInstance();
		int year = currentDate.get(Calendar.YEAR);
		int month = currentDate.get(Calendar.MONTH);
		int day = currentDate.get(Calendar.DAY_OF_MONTH);

		@SuppressLint("SetTextI18n") DatePickerDialog datePickerDialog = new DatePickerDialog(this,
				(view, year1, monthOfYear, dayOfMonth) -> {
					selectedDate = Calendar.getInstance();
					selectedDate.set(year1, monthOfYear, dayOfMonth);

					// Update the birthdateTextView with the selected date in a desired format
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
					binding.birthdateBtn.setText("Birthdate: " + dateFormat.format(selectedDate.getTime()));

					// Calculate the age and display it
					calculateAge();
					StaticDataPasser.storeCurrentBirthDate = dateFormat.format(selectedDate.getTime());

				}, year, month, day);


		datePickerDialog.show();
	}

	private void showEnterBirthdateDialog() {
		builder = new AlertDialog.Builder(this);

		DialogEnterBirthdateBinding dialogEnterBirthdateBinding =
				DialogEnterBirthdateBinding.inflate(getLayoutInflater());
		View dialogView = dialogEnterBirthdateBinding.getRoot();

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

			if (month == null ||
					year.isEmpty() ||
					day.isEmpty()) {

				Toast.makeText(this, "Please complete your Birthdate", Toast.LENGTH_SHORT).show();

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

	private void showRegisterSuccessNotification() {
		String channelId = "registration_channel_id"; // Change this to your desired channel ID
		String channelName = "CareCabs"; // Change this to your desired channel name
		int notificationId = 1; // Change this to a unique ID for each notification

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
				.setSmallIcon(R.drawable.logo_2_v2)
				.setContentTitle("Registration Successful")
				.setContentText("You have successfully registered as a Driver!");

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

	private void uploadProfilePictureToFirebaseStorage(String userID, Uri profilePictureUri) {

		StorageReference storageReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		StorageReference profilePicturePath = storageReference.child("images/profilePictures/"
				+ System.currentTimeMillis() + "_" + userID + ".jpg");

		profilePicturePath.putFile(profilePictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					profilePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								profilePictureURL = uri.toString();
								storeProfilePictureURLInFireStore(userID, profilePictureURL);
							})
							.addOnFailureListener(e -> {
								closePleaseWaitDialog();

								Log.e(TAG, "uploadProfileImageToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Log.e(TAG, "uploadProfileImageToFirebaseStorage: " + e.getMessage());
				});
	}

	private void storeProfilePictureURLInFireStore(String userID, String profilePictureURL) {
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", profilePictureURL);

		documentReference.update(profilePicture)
				.addOnSuccessListener(unused ->

						Log.d(TAG, "storeProfilePictureURLInFireStore: addOnSuccessListener"))

				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Log.e(TAG, "storeProfilePictureURLInFireStore: " + e.getMessage());
				});
	}

	private void uploadVehiclePictureToFirebaseStorage(String userID, Uri vehiclePictureUri) {
		StorageReference storageReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		StorageReference vehiclePicturePath = storageReference.child("images/vehiclePictures/"
				+ System.currentTimeMillis() + "_" + userID + ".jpg");

		vehiclePicturePath.putFile(vehiclePictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					vehiclePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								vehiclePictureURL = uri.toString();
								storeVehiclePictureURLInFireStore(userID, vehiclePictureURL);
							})
							.addOnFailureListener(e -> {
								Log.e(TAG, "uploadVehiclePictureToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> Log.e(TAG, "uploadVehiclePictureToFirebaseStorage: " + e.getMessage())
				);
	}

	private void storeVehiclePictureURLInFireStore(String userID, String vehiclePictureURL) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> vehiclePicture = new HashMap<>();
		vehiclePicture.put("vehiclePicture", vehiclePictureURL);

		documentReference.update(vehiclePicture)
				.addOnSuccessListener(unused -> {
					Log.i(TAG, "storeVehiclePictureURLInFireStore: vehicle picture uploaded successfully");
				})
				.addOnFailureListener(e -> {
					Log.e(TAG, "storeVehiclePictureURLInFireStore: " + e.getMessage());
				});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && data != null) {

			if (requestCode == PROFILE_PICTURE_REQUEST_CODE) {

				profilePictureUri = data.getData();
				binding.imgBtnProfilePic.setImageURI(profilePictureUri);

			} else if (requestCode == VEHICLE_PICTURE_REQUEST_CODE) {

				vehiclePictureUri = data.getData();
				binding.vehicleImageView.setImageURI(vehiclePictureUri);
			}
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