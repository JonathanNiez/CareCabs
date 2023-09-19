package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityResetPasswordBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Calendar;
import java.util.Date;

public class ResetPasswordActivity extends AppCompatActivity {
	private final String TAG = "ResetPasswordActivity";
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, cancelPasswordResetDialog, passwordUpdateFailedDialog,
			passwordUpdateSuccessDialog, passwordResetConfirmationDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private Intent intent;
	private Calendar calendar;
	private Date date;
	private DocumentReference documentReference;
	private ActivityResetPasswordBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);

		initializeNetworkChecker();

		FirebaseApp.initializeApp(this);

		calendar = Calendar.getInstance();
		date = calendar.getTime();

		binding.imgBackBtn.setOnClickListener(view -> {
			showCancelPasswordResetDialog();
		});

		binding.resetPasswordBtn.setOnClickListener(view -> {
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.resetPasswordBtn.setVisibility(View.GONE);

			String email = binding.emailEditText.getText().toString().trim();
			String password = binding.passwordEditText.getText().toString();
			String confirmPassword = binding.confirmPasswordEditText.getText().toString();

			if (email.isEmpty()) {
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.resetPasswordBtn.setVisibility(View.VISIBLE);

				binding.emailEditText.setError("Please enter your Email");

			} else if (isValidEmail(email)) {
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.resetPasswordBtn.setVisibility(View.VISIBLE);

				binding.emailEditText.setError("Please enter a valid Email");

			} else if (password.isEmpty() || confirmPassword.isEmpty()) {
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.resetPasswordBtn.setVisibility(View.VISIBLE);

				binding.passwordEditText.setError("Please enter the password you want to reset");

			} else if (!confirmPassword.equals(password)) {
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.resetPasswordBtn.setVisibility(View.VISIBLE);

				binding.confirmPasswordEditText.setError("Confirm password did not match");

			} else {
				showPasswordResetConfirmationDialog(email);
			}
		});

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
		closeCancelPasswordResetDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closePasswordResetConfirmationDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeNoInternetDialog();
		closeCancelPasswordResetDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closePasswordResetConfirmationDialog();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		showCancelPasswordResetDialog();
	}

	public boolean isValidEmail(String email) {
		String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}";
		return email.matches(emailPattern);
	}

	private void resetPassword(String email) {
		FirebaseMain.getAuth().sendPasswordResetEmail(email)
				.addOnSuccessListener(unused -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateSuccessDialog(email);

				}).addOnFailureListener(e -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateFailedDialog();

					Log.e(TAG, e.getMessage());
				});
	}

	private void closePasswordResetConfirmationDialog() {
		if (passwordResetConfirmationDialog != null && passwordResetConfirmationDialog.isShowing()) {
			passwordResetConfirmationDialog.dismiss();
		}
	}

	private void showPasswordResetConfirmationDialog(String email) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_confirmation, null);

		Button resetBtn = dialogView.findViewById(R.id.resetBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		resetBtn.setOnClickListener(v -> {
			resetPassword(email);
		});

		cancelBtn.setOnClickListener(v -> {
			closePasswordResetConfirmationDialog();
		});

		builder.setView(dialogView);

		passwordResetConfirmationDialog = builder.create();
		passwordResetConfirmationDialog.show();
	}

	private void showPasswordUpdateSuccessDialog(String email) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_success, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);
		TextView emailTextView = dialogView.findViewById(R.id.emailTextView);

		String text = email;
		SpannableString underlinedText = new SpannableString(text);

		underlinedText.setSpan(new UnderlineSpan(), 0, text.length(), 0);

		emailTextView.setText(underlinedText);

		okBtn.setOnClickListener(v -> {
			intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		});

		builder.setView(dialogView);

		passwordUpdateSuccessDialog = builder.create();
		passwordUpdateSuccessDialog.show();
	}

	private void closePasswordUpdateSuccessDialog() {
		if (passwordUpdateSuccessDialog != null && passwordUpdateSuccessDialog.isShowing()) {
			passwordUpdateSuccessDialog.dismiss();
		}
	}

	private void showPasswordUpdateFailedDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		});


		builder.setView(dialogView);

		passwordUpdateFailedDialog = builder.create();
		passwordUpdateFailedDialog.show();
	}

	private void closePasswordUpdateFailedDialog() {
		if (passwordUpdateFailedDialog != null && passwordUpdateFailedDialog.isShowing()) {
			passwordUpdateFailedDialog.dismiss();
		}

	}

	private void showCancelPasswordResetDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_password_reset, null);

		Button noBtn = dialogView.findViewById(R.id.noBtn);
		Button yesBtn = dialogView.findViewById(R.id.yesBtn);

		yesBtn.setOnClickListener(v -> {
			intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		});

		noBtn.setOnClickListener(view -> {
			closeCancelPasswordResetDialog();
		});

		builder.setView(dialogView);

		cancelPasswordResetDialog = builder.create();
		cancelPasswordResetDialog.show();
	}

	private void closeCancelPasswordResetDialog() {
		if (cancelPasswordResetDialog != null && cancelPasswordResetDialog.isShowing()) {
			cancelPasswordResetDialog.dismiss();
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
}