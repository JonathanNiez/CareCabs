package com.capstone.carecabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Register.RegisterActivity;
import com.capstone.carecabs.Register.RegisterUserTypeActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityLoginBinding;
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

public class LoginActivity extends AppCompatActivity {
	private final String TAG = "Login";
	private Intent intent;
	private GoogleSignInAccount googleSignInAccount;
	private GoogleSignInClient googleSignInClient;
	private static final int RC_SIGN_IN = 69;
	private AlertDialog noInternetDialog, emailDialog,
			emailNotRegisteredDialog, incorrectEmailOrPasswordDialog,
			pleaseWaitDialog, loginFailedDialog, exitAppDialog,
			idScanInfoDialog, unknownOccurredDialog, invalidCredentialsDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private AlertDialog.Builder builder;
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
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build();
		googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
		googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

		binding.resetPasswordBtn.setOnClickListener(view -> {
			intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
			startActivity(intent);
			finish();
		});

		binding.googleLoginBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();

			intent = googleSignInClient.getSignInIntent();
			startActivityForResult(intent, RC_SIGN_IN);
		});

		binding.backBtn.setOnClickListener(v -> {
			goToLoginOrRegisterActivity();
		});

		binding.loginBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.loginBtn.setVisibility(View.GONE);

			final String stringEmail = binding.emailEditText.getText().toString().trim();
			final String stringPassword = binding.passwordEditText.getText().toString();

			if (stringEmail.isEmpty()) {
				binding.emailEditText.setError("Please enter your Email");

				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else if (stringPassword.isEmpty()) {

				binding.passwordEditText.setError("Please enter your Password");
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else {
				showPleaseWaitDialog();
				binding.googleLoginBtn.setVisibility(View.GONE);

				loginUser(stringEmail, stringPassword);
			}
		});
	}

	@Override
	public void onBackPressed() {
		goToLoginOrRegisterActivity();
	}


	public boolean isValidEmail(String email) {
		String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}";
		return email.matches(emailPattern);
	}

	private void loginUser(String email, String password) {
		FirebaseMain.getAuth().signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(task -> {
					if (task.isSuccessful()) {
						closePleaseWaitDialog();

						binding.progressBarLayout.setVisibility(View.GONE);
						binding.loginBtn.setVisibility(View.VISIBLE);

						intent = new Intent(LoginActivity.this, LoggingInActivity.class);
						startActivity(intent);
						finish();

						Log.i(TAG, "Login Success");

					} else {
						binding.progressBarLayout.setVisibility(View.GONE);
						binding.loginBtn.setVisibility(View.VISIBLE);

						closePleaseWaitDialog();

						Exception exception = task.getException();
						if (exception instanceof FirebaseAuthInvalidCredentialsException) {
							showInvalidCredentialsDialog();
						} else {
							showUnknownOccurredDialog();
						}
						Log.e(TAG, String.valueOf(task.getException()));
						Log.e(TAG, "Login Failed");
					}
				});
	}


	private void goToLoginOrRegisterActivity() {
		intent = new Intent(LoginActivity.this, LoginOrRegisterActivity.class);
		startActivity(intent);
		finish();
	}

	private void showInvalidCredentialsDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_invalid_credentials, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeInvalidCredentialsDialog();
		});

		builder.setView(dialogView);

		invalidCredentialsDialog = builder.create();
		invalidCredentialsDialog.show();
	}

	private void closeInvalidCredentialsDialog() {
		if (invalidCredentialsDialog != null && invalidCredentialsDialog.isShowing()) {
			invalidCredentialsDialog.dismiss();
		}
	}

	private void showUnknownOccurredDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_unknown_error_occured, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeUnknownOccurredDialog();
		});

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

		View dialogView = getLayoutInflater().inflate(R.layout.email_not_registered_dialog, null);

		Button noBtn = dialogView.findViewById(R.id.noBtn);
		Button yesBtn = dialogView.findViewById(R.id.yesBtn);

		noBtn.setOnClickListener(v -> {
			if (emailNotRegisteredDialog != null && emailNotRegisteredDialog.isShowing()) {
				emailNotRegisteredDialog.dismiss();
			}
		});

		yesBtn.setOnClickListener(v -> {
			if (emailNotRegisteredDialog != null && emailNotRegisteredDialog.isShowing()) {
				emailNotRegisteredDialog.dismiss();
			}

			intent = new Intent(this, RegisterActivity.class);
			startActivity(intent);
			finish();
		});

		builder.setView(dialogView);

		emailNotRegisteredDialog = builder.create();
		emailNotRegisteredDialog.show();
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

		//TODO:google sign in
		if (requestCode == RC_SIGN_IN) {

			Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

			try {
				googleSignInAccount = googleSignInAccountTask.getResult(ApiException.class);
				firebaseAuthWithGoogle(googleSignInAccount);

			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
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

							FirebaseMain.getAuth().signInWithCredential(authCredential).addOnCompleteListener(task1 -> {
								if (task1.isSuccessful()) {

									closePleaseWaitDialog();

									intent = new Intent(LoginActivity.this, LoggingInActivity.class);
									startActivity(intent);
									finish();

								} else {
									closePleaseWaitDialog();
									showLoginFailedDialog();
								}

							});
						} else {
							closePleaseWaitDialog();

							showEmailAlreadyRegisteredDialog();

							intent = new Intent(this, RegisterUserTypeActivity.class);
							intent.putExtra("registerType", "googleRegister");
							startActivity(intent);
							finish();
						}
					});
		}

	}
}