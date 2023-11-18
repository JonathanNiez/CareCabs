package com.capstone.carecabs.Register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityRegisterBinding;
import com.capstone.carecabs.databinding.DialogCancelRegisterBinding;
import com.capstone.carecabs.databinding.DialogEmailVerficationSuccessBinding;
import com.capstone.carecabs.databinding.DialogEmailVerificationLinkSentBinding;
import com.capstone.carecabs.databinding.DialogEnableVoiceAssistantBinding;
import com.capstone.carecabs.databinding.DialogVerifiedEmailBinding;
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
	private ActivityRegisterBinding binding;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 25;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private String fontSize = StaticDataPasser.storeFontSize;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String theme = StaticDataPasser.storeTheme;
	private DocumentReference documentReference;
	private GoogleSignInAccount googleSignInAccount;
	private boolean isEmailVerified = false;
	private Date date;
	private String getUserID, registerType, userType,
			prefixPhoneNumber, accountCreationDate;
	private VoiceAssistant voiceAssistant;
	private Intent intent;
	private static final int GOOGLE_SIGN_IN = 69;
	private AlertDialog.Builder builder;
	private AlertDialog pleaseWaitDialog, noInternetDialog, userTypeImageDialog,
			ageInfoDialog, registerFailedDialog, cancelRegisterDialog,
			emailAlreadyUsedDialog, enableVoiceAssistantDialog, verifiedEmailDialog,
			emailVerificationSentDialog, emailVerifiedSuccess;
	private NetworkChangeReceiver networkChangeReceiver;

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
		closeCancelRegistrationDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
		closeEmailIsAlreadyUsedDialog();
		closeEnableVoiceAssistantDialog();
		closeVerifiedEmailDialog();
		closeEmailVerificationSentDialog();
		closeEmailVerifiedSuccessDialog();
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
		closeCancelRegistrationDialog();
		closeNoInternetDialog();
		closeUserTypeImageDialog();
		closeEmailIsAlreadyUsedDialog();
		closeEnableVoiceAssistantDialog();
		closeVerifiedEmailDialog();
		closeEmailVerificationSentDialog();
		closeEmailVerifiedSuccessDialog();
	}

	@Override
	protected void onResume() {
		super.onResume();

		binding.progressBarLayout.setVisibility(View.GONE);
		binding.nextBtn.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getUser().reload()
					.addOnCompleteListener(task -> {
						if (task.isSuccessful()) {
							if (FirebaseMain.getUser().isEmailVerified()) {
								isEmailVerified = true;
							}
						} else {
							Log.e(TAG, "onCreate: ", task.getException());
						}
					});
		}

		Calendar calendar = Calendar.getInstance();
		date = calendar.getTime();

		binding.backFloatingBtn.setOnClickListener(v -> {
			String email = binding.emailEditText.getText().toString().trim();
			String password = binding.passwordEditText.getText().toString();
			String confirmPassword = binding.confirmPasswordEditText.getText().toString();
			String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

			if (!email.isEmpty() || !password.isEmpty()
					|| !confirmPassword.isEmpty() || !phoneNumber.isEmpty()) {
				showCancelRegistrationDialog();
			}
		});

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
		});

		GoogleSignInOptions googleSignInOptions = new
				GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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
				startActivityForResult(intent, GOOGLE_SIGN_IN);

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
				startActivityForResult(intent, GOOGLE_SIGN_IN);

			} else if (userType.equals("Person with Disabilities (PWD)")) {

				showEnableVoiceAssistantDialog();

				if (voiceAssistantState.equals("enabled")) {
					voiceAssistant = VoiceAssistant.getInstance(this);

					binding.emailEditText.setOnFocusChangeListener((v, hasFocus) ->
							voiceAssistant.speak("Email"));
					binding.passwordEditText.setOnFocusChangeListener((v, hasFocus) ->
							voiceAssistant.speak("Password"));
					binding.confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) ->
							voiceAssistant.speak("Confirm password"));
					binding.phoneNumberEditText.setOnFocusChangeListener((v, hasFocus) ->
							voiceAssistant.speak("Phone number"));
				}
			}
			binding.userTypeImageBtn.setOnClickListener(v -> showUserTypeImageDialog());

			binding.settingsFloatingBtn.setOnClickListener(v -> showSettingsBottomSheet());

			binding.backFloatingBtn.setOnClickListener(v -> checkEditTextIfNotEmpty());

			binding.nextBtn.setOnClickListener(v -> {
				showPleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.VISIBLE);
				binding.nextBtn.setVisibility(View.GONE);

				String email = binding.emailEditText.getText().toString().trim();
				String password = binding.passwordEditText.getText().toString();
				String confirmPassword = binding.confirmPasswordEditText.getText().toString();
				String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

				if (email.isEmpty() ||
						password.isEmpty() ||
						confirmPassword.isEmpty() ||
						phoneNumber.isEmpty()) {

					binding.emailEditText.setError("Please enter your Email");

					if (voiceAssistantState.equals("enabled")) {
						voiceAssistant = VoiceAssistant.getInstance(this);
						voiceAssistant.speak("Please enter your Email");
					}

					closePleaseWaitDialog();
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

				} else if (!confirmPassword.equals(password)) {
					binding.confirmPasswordEditText.setError("Password not match");

					if (voiceAssistantState.equals("enabled")) {
						voiceAssistant = VoiceAssistant.getInstance(this);
						voiceAssistant.speak("Password did not matched");
					}

					closePleaseWaitDialog();
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

				} else if (phoneNumber.length() < 10) {
					binding.phoneNumberEditText.setError("Phone number must be 11 digits");

					closePleaseWaitDialog();
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

				} else {
					closePleaseWaitDialog();
					prefixPhoneNumber = "+63" + phoneNumber;

					if (FirebaseMain.getUser() != null) {
						FirebaseMain.getUser().reload()
								.addOnCompleteListener(task -> {
									if (task.isSuccessful()) {
										if (FirebaseMain.getUser().isEmailVerified()) {
											getUserID = FirebaseMain.getUser().getUid();
											storeUserDataToFireStore(getUserID, email);
										} else {
											showToast("Your Email is not Verified", 0);
											binding.progressBarLayout.setVisibility(View.GONE);
											binding.nextBtn.setVisibility(View.VISIBLE);
										}
									} else {
										showToast("Unknown error occurred", 1);

										binding.progressBarLayout.setVisibility(View.GONE);
										binding.nextBtn.setVisibility(View.VISIBLE);
									}
								});
					} else {
						showVerifiedEmailDialog(userType, email, password);
					}
				}
			});
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		checkEditTextIfNotEmpty();
	}

	private void sendEmailVerification(String userID, String email) {
		if (FirebaseMain.getUser() != null) {
			if (FirebaseMain.getUser().isEmailVerified()) {
				storeUserDataToFireStore(userID, email);
			} else {
				FirebaseMain.getUser().sendEmailVerification()
						.addOnSuccessListener(unused -> {
							closeVerifiedEmailDialog();
							showEmailVerificationSentDialog(userID, email);
						})
						.addOnFailureListener(e -> {
							showToast("Failed to send an Email Verification Link", 1);
							Log.e(TAG, "sendEmailVerification - onFailure: " + e.getMessage());
						});
			}
		} else {
			closeVerifiedEmailDialog();
			showToast("Failed to send an Email Verification Link", 1);
			Log.e(TAG, "sendEmailVerification: current user is null");
		}
	}

	private void checkEditTextIfNotEmpty() {
		String email = binding.emailEditText.getText().toString().trim();
		String password = binding.passwordEditText.getText().toString();
		String confirmPassword = binding.confirmPasswordEditText.getText().toString();
		String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();

		if (!email.isEmpty() ||
				!password.isEmpty() ||
				!confirmPassword.isEmpty() ||
				!phoneNumber.isEmpty()) {

			showCancelRegistrationDialog();

		} else {
			intent = new Intent(RegisterActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
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
					sendEmailVerification(getUserID, email);
				})
				.addOnFailureListener(e -> {
					try {
						throw e;
					} catch (FirebaseAuthUserCollisionException collisionException) {

						closePleaseWaitDialog();
						showEmailIsAlreadyUsedDialog();
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

						Log.e(TAG, "registerDriver: " + collisionException.getMessage());

					} catch (Exception otherException) {
						closePleaseWaitDialog();
						showRegisterFailedDialog();
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

						Log.e(TAG, "registerDriver: " + otherException.getMessage());
					}
				});
	}

	private void registerSenior(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {

					getUserID = authResult.getUser().getUid();
					sendEmailVerification(getUserID, email);
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
					sendEmailVerification(getUserID, email);
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

	private void registerDriverWithoutEmailVerification(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {

					getUserID = authResult.getUser().getUid();
					storeUserDataToFireStore(getUserID, email);
				})
				.addOnFailureListener(e -> {
					try {
						throw e;
					} catch (FirebaseAuthUserCollisionException collisionException) {

						closePleaseWaitDialog();
						showEmailIsAlreadyUsedDialog();
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

						Log.e(TAG, "registerDriver: " + collisionException.getMessage());

					} catch (Exception otherException) {
						closePleaseWaitDialog();
						showRegisterFailedDialog();
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.nextBtn.setVisibility(View.VISIBLE);

						Log.e(TAG, "registerDriver: " + otherException.getMessage());
					}
				});
	}

	private void registerSeniorWithoutEmailVerification(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {

					getUserID = authResult.getUser().getUid();
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

	private void registerPWDWithoutEmailVerification(String email, String password) {
		FirebaseMain.getAuth().createUserWithEmailAndPassword(email, password)
				.addOnSuccessListener(authResult -> {

					getUserID = authResult.getUser().getUid();
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
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
				new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
		accountCreationDate = dateFormat.format(date);
		theme = StaticDataPasser.storeTheme;

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

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

					closePleaseWaitDialog();
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);

					FirebaseMain.signOutUser();

					intent = new Intent(RegisterActivity.this, LoginOrRegisterActivity.class);
					startActivity(intent);
					finish();

					showRegisterFailedDialog();
				});
	}

	//register using google
	private void googleRegisterDriver(AuthCredential authCredential, String googleEmail, String googleProfilePicture) {
		FirebaseMain.getAuth().signInWithCredential(authCredential).addOnSuccessListener(authResult -> {
			getUserID = authResult.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(getUserID);
			storeGoogleUserDataToFireStore(getUserID, googleEmail, googleProfilePicture);

		}).addOnFailureListener(e -> {
			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);

			FirebaseMain.signOutUser();

			showRegisterFailedDialog();

			Log.e(TAG, "googleRegisterDriver: " + e.getMessage());
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
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
				new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
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
	private void showToast(String message, int duration) {
		Toast.makeText(this, message, duration).show();
	}

	private void showEnableVoiceAssistantDialog() {
		builder = new AlertDialog.Builder(this);

		DialogEnableVoiceAssistantBinding dialogEnableVoiceAssistantBinding =
				DialogEnableVoiceAssistantBinding.inflate(getLayoutInflater());
		View dialogView = dialogEnableVoiceAssistantBinding.getRoot();

		if (fontSize.equals("large")) {
			dialogEnableVoiceAssistantBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogEnableVoiceAssistantBinding.bodyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			dialogEnableVoiceAssistantBinding.bodyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			dialogEnableVoiceAssistantBinding.yesBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			dialogEnableVoiceAssistantBinding.noBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		}

		dialogEnableVoiceAssistantBinding.noBtn.setOnClickListener(v -> closeEnableVoiceAssistantDialog());

		dialogEnableVoiceAssistantBinding.yesBtn.setOnClickListener(v -> {
			voiceAssistantState = "enabled";
			StaticDataPasser.storeVoiceAssistantState = voiceAssistantState;
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Voice Assistant enabled");

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

	private void showCancelRegistrationDialog() {
		builder = new AlertDialog.Builder(this);

		DialogCancelRegisterBinding dialogCancelRegisterBinding =
				DialogCancelRegisterBinding.inflate(getLayoutInflater());
		View dialogView = dialogCancelRegisterBinding.getRoot();

		if (fontSize.equals("large")) {
			dialogCancelRegisterBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogCancelRegisterBinding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			dialogCancelRegisterBinding.yesBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			dialogCancelRegisterBinding.noBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Do you want to cancel the Registration?");
		}

		dialogCancelRegisterBinding.yesBtn.setOnClickListener(v -> {
			intent = new Intent(this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();

			closeCancelRegistrationDialog();
		});

		dialogCancelRegisterBinding.noBtn.setOnClickListener(v -> closeCancelRegistrationDialog());

		builder.setView(dialogView);

		cancelRegisterDialog = builder.create();
		cancelRegisterDialog.show();
	}

	private void closeCancelRegistrationDialog() {
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

	private void showVerifiedEmailDialog(String userType, String email, String password) {
		builder = new AlertDialog.Builder(this);

		DialogVerifiedEmailBinding dialogVerifiedEmailBinding =
				DialogVerifiedEmailBinding.inflate(getLayoutInflater());
		View dialogView = dialogVerifiedEmailBinding.getRoot();

		dialogVerifiedEmailBinding.loadingGif.setVisibility(View.GONE);

		if (fontSize.equals("large")) {
			float textSize = 22;
			dialogVerifiedEmailBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogVerifiedEmailBinding.bodyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogVerifiedEmailBinding.bodyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogVerifiedEmailBinding.closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogVerifiedEmailBinding.sendBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogVerifiedEmailBinding.sendVerificationLaterBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}

		dialogVerifiedEmailBinding.sendVerificationLaterBtn.setOnClickListener(v -> {
			dialogVerifiedEmailBinding.closeBtn.setVisibility(View.GONE);
			dialogVerifiedEmailBinding.sendBtn.setVisibility(View.GONE);
			dialogVerifiedEmailBinding.sendVerificationLaterBtn.setVisibility(View.GONE);
			dialogVerifiedEmailBinding.loadingGif.setVisibility(View.VISIBLE);

			switch (userType) {
				case "Driver":
					registerDriverWithoutEmailVerification(email, password);

					break;

				case "Person with Disabilities (PWD)":
					registerPWDWithoutEmailVerification(email, password);

					break;

				case "Senior Citizen":
					registerSeniorWithoutEmailVerification(email, password);

					break;
			}
		});

		dialogVerifiedEmailBinding.sendBtn
				.setOnClickListener(v -> {
					dialogVerifiedEmailBinding.closeBtn.setVisibility(View.GONE);
					dialogVerifiedEmailBinding.sendBtn.setVisibility(View.GONE);
					dialogVerifiedEmailBinding.sendVerificationLaterBtn.setVisibility(View.GONE);
					dialogVerifiedEmailBinding.loadingGif.setVisibility(View.VISIBLE);

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
				});

		dialogVerifiedEmailBinding.closeBtn
				.setOnClickListener(v -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.nextBtn.setVisibility(View.VISIBLE);
					closeVerifiedEmailDialog();
				});

		builder.setView(dialogView);

		verifiedEmailDialog = builder.create();
		verifiedEmailDialog.show();
	}

	private void closeVerifiedEmailDialog() {
		if (verifiedEmailDialog != null && verifiedEmailDialog.isShowing()) {
			verifiedEmailDialog.dismiss();
		}
	}

	@SuppressLint("SetTextI18n")
	private void showEmailVerificationSentDialog(String userID, String email) {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		DialogEmailVerificationLinkSentBinding sentBinding =
				DialogEmailVerificationLinkSentBinding.inflate(getLayoutInflater());
		View dialogView = sentBinding.getRoot();

		if (fontSize.equals("large")) {
			float textSize = 22;
			sentBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			sentBinding.bodyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			sentBinding.bodyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			sentBinding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}

		sentBinding.emailTextView.setText(email);

		sentBinding.okayBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
			closeEmailVerificationSentDialog();
		});

		builder.setView(dialogView);

		emailVerificationSentDialog = builder.create();
		emailVerificationSentDialog.show();
	}

	private void closeEmailVerificationSentDialog() {
		if (emailVerificationSentDialog != null && emailVerificationSentDialog.isShowing()) {
			emailVerificationSentDialog.dismiss();
		}
	}

	private void showEmailVerifiedSuccessDialog(String userID, String email) {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		DialogEmailVerficationSuccessBinding successBinding =
				DialogEmailVerficationSuccessBinding.inflate(getLayoutInflater());
		View dialogView = successBinding.getRoot();

		if (fontSize.equals("large")) {
			float textSize = 22;
			successBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			successBinding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			successBinding.pleaseWaitTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}

		FirebaseMain.getAuth()
				.addAuthStateListener(firebaseAuth -> {
					if (FirebaseMain.getUser() != null) {
						if (FirebaseMain.getUser().isEmailVerified()) {
							new Handler().postDelayed(() -> {
								storeUserDataToFireStore(userID, email);
							}, 2500);
						}
					}
				});

		builder.setView(dialogView);
		emailVerifiedSuccess = builder.create();
		emailVerifiedSuccess.show();
	}

	private void closeEmailVerifiedSuccessDialog() {
		if (emailVerifiedSuccess != null && emailVerifiedSuccess.isShowing()) {
			emailVerifiedSuccess.dismiss();
		}
	}

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

		Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

		tryAgainBtn.setOnClickListener(v -> closeNoInternetDialog());

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
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == GOOGLE_SIGN_IN) {
			Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				googleSignInAccount = googleSignInAccountTask.getResult(ApiException.class);
				fireBaseAuthWithGoogle(googleSignInAccount.getIdToken());
			} catch (Exception e) {
				Log.e(TAG, "onActivityResult: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}