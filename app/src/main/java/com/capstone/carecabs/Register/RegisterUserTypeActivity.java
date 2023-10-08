package com.capstone.carecabs.Register;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityRegisterUserTypeBinding;

public class RegisterUserTypeActivity extends AppCompatActivity {
	private Intent intent;
	private String registerUserType, registerType;
	private AlertDialog.Builder builder;
	private AlertDialog userTypeDialog, emailAlreadyRegisteredDialog,
			noInternetDialog, cancelRegisterDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private boolean shouldExit = false;
	private ActivityRegisterUserTypeBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelRegisterDialog();
		closeUserTypeDialog();
		closeNoInternetDialog();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelRegisterDialog();
		closeUserTypeDialog();
		closeNoInternetDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterUserTypeBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.googleRegisterLayout.setVisibility(View.GONE);

		intent = getIntent();
		String getRegisterType = intent.getStringExtra("registerType");

		if (getRegisterType != null) {
			if (getRegisterType.equals("googleRegister")) {
				binding.googleRegisterLayout.setVisibility(View.VISIBLE);

				registerType = "googleRegister";
			} else {
				registerType = "emailRegister";
			}
		} else {
			return;
		}

		binding.cancelBtn.setOnClickListener(v -> {
			showCancelRegisterDialog();
		});

		binding.driverImgBtn.setOnClickListener(v -> {
			intent = new Intent(this, RegisterActivity.class);
			registerUserType = "Driver";
			intent.putExtra("registerUserType", registerUserType);
			intent.putExtra("registerType", registerType);
			startActivity(intent);
			finish();
		});

		binding.passengerImgBtn.setOnClickListener(v -> {
			showUserTypeDialog();
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

	private void showCancelRegisterDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_register, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			intent = new Intent(RegisterUserTypeActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();

			closeCancelRegisterDialog();
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

	private void closeUserTypeDialog() {
		if (userTypeDialog != null && userTypeDialog.isShowing()) {
			userTypeDialog.dismiss();
		}
	}

	private void showUserTypeDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_type, null);

		ImageButton seniorImgBtn = dialogView.findViewById(R.id.seniorImgBtn);
		ImageButton pwdImgBtn = dialogView.findViewById(R.id.pwdImgBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		seniorImgBtn.setOnClickListener(v -> {
			intent = new Intent(RegisterUserTypeActivity.this, RegisterActivity.class);
			registerUserType = "Senior Citizen";
			intent.putExtra("registerUserType", registerUserType);
			intent.putExtra("registerType", registerType);
			startActivity(intent);
			finish();

			closeUserTypeDialog();
		});

		pwdImgBtn.setOnClickListener(v -> {
			intent = new Intent(RegisterUserTypeActivity.this, RegisterActivity.class);
			registerUserType = "Persons with Disability (PWD)";
			intent.putExtra("registerUserType", registerUserType);
			intent.putExtra("registerType", registerType);
			startActivity(intent);
			finish();

			closeUserTypeDialog();
		});

		cancelBtn.setOnClickListener(v -> {
			closeUserTypeDialog();
		});

		builder.setView(dialogView);

		userTypeDialog = builder.create();
		userTypeDialog.show();
	}

	private void showEmailAlreadyRegisteredDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_is_already_registered, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			if (emailAlreadyRegisteredDialog != null && emailAlreadyRegisteredDialog.isShowing()) {
				emailAlreadyRegisteredDialog.dismiss();
			}
			intent = new Intent(RegisterUserTypeActivity.this, RegisterActivity.class);
			startActivity(intent);
			finish();
		});

		builder.setView(dialogView);

		emailAlreadyRegisteredDialog = builder.create();
		emailAlreadyRegisteredDialog.show();
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