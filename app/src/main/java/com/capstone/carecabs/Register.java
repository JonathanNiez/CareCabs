package com.capstone.carecabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityRegisterBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;

import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
	private DocumentReference documentReference;
	private GoogleSignInClient googleSignInClient;
	private GoogleSignInAccount googleSignInAccount;
	private GoogleSignInOptions googleSignInOptions;
	private Calendar calendar;
	private Date date;
	private String getUserID;
	private final String TAG = "Register";
	private boolean shouldExit = false;
	private Intent intent;
	private static final int RC_SIGN_IN = 69;
	private AlertDialog pleaseWaitDialog, noInternetDialog, userTypeImageDialog,
			ageInfoDialog, registerFailedDialog, cancelRegisterDialog;
	private AlertDialog.Builder builder;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityRegisterBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		initializeNetworkChecker();

		FirebaseMain.getAuth();
		FirebaseApp.initializeApp(this);

		calendar = Calendar.getInstance();
		date = calendar.getTime();

		googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
		googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
		googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

		intent = getIntent();
		String getRegisterUserType = intent.getStringExtra("registerUserType");
		String getRegisterType = intent.getStringExtra("registerType");
		StaticDataPasser.storeRegisterType = getRegisterType;
		StaticDataPasser.storeRegisterUserType = getRegisterUserType;

		if (getRegisterType != null && getRegisterUserType != null) {

			switch (getRegisterUserType){
				case "Driver":
					binding.userTypeImageBtn.setImageResource(R.drawable.driver_64);
					break;

				case "Persons with Disability (PWD)":
					binding.userTypeImageBtn.setImageResource(R.drawable.pwd_64);
					break;

				case "Senior Citizen":
					binding.userTypeImageBtn.setImageResource(R.drawable.senior_64_2);
					break;
			}

			if (getRegisterType.equals("googleRegister")) {
				intent = googleSignInClient.getSignInIntent();
				startActivityForResult(intent, RC_SIGN_IN);

				showPleaseWaitDialog();

			} else if (getRegisterUserType.equals("Senior Citizen")) {
				showAgeInfoDialog();

			} else if (getRegisterType.equals("googleRegister") &&
					getRegisterUserType.equals("Senior Citizen")) {
				intent = googleSignInClient.getSignInIntent();
				startActivityForResult(intent, RC_SIGN_IN);

				showAgeInfoDialog();
			}
		} else {
			return;
		}

		binding.userTypeImageBtn.setOnClickListener(v -> {
			showUserTypeImageDialog();
		});

		binding.imgBackBtn.setOnClickListener(v -> {
			showCancelRegisterDialog();
		});

		binding.nextBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.nextBtn.setVisibility(View.GONE);

			String stringEmail = binding.email.getText().toString().trim();
			String stringPassword = binding.password.getText().toString();
			String stringConfirmPassword = binding.confirmPassword.getText().toString();
			String stringPhoneNumber = binding.phoneNumber.getText().toString().trim();
			String prefixPhoneNumber = "+63" + stringPhoneNumber;
			String hashedPassword = BCrypt.hashpw(stringPassword, BCrypt.gensalt());
			StaticDataPasser.storePhoneNumber = prefixPhoneNumber;

			if (stringEmail.isEmpty() || stringPassword.isEmpty()
					|| stringPhoneNumber.isEmpty()) {
				binding.email.setError("Please enter your Email");
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.nextBtn.setVisibility(View.VISIBLE);

			} else if (!stringConfirmPassword.equals(stringPassword)) {
				binding.confirmPassword.setError("Password did not matched");
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.nextBtn.setVisibility(View.VISIBLE);

			} else {
				switch (getRegisterUserType) {
					case "Driver":

						registerDriver(stringEmail, hashedPassword, getRegisterUserType, prefixPhoneNumber);
						break;

					case "Persons with Disability (PWD)":
						registerPWD(stringEmail, hashedPassword, getRegisterUserType, prefixPhoneNumber);

						break;

					case "Senior Citizen":
						registerSenior(stringEmail, hashedPassword, getRegisterUserType, prefixPhoneNumber);

						break;
				}
			}
		});
	}

	private void registerDriver(String email, String password, String userType, String phoneNumber) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeUserDataToFireStore(getUserID, email, password, userType, phoneNumber);

		}).addOnFailureListener(e -> {
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();

			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
		});

	}

	private void registerSenior(String email, String password, String userType, String phoneNumber) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeUserDataToFireStore(getUserID, email, password, userType, phoneNumber);

		}).addOnFailureListener(e -> {
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();

			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
		});

	}

	private void registerPWD(String email, String password, String userType, String phoneNumber) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeUserDataToFireStore(getUserID, email, password, userType, phoneNumber);

		}).addOnFailureListener(e -> {
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();

			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
		});

	}

	private void storeUserDataToFireStore(String userID, String email, String password, String userType, String phoneNumber) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-yyyy-dd HH:mm:ss");
		String formattedDate = dateFormat.format(date);

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("userID", userID);
		registerUser.put("email", email);
		registerUser.put("password", password);
		registerUser.put("userType", userType);
		registerUser.put("profilePicture", "default");
		registerUser.put("phoneNumber", phoneNumber);
		registerUser.put("accountCreationDate", formattedDate);
		documentReference.set(registerUser).addOnSuccessListener(unused -> {

			switch (userType) {
				case "Driver":
					intent = new Intent(Register.this, RegisterDriver.class);

					break;

				case "Senior Citizen":
					intent = new Intent(Register.this, RegisterSenior.class);

					break;

				case "PWD":
					intent = new Intent(Register.this, RegisterPWD.class);

					break;
			}
			StaticDataPasser.storePhoneNumber = null;

			startActivity(intent);
			finish();

		}).addOnFailureListener(e -> {
			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
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

		closeRegisterFailedDialog();
		closePleaseWaitDialog();
		closeAgeInfoDialog();
		closeCancelRegisterDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeRegisterFailedDialog();
		closePleaseWaitDialog();
		closeAgeInfoDialog();
		closeCancelRegisterDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
	}

	private void showAgeInfoDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.age_required_dialog, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeAgeInfoDialog();
		});

		builder.setView(dialogView);

		ageInfoDialog = builder.create();
		ageInfoDialog.show();
	}

	private void closeAgeInfoDialog() {
		if (ageInfoDialog != null && ageInfoDialog.isShowing()) {
			ageInfoDialog.dismiss();
		}
	}

	private void showUserTypeImageDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_you_are_registering_as, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);
		TextView registerAsTextView = dialogView.findViewById(R.id.registerAsTextView);

		registerAsTextView.setText(StaticDataPasser.storeRegisterUserType);

		okBtn.setOnClickListener(v -> {
			closeUserTypeImageDialog();
		});

		builder.setView(dialogView);

		userTypeImageDialog = builder.create();
		userTypeImageDialog.show();
	}

	private void closeUserTypeImageDialog() {
		if (userTypeImageDialog != null && userTypeImageDialog.isShowing()) {
			userTypeImageDialog.dismiss();
		}
	}

	private void showCancelRegisterDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.cancel_register_dialog, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			FirebaseMain.signOutUser();

			intent = new Intent(this, Login.class);
			startActivity(intent);
			finish();
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

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.please_wait_dialog, null);

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SIGN_IN) {
			Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				googleSignInAccount = googleSignInAccountTask.getResult(ApiException.class);
				fireBaseAuthWithGoogle(googleSignInAccount.getIdToken());
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}

	}

	private void googleRegisterDriver(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void googleRegisterSenior(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void googleRegisterPWD(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance().collection("users").document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void storeGoogleUserDataToFireStore(String userID, String googleEmail, String profilePic) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-yyyy-dd HH:mm:ss");
		String formattedDate = dateFormat.format(date);

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("userID", userID);
		registerUser.put("email", googleEmail);
		registerUser.put("userType", StaticDataPasser.storeRegisterUserType);
		registerUser.put("profilePicture", profilePic);
		registerUser.put("phoneNumber", StaticDataPasser.storePhoneNumber);
		registerUser.put("accountCreationDate", formattedDate);

		documentReference.set(registerUser).addOnSuccessListener(unused -> {
			switch (StaticDataPasser.storeRegisterUserType) {
				case "Driver":
					intent = new Intent(Register.this, RegisterDriver.class);
					break;

				case "Senior Citizen":
					intent = new Intent(Register.this, RegisterSenior.class);

					break;

				case "PWD":
					intent = new Intent(Register.this, RegisterPWD.class);

					break;
			}
			StaticDataPasser.storePhoneNumber = null;

			startActivity(intent);
			finish();
		}).addOnFailureListener(e -> {
			Log.e(TAG, e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	//register using google
	private void fireBaseAuthWithGoogle(String idToken) {

		AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
		if (googleSignInAccount != null) {
			googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

			String getGoogleEmail = googleSignInAccount.getEmail();
			String getGoogleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

			switch (StaticDataPasser.storeRegisterUserType) {
				case "Driver":

					googleRegisterDriver(authCredential, getGoogleEmail, getGoogleProfilePic);
					break;

				case "Persons with Disability (PWD)":
					googleRegisterPWD(authCredential, getGoogleEmail, getGoogleProfilePic);

					break;

				case "Senior Citizen":
					googleRegisterSenior(authCredential, getGoogleEmail, getGoogleProfilePic);

					break;
			}
		}
	}
}