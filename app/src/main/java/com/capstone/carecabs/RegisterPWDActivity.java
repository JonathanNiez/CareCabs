package com.capstone.carecabs;

import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityRegisterPwdBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RegisterPWDActivity extends AppCompatActivity {
	private DocumentReference documentReference;
	private String userID;
	private final String TAG = "RegisterPWDActivity";
	private String verificationStatus = "Not Verified";
	private boolean shouldExit = false;
	private boolean isIDScanned = false;
	private Intent intent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, registerFailedDialog,
			cancelRegisterDialog, idNotScannedDialog, idScanInfoDialog,
			birthdateInputChoiceDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityRegisterPwdBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterPwdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		initializeNetworkChecker();

		FirebaseApp.initializeApp(this);

		StaticDataPasser.storeRegisterUserType = "Persons with Disabilities (PWD)";

		binding.imgBackBtn.setOnClickListener(v -> {
			showCancelRegisterDialog();
		});

		binding.helpImgBtn.setOnClickListener(v -> {
			showIDScanInfoDialog();
		});

		binding.scanIDBtn.setOnClickListener(view -> {
			intent = new Intent(RegisterPWDActivity.this, ScanIDActivity.class);
			startActivity(intent);
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

		ArrayAdapter<CharSequence> disabilityAdapter = ArrayAdapter.createFromResource(
				this,
				R.array.disability_type,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerDisability.setVisibility(View.VISIBLE);
		binding.spinnerDisability.setAdapter(disabilityAdapter);
		binding.spinnerDisability.setSelection(0);
		binding.spinnerDisability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					binding.spinnerDisability.setSelection(0);
				} else {
					String selectedDisability = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedDisability = selectedDisability;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				binding.spinnerDisability.setSelection(0);
			}
		});

		binding.birthdateBtn.setOnClickListener(v -> {
			showBirthdateInputChoiceDialog();
		});

		binding.doneBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.doneBtn.setVisibility(View.GONE);
			binding.scanIDLayout.setVisibility(View.GONE);

			String stringFirstname = binding.firstname.getText().toString().trim();
			String stringLastname = binding.lastname.getText().toString().trim();

			if (stringFirstname.isEmpty() || stringLastname.isEmpty()
					|| StaticDataPasser.storeCurrentBirthDate == null
					|| StaticDataPasser.storeCurrentAge == 0
					|| Objects.equals(StaticDataPasser.storeSelectedSex, "Select your sex")
					|| Objects.equals(StaticDataPasser.storeSelectedDisability, "Select your Disability")) {
				Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.doneBtn.setVisibility(View.VISIBLE);
				binding.scanIDLayout.setVisibility(View.VISIBLE);

			} else {
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
		closeIDScanInfoDialog();
		closeNoInternetDialog();
		closeBirthdateInputChoiceDialog();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelRegisterDialog();
		closeIDNotScannedDialog();
		closeIDScanInfoDialog();
		closeNoInternetDialog();
		closeBirthdateInputChoiceDialog();
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

	private void updateUserRegisterToFireStore(String firstname, String lastname,
	                                           String verificationStatus) {
		userID = FirebaseMain.getUser().getUid();
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(StaticDataPasser.userCollection).document(userID);

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("firstname", firstname);
		registerUser.put("lastname", lastname);
		registerUser.put("disability", StaticDataPasser.storeSelectedDisability);
		registerUser.put("age", StaticDataPasser.storeCurrentAge);
		registerUser.put("birthdate", StaticDataPasser.storeCurrentBirthDate);
		registerUser.put("sex", StaticDataPasser.storeSelectedSex);
		registerUser.put("userType", StaticDataPasser.storeRegisterUserType);
		registerUser.put("verificationStatus", verificationStatus);
		registerUser.put("isRegisterComplete", true);
		registerUser.put("totalTrips", 0);

		documentReference.update(registerUser).addOnSuccessListener(unused -> {
			binding.progressBarLayout.setVisibility(View.GONE);
			binding.doneBtn.setVisibility(View.VISIBLE);
			binding.scanIDLayout.setVisibility(View.VISIBLE);

			showRegisterSuccessNotification();

			intent = new Intent(RegisterPWDActivity.this, MainActivity.class);
			startActivity(intent);
			finish();
		}).addOnFailureListener(e -> {
			showRegisterFailedDialog();

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.doneBtn.setVisibility(View.VISIBLE);
			binding.scanIDLayout.setVisibility(View.VISIBLE);

			Log.e(TAG, e.getMessage());
		});
	}

	private void updateCancelledRegister(String userID) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(StaticDataPasser.userCollection).document(userID);

		Map<String, Object> updateRegister = new HashMap<>();
		updateRegister.put("isRegisterComplete", false);
		documentReference.update(updateRegister).addOnSuccessListener(unused -> {

			FirebaseMain.signOutUser();

			intent = new Intent(RegisterPWDActivity.this, LoginActivity.class);
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


	private void showIDNotScannedDialog(String firstname, String lastname) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.id_not_scanned_dialog, null);

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

		View dialogView = getLayoutInflater().inflate(R.layout.cancel_register_dialog, null);

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

	private void showDatePickerDialog() {
		final Calendar currentDate = Calendar.getInstance();
		int year = currentDate.get(Calendar.YEAR);
		int month = currentDate.get(Calendar.MONTH);
		int day = currentDate.get(Calendar.DAY_OF_MONTH);

		DatePickerDialog datePickerDialog = new DatePickerDialog(this,
				(view, year1, monthOfYear, dayOfMonth) -> {
					selectedDate = Calendar.getInstance();
					selectedDate.set(year1, monthOfYear, dayOfMonth);

					// Update the birthdateTextView with the selected date in a desired format
					SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
					binding.birthdateBtn.setText("Birthdate: " + dateFormat.format(selectedDate.getTime()));
					StaticDataPasser.storeCurrentBirthDate = dateFormat.format(selectedDate.getTime());

					// Calculate the age and display it
					calculateAge();
				}, year, month, day);
		datePickerDialog.show();
	}

	private void showBirthdateInputChoiceDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_birthdate_input_choice, null);

		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		Button doneBtn = dialogView.findViewById(R.id.doneBtn);
		EditText yearEditText = dialogView.findViewById(R.id.yearEditText);
		EditText dayEditText = dialogView.findViewById(R.id.dayEditText);



		cancelBtn.setOnClickListener(v -> {
			closeBirthdateInputChoiceDialog();
		});

		builder.setView(dialogView);

		birthdateInputChoiceDialog = builder.create();
		birthdateInputChoiceDialog.show();
	}

	private void closeBirthdateInputChoiceDialog() {
		if (birthdateInputChoiceDialog != null && birthdateInputChoiceDialog.isShowing()) {
			birthdateInputChoiceDialog.dismiss();
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
				.setSmallIcon(R.drawable.logo)
				.setContentTitle("Registration Successful")
				.setContentText("You have successfully registered as a PWD");

		Notification notification = builder.build();
		notificationManager.notify(notificationId, notification);
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

		View dialogView = getLayoutInflater().inflate(R.layout.register_failed_dialog, null);

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

}