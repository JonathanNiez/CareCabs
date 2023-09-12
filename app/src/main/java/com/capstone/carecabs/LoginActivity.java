package com.capstone.carecabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.carecabs.Firebase.FirebaseMain;
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
	private Intent intent;
	private GoogleSignInOptions googleSignInOptions;
	private GoogleSignInAccount googleSignInAccount;
	private GoogleSignInClient googleSignInClient;
	private final String TAG = "Login";
	private boolean shouldExit = false;
	private static final int RC_SIGN_IN = 69;
	private AlertDialog noInternetDialog, emailDialog,
			emailNotRegisteredDialog, incorrectEmailOrPasswordDialog,
			pleaseWaitDialog, registerUsingDialog, loginFailedDialog,
			exitAppDialog, idScanInfoDialog, unknownOccurredDialog,
			invalidCredentialsDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private AlertDialog.Builder builder;
	private ActivityLoginBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		initializeNetworkChecker();

		FirebaseApp.initializeApp(this);

		googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail().build();
		googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
		googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

		binding.registerTextView.setOnClickListener(v -> {
			showRegisterUsingDialog();
		});

		binding.resetPasswordTextView.setOnClickListener(view -> {
			intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
			startActivity(intent);
			finish();
		});

		binding.googleImgBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();

			intent = googleSignInClient.getSignInIntent();
			startActivityForResult(intent, RC_SIGN_IN);
		});

		binding.loginBtn.setOnClickListener(v -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.loginBtn.setVisibility(View.GONE);

			final String stringEmail = binding.email.getText().toString().trim();
			final String stringPassword = binding.password.getText().toString();

			if (stringEmail.isEmpty()) {
				binding.email.setError("Please enter your Email");

				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else if (stringPassword.isEmpty()) {

				binding.password.setError("Please enter your Password");
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.loginBtn.setVisibility(View.VISIBLE);

			} else {
				showPleaseWaitDialog();
				binding.googleSignInLayout.setVisibility(View.GONE);

				loginUser(stringEmail, stringPassword);
			}
		});
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

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeExitConfirmationDialog();
		closeLoginFailedDialog();
		closePleaseWaitDialog();
		closeRegisterUsingDialog();
		closeNoInternetDialog();
		closeUnknownOccurredDialog();
		closeInvalidCredentialsDialog();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeExitConfirmationDialog();
		closeLoginFailedDialog();
		closePleaseWaitDialog();
		closeRegisterUsingDialog();
		closeNoInternetDialog();
		closeUnknownOccurredDialog();
		closeInvalidCredentialsDialog();
	}

	@Override
	public void onBackPressed() {

		if (shouldExit) {
			super.onBackPressed(); // Exit the app
		} else {
			// Show an exit confirmation dialog
			showExitConfirmationDialog();
		}

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

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			finish();
		});

		noBtn.setOnClickListener(v -> {
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

		View dialogView = getLayoutInflater().inflate(R.layout.incorrect_email_or_password_dialog, null);

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

		View dialogView = getLayoutInflater().inflate(R.layout.email_is_already_registered_dialog, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			emailDialog.dismiss();
		});

		builder.setView(dialogView);

		emailDialog = builder.create();
		emailDialog.show();
	}

	private void showRegisterUsingDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.register_using_dialog, null);

		ImageButton googleImgBtn = dialogView.findViewById(R.id.googleImgBtn);
		ImageButton emailImgBtn = dialogView.findViewById(R.id.emailImgBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		googleImgBtn.setOnClickListener(v -> {
			intent = new Intent(LoginActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "googleRegister");
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

			closeRegisterUsingDialog();
		});

		emailImgBtn.setOnClickListener(v -> {
			intent = new Intent(LoginActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "emailRegister");
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

			closeRegisterUsingDialog();
		});

		cancelBtn.setOnClickListener(v -> {
			closeRegisterUsingDialog();
		});

		builder.setView(dialogView);

		registerUsingDialog = builder.create();
		registerUsingDialog.show();

	}

	private void closeRegisterUsingDialog() {
		if (registerUsingDialog != null && registerUsingDialog.isShowing()) {
			registerUsingDialog.dismiss();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RC_SIGN_IN) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				GoogleSignInAccount account = task.getResult(ApiException.class);
				firebaseAuthWithGoogle(account);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
		String getGoogleEmail = acct.getEmail();
		AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
		FirebaseAuth.getInstance().fetchSignInMethodsForEmail(getGoogleEmail)
				.addOnCompleteListener(task -> {
					SignInMethodQueryResult result = task.getResult();
					if (!result.getSignInMethods().isEmpty()) {
						FirebaseMain.getAuth().signInWithCredential(credential).addOnCompleteListener(task1 -> {
							if (task1.isSuccessful()) {

								intent = new Intent(LoginActivity.this, LoggingInActivity.class);
								startActivity(intent);
								finish();

								closePleaseWaitDialog();
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

	private void showLoginFailedDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.login_failed_dialog, null);

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

}