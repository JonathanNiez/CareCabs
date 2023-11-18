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
import com.capstone.carecabs.databinding.DialogUserTypeBinding;

public class RegisterUserTypeActivity extends AppCompatActivity {
	private ActivityRegisterUserTypeBinding binding;
	private Intent intent;
	private String userType, registerType;
	private AlertDialog.Builder builder;
	private AlertDialog userTypeDialog, emailAlreadyRegisteredDialog,
			noInternetDialog, cancelRegisterDialog;
	private NetworkChangeReceiver networkChangeReceiver;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeUserTypeDialog();
		closeNoInternetDialog();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeUserTypeDialog();
		closeNoInternetDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterUserTypeBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.googleRegisterLayout.setVisibility(View.GONE);

		if (getIntent() != null &&
				getIntent().hasExtra("registerType")) {

			intent = getIntent();
			registerType = intent.getStringExtra("registerType");

			if (registerType.equals("Google")) {
				binding.googleRegisterLayout.setVisibility(View.VISIBLE);
			}

			binding.driverBtn.setOnClickListener(v -> {
				intent = new Intent(this, RegisterActivity.class);
				userType = "Driver";
				intent.putExtra("userType", userType);
				intent.putExtra("registerType", registerType);
				startActivity(intent);
				finish();
			});
		}

		binding.passengerBtn.setOnClickListener(v -> showUserTypeDialog());

		binding.cancelBtn.setOnClickListener(v -> goToLoginOrRegisterAcivity());

	}

	@Override
	public void onBackPressed() {
		goToLoginOrRegisterAcivity();
		super.onBackPressed();
	}

	private void goToLoginOrRegisterAcivity() {
		intent = new Intent(this, LoginOrRegisterActivity.class);
		startActivity(intent);
		finish();
	}

	private void closeUserTypeDialog() {
		if (userTypeDialog != null && userTypeDialog.isShowing()) {
			userTypeDialog.dismiss();
		}
	}

	private void showUserTypeDialog() {
		builder = new AlertDialog.Builder(this);

		DialogUserTypeBinding dialogUserTypeBinding = DialogUserTypeBinding.inflate(getLayoutInflater());
		View dialogView = dialogUserTypeBinding.getRoot();

		dialogUserTypeBinding.seniorBtn.setOnClickListener(v -> {
			intent = new Intent(RegisterUserTypeActivity.this, RegisterActivity.class);
			userType = "Senior Citizen";
			intent.putExtra("userType", userType);
			intent.putExtra("registerType", registerType);
			startActivity(intent);
			finish();

			closeUserTypeDialog();
		});

		dialogUserTypeBinding.pwdBtn.setOnClickListener(v -> {
			intent = new Intent(RegisterUserTypeActivity.this, RegisterActivity.class);
			userType = "Person with Disabilities (PWD)";
			intent.putExtra("userType", userType);
			intent.putExtra("registerType", registerType);
			startActivity(intent);
			finish();

			closeUserTypeDialog();
		});

		dialogUserTypeBinding.cancelBtn.setOnClickListener(v -> {
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