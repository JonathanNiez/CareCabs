package com.capstone.carecabs.Register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityRegisterBinding;
import com.capstone.carecabs.databinding.DialogEnableVoiceAssistantBinding;
import com.capstone.carecabs.databinding.DialogYouAreRegisteringAsBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "Register";
	private float textSizeSP, textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private DocumentReference documentReference;
	private GoogleSignInAccount googleSignInAccount;
	private Date date;
	private String getUserID, registerType, userType,
			prefixPhoneNumber, accountCreationDate,
			theme = "normal", fontSize = "normal";
	private String voiceAssistantState = "enabled";
	private VoiceAssistant voiceAssistant;
	private Intent intent;
	private static final int RC_SIGN_IN = 69;
	private AlertDialog pleaseWaitDialog, noInternetDialog, userTypeImageDialog,
			ageInfoDialog, registerFailedDialog, cancelRegisterDialog,
			emailAlreadyUsedDialog, enableVoiceAssistantDialog;
	private AlertDialog.Builder builder;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityRegisterBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeRegisterFailedDialog();
		closePleaseWaitDialog();
		closeAgeRequiredDialog();
		closeCancelRegisterDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
		closeEmailIsAlreadyUsedDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeRegisterFailedDialog();
		closePleaseWaitDialog();
		closeAgeRequiredDialog();
		closeCancelRegisterDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
		closeEmailIsAlreadyUsedDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		Calendar calendar = Calendar.getInstance();
		date = calendar.getTime();

		binding.backFloatingBtn.setOnClickListener(v -> {
			String email = binding.emailEditText.getText().toString().trim();
			String password = binding.passwordEditText.getText().toString();
			String confirmPassword = binding.confirmPasswordEditText.getText().toString();
			String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

			if (!email.isEmpty() || !password.isEmpty()
					|| !confirmPassword.isEmpty() || !phoneNumber.isEmpty()) {
				showCancelRegisterDialog();
			}
		});

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
		});

		GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build();
		GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
		googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

		if (getIntent() != null &&
				getIntent().hasExtra("userType") &&
				getIntent().hasExtra("registerType")) {

			intent = getIntent();
			userType = intent.getStringExtra("userType");
			registerType = intent.getStringExtra("registerType");

			switch (userType) {
				case "Driver":
					binding.userTypeImageBtn.setImageResource(R.drawable.driver_64);

					break;

				case "Person with Disabilities (PWD)":
					binding.userTypeImageBtn.setImageResource(R.drawable.pwd_64);

					break;

				case "Senior Citizen":
					binding.userTypeImageBtn.setImageResource(R.drawable.senior_64_2);

					break;
			}

			if (registerType.equals("Google")) {
				intent = googleSignInClient.getSignInIntent();
				startActivityForResult(intent, RC_SIGN_IN);

			} else if (userType.equals("Senior Citizen")) {
				showAgeRequiredDialog();

				fontSize = "large";
				StaticDataPasser.storeFontSize = fontSize;
				setFontSize(fontSize);

			} else if (registerType.equals("Google") &&
					userType.equals("Senior Citizen")) {
				showAgeRequiredDialog();

				fontSize = "large";
				StaticDataPasser.storeFontSize = fontSize;
				setFontSize(fontSize);


				intent = googleSignInClient.getSignInIntent();
				startActivityForResult(intent, RC_SIGN_IN);

			} else if (userType.equals("Person with Disabilities (PWD)")) {

				showEnableVoiceAssistantDialog();

				if (voiceAssistantState.equals("enabled")) {
					voiceAssistant = VoiceAssistant.getInstance(this);

					binding.emailEditText.setOnClickListener(v -> {
						voiceAssistant.speak("Email");
					});

					binding.passwordEditText.setOnClickListener(v -> {
						voiceAssistant.speak("Password");
					});


					binding.confirmPasswordEditText.setOnClickListener(v -> {
						voiceAssistant.speak("Confirm password");
					});


					binding.phoneNumberEditText.setOnClickListener(v -> {
						voiceAssistant.speak("Phone number");
					});
				}
			}

			binding.userTypeImageBtn.setOnClickListener(v -> showUserTypeImageDialog());

			binding.nextBtn.setOnClickListener(v -> {
				binding.progressBarLayout.setVisibility(View.VISIBLE);
				binding.nextBtn.setVisibility(View.GONE);

				String email = binding.emailEditText.getText().toString().trim();
				String password = binding.passwordEditText.getText().toString();
				String confirmPassword = binding.confirmPasswordEditText.getText().toString();
				String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

				if (email.isEmpty() || password.isEmpty()
						|| phoneNumber.isEmpty()) {
					binding.emailEditText.setError("Please enter your Email");

					if (voiceAssistantState.equals("enabled")) {
						voiceAssistant = VoiceAssistant.getInstance(this);
						voiceAssistant.speak("Please enter your Email");
					}

					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

				} else if (!confirmPassword.equals(password)) {
					binding.confirmPasswordEditText.setError("Password did not matched");

					if (voiceAssistantState.equals("enabled")) {
						voiceAssistant = VoiceAssistant.getInstance(this);
						voiceAssistant.speak("Password did not matched");
					}
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

				} else {
					prefixPhoneNumber = "+63" + phoneNumber;

					showPleaseWaitDialog();

					switch (userType) {
						case "Driver":

							registerDriver(email, password);
							break;

						case "Person with Disabilities (PWD)":
							registerPWD(email, password);

							break;

						case "Senior Citizen":
							registerSenior(email, password);

							break;
					}
				}
			});

		}

	}


	@Override
	public void onBackPressed() {
		boolean shouldExit = false;
		if (shouldExit) {
			super.onBackPressed();
		} else {
			String email = binding.emailEditText.getText().toString().trim();
			String password = binding.passwordEditText.getText().toString();
			String confirmPassword = binding.confirmPasswordEditText.getText().toString();
			String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

			if (!email.isEmpty() || !password.isEmpty()
					|| !confirmPassword.isEmpty() || !phoneNumber.isEmpty()) {
				showCancelRegisterDialog();
			}
		}
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;

			binding.emailLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.passwordLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.confirmPasswordLayout.setHelperTextTextAppearance(R.style.LargeHelperText);
			binding.phoneNumberLayout.setHelperTextTextAppearance(R.style.LargeHelperText);

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;

			binding.emailLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.passwordLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.confirmPasswordLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
			binding.phoneNumberLayout.setHelperTextTextAppearance(R.style.NormalHelperText);

		}

		binding.registerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.confirmPasswordEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.phoneNumberEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.neverShareYourPasswordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.nextTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

	}

	private void registerDriver(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {
					getUserID = authResult.getUser().getUid();

					documentReference = FirebaseMain.getFireStoreInstance()
							.collection(FirebaseMain.userCollection).document(getUserID);
					storeUserDataToFireStore(getUserID, email);

				})
				.addOnFailureListener(e -> {
					try {
						throw e;
					} catch (FirebaseAuthUserCollisionException collisionException) {

						closePleaseWaitDialog();
						showEmailIsAlreadyUsedDialog();

						Log.e(TAG, "registerDriver: " + collisionException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

					} catch (Exception otherException) {
						closePleaseWaitDialog();
						showRegisterFailedDialog();

						Log.e(TAG, "registerDriver: " + otherException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);
					}
				});
	}

	private void registerSenior(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {
					getUserID = authResult.getUser().getUid();

					documentReference = FirebaseMain.getFireStoreInstance()
							.collection(FirebaseMain.userCollection).document(getUserID);
					storeUserDataToFireStore(getUserID, email);

				})
				.addOnFailureListener(e -> {
					try {
						throw e;
					} catch (FirebaseAuthUserCollisionException collisionException) {

						closePleaseWaitDialog();
						showEmailIsAlreadyUsedDialog();

						Log.e(TAG, "registerSenior: " + collisionException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

					} catch (Exception otherException) {
						closePleaseWaitDialog();
						showRegisterFailedDialog();

						Log.e(TAG, "registerSenior: " + otherException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);
					}
				});

	}

	private void registerPWD(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {
					getUserID = authResult.getUser().getUid();

					documentReference = FirebaseMain.getFireStoreInstance()
							.collection(FirebaseMain.userCollection).document(getUserID);
					storeUserDataToFireStore(getUserID, email);

				})
				.addOnFailureListener(e -> {
					try {
						throw e;
					} catch (FirebaseAuthUserCollisionException collisionException) {

						closePleaseWaitDialog();
						showEmailIsAlreadyUsedDialog();

						Log.e(TAG, "registerPWD: " + collisionException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

					} catch (Exception otherException) {
						closePleaseWaitDialog();
						showRegisterFailedDialog();

						Log.e(TAG, "registerPWD: " + otherException.getMessage());

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);
					}
				});

	}

	private void storeUserDataToFireStore(String userID, String email) {
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
		accountCreationDate = dateFormat.format(date);
		theme = StaticDataPasser.storeTheme;

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("userID", userID);
		registerUser.put("email", email);
		registerUser.put("userType", userType);
		registerUser.put("phoneNumber", prefixPhoneNumber);
		registerUser.put("accountCreationDate", accountCreationDate);
		registerUser.put("fontSize", fontSize);
		registerUser.put("theme", theme);
		registerUser.put("registerType", registerType);
		registerUser.put("isVerified", false);
		registerUser.put("isFirstTimeUser", true);

		documentReference.set(registerUser)
				.addOnSuccessListener(unused -> {

					switch (userType) {
						case "Driver":
							intent = new Intent(RegisterActivity.this, RegisterDriverActivity.class);

							break;

						case "Senior Citizen":
							intent = new Intent(RegisterActivity.this, RegisterSeniorActivity.class);

							break;

						case "Person with Disabilities (PWD)":
							intent = new Intent(RegisterActivity.this, RegisterPWDActivity.class);

							break;
					}
					startActivity(intent);
					finish();

				})
				.addOnFailureListener(e -> {
					Log.e(TAG, "storeUserDataToFireStore: " + e.getMessage());

					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterActivity.this, LoginActivity.class);
					startActivity(intent);
					finish();

					showRegisterFailedDialog();
				});
	}

	private void showEnableVoiceAssistantDialog() {
		builder = new AlertDialog.Builder(this);

		DialogEnableVoiceAssistantBinding binding =
				DialogEnableVoiceAssistantBinding.inflate(getLayoutInflater());
		View dialogView = binding.getRoot();

		String message = "Voice Assistant is helpful for visual impairments." +
				"Do you want to enable Voice Assistant?";

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistantState = "disabled";
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak(message);
		}

		binding.yesBtn.setOnClickListener(v -> closeEnableVoiceAssistantDialog());

		binding.noBtn.setOnClickListener(v -> {
			if (voiceAssistantState.equals("enabled")) {
				voiceAssistant = VoiceAssistant.getInstance(this);
				voiceAssistant.shutdown();
			}

			closeEnableVoiceAssistantDialog();
		});

		builder.setView(dialogView);

		enableVoiceAssistantDialog = builder.create();
		enableVoiceAssistantDialog.show();

	}

	private void closeEnableVoiceAssistantDialog() {
		if (enableVoiceAssistantDialog != null && enableVoiceAssistantDialog.isShowing()) {
			enableVoiceAssistantDialog.dismiss();
		}
	}

	private void showAgeRequiredDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_age_required, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> closeAgeRequiredDialog());

		builder.setView(dialogView);

		ageInfoDialog = builder.create();
		ageInfoDialog.show();
	}

	private void closeAgeRequiredDialog() {
		if (ageInfoDialog != null && ageInfoDialog.isShowing()) {
			ageInfoDialog.dismiss();
		}
	}

	private void showUserTypeImageDialog() {
		builder = new AlertDialog.Builder(this);

		DialogYouAreRegisteringAsBinding dialogYouAreRegisteringAsBinding =
				DialogYouAreRegisteringAsBinding.inflate(getLayoutInflater());
		View dialogView = dialogYouAreRegisteringAsBinding.getRoot();

		dialogYouAreRegisteringAsBinding.registerAsTextView.setText(userType);

		switch (userType) {
			case "Driver":
				dialogYouAreRegisteringAsBinding.userTypeImageView.setImageResource(R.drawable.driver_64);

				break;

			case "Senior Citizen":
				dialogYouAreRegisteringAsBinding.userTypeImageView.setImageResource(R.drawable.senior_64_2);

				break;

			case "Person with Disabilities (PWD)":
				dialogYouAreRegisteringAsBinding.userTypeImageView.setImageResource(R.drawable.pwd_64);

				break;
		}

		dialogYouAreRegisteringAsBinding.okayBtn.setOnClickListener(v -> {
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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_register, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Do you want to cancel the Registration?");
		}

		yesBtn.setOnClickListener(v -> {
			intent = new Intent(this, LoginOrRegisterActivity.class);
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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_please_wait, null);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Please wait");
		}

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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> closeRegisterFailedDialog());

		builder.setView(dialogView);

		registerFailedDialog = builder.create();
		registerFailedDialog.show();

	}

	private void closeRegisterFailedDialog() {
		if (registerFailedDialog != null && registerFailedDialog.isShowing()) {
			registerFailedDialog.dismiss();
		}
	}

	private void showEmailIsAlreadyUsedDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_is_already_registered, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeEmailIsAlreadyUsedDialog();
		});

		builder.setView(dialogView);

		emailAlreadyUsedDialog = builder.create();
		emailAlreadyUsedDialog.show();

	}

	private void closeEmailIsAlreadyUsedDialog() {
		if (emailAlreadyUsedDialog != null && emailAlreadyUsedDialog.isShowing()) {
			emailAlreadyUsedDialog.dismiss();
		}
	}

	private void googleRegisterDriver(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, "googleRegisterDriver: " + e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);

			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void googleRegisterSenior(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, "googleRegisterSenior: " + e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);

			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void googleRegisterPWD(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			Log.e(TAG, "googleRegisterPWD: " + e.getMessage());

			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);

			FirebaseMain.signOutUser();

			showRegisterFailedDialog();
		});
	}

	private void storeGoogleUserDataToFireStore(String userID, String googleEmail, String profilePic) {
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
		accountCreationDate = dateFormat.format(date);
		theme = StaticDataPasser.storeTheme;

		Map<String, Object> registerUser = new HashMap<>();
		registerUser.put("userID", userID);
		registerUser.put("email", googleEmail);
		registerUser.put("userType", userType);
		registerUser.put("profilePicture", profilePic);
		registerUser.put("phoneNumber", prefixPhoneNumber);
		registerUser.put("accountCreationDate", accountCreationDate);
		registerUser.put("fontSize", fontSize);
		registerUser.put("theme", theme);
		registerUser.put("registerType", registerType);
		registerUser.put("isVerified", false);
		registerUser.put("isFirstTimeUser", true);

		documentReference.set(registerUser)
				.addOnSuccessListener(unused -> {
					switch (userType) {
						case "Driver":
							intent = new Intent(RegisterActivity.this, RegisterDriverActivity.class);
							break;

						case "Senior Citizen":
							intent = new Intent(RegisterActivity.this, RegisterSeniorActivity.class);

							break;

						case "Person with Disabilities (PWD)":
							intent = new Intent(RegisterActivity.this, RegisterPWDActivity.class);

							break;
					}
					StaticDataPasser.storePhoneNumber = null;

					startActivity(intent);
					finish();
				})
				.addOnFailureListener(e -> {

					Log.e(TAG, "storeGoogleUserDataToFireStore: " + e.getMessage());

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

			switch (userType) {
				case "Driver":
					googleRegisterDriver(authCredential, getGoogleEmail, getGoogleProfilePic);

					break;

				case "Person with Disabilities (PWD)":
					googleRegisterPWD(authCredential, getGoogleEmail, getGoogleProfilePic);

					break;

				case "Senior Citizen":
					googleRegisterSenior(authCredential, getGoogleEmail, getGoogleProfilePic);

					break;
			}
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
				Log.e(TAG, "onActivityResult: " + e.getMessage());
			}
		}

	}
}