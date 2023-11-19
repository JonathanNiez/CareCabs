package com.capstone.carecabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Register.RegisterUserTypeActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityLoginBinding;
import com.capstone.carecabs.databinding.DialogEmailNotVerifiedBinding;
import com.capstone.carecabs.databinding.DialogEmailVerificationLinkSentBinding;
import com.capstone.carecabs.databinding.DialogInvalidCredentialsBinding;
import com.capstone.carecabs.databinding.DialogLoginUnknownErrorOccuredBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;

public class LoginActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "Login";
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private Intent intent;
	private GoogleSignInAccount googleSignInAccount;
	private GoogleSignInClient googleSignInClient;
	private static final int GOOGLE_SIGN_IN = 69;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, emailDialog,
			emailNotRegisteredDialog, incorrectEmailOrPasswordDialog,
			pleaseWaitDialog, loginFailedDialog, exitAppDialog,
			idScanInfoDialog, unknownOccurredDialog, invalidCredentialsDialog,
			emailVerificationSentDialog, emailNotVerifiedDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityLoginBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeExitConfirmationDialog();
		closeLoginFailedDialog();
		closePleaseWaitDialog();
		closeNoInternetDialog();
		closeUnknownOccurredDialog();
		closeInvalidCredentialsDialog();
		closeEmailNotRegisterDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeExitConfirmationDialog();
		closeLoginFailedDialog();
		closePleaseWaitDialog();
		closeNoInternetDialog();
		closeUnknownOccurredDialog();
		closeInvalidCredentialsDialog();
		closeEmailNotRegisterDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		GoogleSignInOptions googleSignInOptions = new
				GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build();

		googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
		googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

		binding.resetPasswordBtn.setOnClickListener(view -> {
			intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
			startActivity(intent);
			finish();
		});

		binding.googleLoginBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.googleLoginBtn.setVisibility(View.GONE);

			intent = googleSignInClient.getSignInIntent();
			startActivityForResult(intent, GOOGLE_SIGN_IN);
		});

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), TAG);
		});

		binding.backFloatingBtn.setOnClickListener(v -> goToLoginOrRegisterActivity());

		binding.loginBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.loginBtn.setVisibility(View.GONE);

			final String email = binding.emailEditText.getText().toString().trim();
			final String password = binding.passwordEditText.getText().toString();

			if (email.isEmpty()) {

				binding.emailEditText.setError("Please enter your Email");

				closePleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else if (password.isEmpty()) {

				binding.passwordEditText.setError("Please enter your Password");

				closePleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else {
				binding.googleLoginBtn.setVisibility(View.GONE);
				loginUser(email, password);
			}
		});
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		goToLoginOrRegisterActivity();
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		String fonSize = isChecked ? "large" : "normal";
		setFontSize(fonSize);
	}

	private void setFontSize(String fontSize) {
		float textSizeSP;
		float textHeaderSizeSP;
		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.loginTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

		binding.emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

		binding.neverShareYourPasswordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.forgotPasswordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.loginBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.loginWithGoogleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.resetPasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void loginUser(String email, String password) {
		FirebaseMain.getAuth().signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(task -> {
					if (task.isSuccessful()) {

						closePleaseWaitDialog();

						if (FirebaseMain.getUser().isEmailVerified()) {
							binding.progressBarLayout.setVisibility(View.GONE);
							binding.loginBtn.setVisibility(View.VISIBLE);

							goToLoggingInActivity();

							Log.i(TAG, "Login Success");
						} else {
							showEmailNotVerifiedDialog(email, password);
						}

					} else {
						closePleaseWaitDialog();
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.loginBtn.setVisibility(View.VISIBLE);

						Exception exception = task.getException();
						if (exception instanceof FirebaseAuthInvalidCredentialsException) {
							showInvalidCredentialsDialog();
							binding.progressBarLayout.setVisibility(View.GONE);
							binding.loginBtn.setVisibility(View.VISIBLE);

						} else {
							showUnknownOccurredDialog();
							binding.progressBarLayout.setVisibility(View.GONE);
							binding.loginBtn.setVisibility(View.VISIBLE);
						}

						Log.e(TAG, "loginUser: " + task.getException());
					}
				});
	}

	private void goToLoggingInActivity() {
		intent = new Intent(LoginActivity.this, LoggingInActivity.class);
		startActivity(intent);
		finish();
	}

	private void sendEmailVerification(String email) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getUser().sendEmailVerification()
					.addOnSuccessListener(unused -> showEmailVerificationSentDialog(email))
					.addOnFailureListener(e -> {
						showToast("Failed to send an Email Verification Link", 1);
						Log.e(TAG, "sendEmailVerification - onFailure: " + e.getMessage());
					});
		}
	}

	private void goToLoginOrRegisterActivity() {
		intent = new Intent(LoginActivity.this, LoginOrRegisterActivity.class);
		startActivity(intent);
		finish();
	}

	private void showToast(String message, int duration) {
		Toast.makeText(this, message, duration).show();
	}

	private void showInvalidCredentialsDialog() {
		builder = new AlertDialog.Builder(this);

		DialogInvalidCredentialsBinding dialogInvalidCredentialsBinding =
				DialogInvalidCredentialsBinding.inflate(getLayoutInflater());
		View dialogView = dialogInvalidCredentialsBinding.getRoot();

		dialogInvalidCredentialsBinding.okayBtn.setOnClickListener(v -> closeInvalidCredentialsDialog());

		builder.setView(dialogView);

		invalidCredentialsDialog = builder.create();
		invalidCredentialsDialog.show();
	}

	private void closeInvalidCredentialsDialog() {
		if (invalidCredentialsDialog != null && invalidCredentialsDialog.isShowing()) {
			invalidCredentialsDialog.dismiss();
		}
	}

	private void showEmailNotVerifiedDialog(String email, String password) {
		builder = new AlertDialog.Builder(this);

		DialogEmailNotVerifiedBinding dialogEmailNotVerifiedBinding =
				DialogEmailNotVerifiedBinding.inflate(getLayoutInflater());
		View dialogView = dialogEmailNotVerifiedBinding.getRoot();

		dialogEmailNotVerifiedBinding.emailTextView.setText(email);

		dialogEmailNotVerifiedBinding.laterBtn
				.setOnClickListener(v -> {
					FirebaseMain.getAuth()
							.signInWithEmailAndPassword(email, password)
							.addOnCompleteListener(task -> {
								if (task.isSuccessful()) {
									goToLoggingInActivity();
									binding.progressBarLayout.setVisibility(View.GONE);
									binding.loginBtn.setVisibility(View.VISIBLE);
									binding.googleLoginBtn.setVisibility(View.VISIBLE);
									closeEmailNotVerifiedDialog();

								} else {
									Exception exception = task.getException();
									if (exception instanceof FirebaseAuthInvalidCredentialsException) {
										closeEmailNotVerifiedDialog();
										showInvalidCredentialsDialog();
									} else {
										closeEmailNotVerifiedDialog();
										showUnknownOccurredDialog();
									}

									Log.e(TAG, "showEmailNotVerifiedDialog - onFailure: " + task.getException());
								}
							});
				});

		dialogEmailNotVerifiedBinding.sendBtn
				.setOnClickListener(v -> {
					closeEmailNotVerifiedDialog();
					sendEmailVerification(email);
				});

		builder.setView(dialogView);

		emailNotVerifiedDialog = builder.create();
		emailNotVerifiedDialog.show();
	}

	private void closeEmailNotVerifiedDialog() {
		if (emailNotVerifiedDialog != null && emailNotVerifiedDialog.isShowing()) {
			emailNotVerifiedDialog.dismiss();
		}
	}

	private void showEmailVerificationSentDialog(String email) {
		builder = new AlertDialog.Builder(this);

		DialogEmailVerificationLinkSentBinding sentBinding =
				DialogEmailVerificationLinkSentBinding.inflate(getLayoutInflater());
		View dialogView = sentBinding.getRoot();

		sentBinding.emailTextView.setText(email);

		builder.setView(dialogView);

		emailVerificationSentDialog = builder.create();
		emailVerificationSentDialog.show();
	}

	private void closeEmailVerificationSentDialog() {
		if (emailVerificationSentDialog != null && emailVerificationSentDialog.isShowing()) {
			emailVerificationSentDialog.dismiss();
		}
	}

	private void showUnknownOccurredDialog() {
		builder = new AlertDialog.Builder(this);

		DialogLoginUnknownErrorOccuredBinding dialogLoginUnknownErrorOccuredBinding =
				DialogLoginUnknownErrorOccuredBinding.inflate(getLayoutInflater());
		View dialogView = dialogLoginUnknownErrorOccuredBinding.getRoot();

		dialogLoginUnknownErrorOccuredBinding.okayBtn.setOnClickListener(v -> closeUnknownOccurredDialog());

		builder.setView(dialogView);

		unknownOccurredDialog = builder.create();
		unknownOccurredDialog.show();
	}

	private void closeUnknownOccurredDialog() {
		if (unknownOccurredDialog != null && unknownOccurredDialog.isShowing()) {
			unknownOccurredDialog.dismiss();
		}
	}

	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_app, null);

		Button exitBtn = dialogView.findViewById(R.id.exitBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		exitBtn.setOnClickListener(v -> {
			finish();
		});

		cancelBtn.setOnClickListener(v -> {
			closeExitConfirmationDialog();
		});

		builder.setView(dialogView);

		exitAppDialog = builder.create();
		exitAppDialog.show();
	}

	private void closeExitConfirmationDialog() {
		if (exitAppDialog != null && exitAppDialog.isShowing()) {
			exitAppDialog.dismiss();
		}
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

	private void showEmailNotRegisterDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_email_not_registered, null);

		Button noBtn = dialogView.findViewById(R.id.noBtn);
		Button yesBtn = dialogView.findViewById(R.id.yesBtn);

		noBtn.setOnClickListener(v -> {
			closeEmailNotRegisterDialog();
		});

		yesBtn.setOnClickListener(v -> {
			intent = new Intent(this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "googleRegister");
			startActivity(intent);
			finish();
		});

		builder.setView(dialogView);

		emailNotRegisteredDialog = builder.create();
		emailNotRegisteredDialog.show();
	}

	private void closeEmailNotRegisterDialog() {
		if (emailNotRegisteredDialog != null && emailNotRegisteredDialog.isShowing()) {
			emailNotRegisteredDialog.dismiss();
		}
	}

	private void showIncorrectEmailOrPasswordDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_incorrect_email_or_password, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			if (incorrectEmailOrPasswordDialog != null && incorrectEmailOrPasswordDialog.isShowing()) {
				incorrectEmailOrPasswordDialog.dismiss();
			}
		});

		builder.setView(dialogView);

		incorrectEmailOrPasswordDialog = builder.create();
		incorrectEmailOrPasswordDialog.show();
	}

	private void showEmailAlreadyRegisteredDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_is_already_registered, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			emailDialog.dismiss();
		});

		builder.setView(dialogView);

		emailDialog = builder.create();
		emailDialog.show();
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

	private void showLoginFailedDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeLoginFailedDialog();
		});

		builder.setView(dialogView);

		loginFailedDialog = builder.create();
		loginFailedDialog.show();

	}

	private void closeLoginFailedDialog() {
		if (loginFailedDialog != null && loginFailedDialog.isShowing()) {
			loginFailedDialog.dismiss();
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == GOOGLE_SIGN_IN) {

			Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

			try {
				googleSignInAccount = googleSignInAccountTask.getResult(ApiException.class);
				firebaseAuthWithGoogle(googleSignInAccount);

			} catch (Exception e) {
				Log.e(TAG, "onActivityResult: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void firebaseAuthWithGoogle(GoogleSignInAccount googleSignInAccount) {

		if (googleSignInAccount != null) {

			String getGoogleEmail = googleSignInAccount.getEmail();

			AuthCredential authCredential = GoogleAuthProvider
					.getCredential(googleSignInAccount
							.getIdToken(), null);

			FirebaseAuth.getInstance()
					.fetchSignInMethodsForEmail(getGoogleEmail)
					.addOnCompleteListener(task -> {
						SignInMethodQueryResult signInMethodQueryResult = task.getResult();
						if (!signInMethodQueryResult.getSignInMethods().isEmpty()) {

							FirebaseMain.getAuth().signInWithCredential(authCredential)
									.addOnCompleteListener(task1 -> {
										if (task1.isSuccessful()) {
											closePleaseWaitDialog();

											intent = new Intent(LoginActivity.this, LoggingInActivity.class);
											startActivity(intent);
											finish();

										} else {
											closePleaseWaitDialog();
											binding.progressBarLayout.setVisibility(View.GONE);
											binding.googleLoginBtn.setVisibility(View.VISIBLE);
											showLoginFailedDialog();

											Log.e(TAG, "firebaseAuthWithGoogle: " + task1.getException());
										}

									});
						} else {
							closePleaseWaitDialog();
							showEmailNotRegisterDialog();
							binding.progressBarLayout.setVisibility(View.GONE);
							binding.googleLoginBtn.setVisibility(View.VISIBLE);
						}
					});
		}
	}
}