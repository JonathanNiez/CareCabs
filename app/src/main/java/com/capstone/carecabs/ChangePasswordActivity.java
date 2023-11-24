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
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityResetPasswordBinding;
import com.google.firebase.FirebaseApp;

import java.util.Calendar;
import java.util.Date;

public class ChangePasswordActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "ResetPasswordActivity";
	private ActivityResetPasswordBinding binding;
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, cancelPasswordResetDialog,
			passwordUpdateFailedDialog, passwordUpdateSuccessDialog,
			passwordResetConfirmationDialog, passwordWarningDialog,
			pleaseWaitDialog;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 25;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private NetworkChangeReceiver networkChangeReceiver;
	private String fontSize = StaticDataPasser.storeFontSize;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private VoiceAssistant voiceAssistant;
	private Intent intent;
	private Calendar calendar;
	private Date date;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
		closeCancelPasswordResetDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closePasswordResetConfirmationDialog();
		closePasswordWarningDialog();
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
		closePasswordWarningDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.progressBarLayout.setVisibility(View.GONE);
		showPasswordWarningDialog();

		FirebaseApp.initializeApp(this);

		calendar = Calendar.getInstance();
		date = calendar.getTime();

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);

			binding.emailEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Email"));
		}

		binding.backFloatingBtn.setOnClickListener(view -> backToLoginActivity());

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), TAG);
		});

		binding.resetPasswordBtn.setOnClickListener(view -> {
			showPleaseWaitDialog();
			binding.progressBarLayout.setVisibility(View.VISIBLE);
			binding.resetPasswordBtn.setVisibility(View.GONE);

			final String email = binding.emailEditText.getText().toString().trim();

			if (email.isEmpty()) {
				binding.emailEditText.setError("Please enter your Email");

				closePleaseWaitDialog();
				binding.progressBarLayout.setVisibility(View.GONE);
				binding.resetPasswordBtn.setVisibility(View.VISIBLE);

			} else {
				showPasswordResetConfirmationDialog(email);
			}
		});

	}


	@Override
	public void onBackPressed() {
		super.onBackPressed();
		backToLoginActivity();
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

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;

			binding.emailLayout.setHelperTextTextAppearance(R.style.NormalHelperText);
		}

		binding.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.neverShareYourPasswordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.resetPasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void backToLoginActivity() {
		intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	private void resetPassword(String email) {
		FirebaseMain.getAuth().sendPasswordResetEmail(email)
				.addOnSuccessListener(unused -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateSuccessDialog(email);

				})
				.addOnFailureListener(e -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateFailedDialog();

					Log.e(TAG, "resetPassword: " + e.getMessage());
				});
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

	private void showPasswordWarningDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_change_password_warning, null);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak(getString(R.string.password_change_warning_string));
		}

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> closePasswordWarningDialog());

		builder.setView(dialogView);
		passwordWarningDialog = builder.create();
		passwordWarningDialog.show();
	}

	private void closePasswordWarningDialog() {
		if (passwordWarningDialog != null && passwordWarningDialog.isShowing()) {
			passwordWarningDialog.dismiss();
		}
	}

	private void showPasswordResetConfirmationDialog(String email) {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_confirmation, null);

		Button resetBtn = dialogView.findViewById(R.id.resetBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		resetBtn.setOnClickListener(v -> resetPassword(email));

		cancelBtn.setOnClickListener(v -> closePasswordResetConfirmationDialog());

		builder.setView(dialogView);

		passwordResetConfirmationDialog = builder.create();
		passwordResetConfirmationDialog.show();
	}

	private void closePasswordResetConfirmationDialog() {
		if (passwordResetConfirmationDialog != null && passwordResetConfirmationDialog.isShowing()) {
			passwordResetConfirmationDialog.dismiss();
		}
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
			intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
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

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> closePasswordUpdateFailedDialog());

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
			intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
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