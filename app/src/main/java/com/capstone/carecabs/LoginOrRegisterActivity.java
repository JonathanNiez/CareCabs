package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.capstone.carecabs.Register.RegisterUserTypeActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityLoginOrRegisterBinding;
import com.capstone.carecabs.databinding.DialogRegisterUsingBinding;

public class LoginOrRegisterActivity extends AppCompatActivity {

	private ActivityLoginOrRegisterBinding binding;
	private Intent intent;
	private NetworkChangeReceiver networkChangeReceiver;
	private AlertDialog exitAppDialog, registerUsingDialog,
			noInternetDialog;
	private AlertDialog.Builder builder;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
		closeRegisterUsingDialog();
		closeExitConfirmationDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeNoInternetDialog();
		closeRegisterUsingDialog();
		closeExitConfirmationDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginOrRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.loginBtn.setOnClickListener(v -> {
			intent = new Intent(LoginOrRegisterActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		});

		binding.registerBtn.setOnClickListener(v -> {
			showRegisterUsingDialog();
		});

	}

	@Override
	public void onBackPressed() {
		showExitConfirmationDialog();
	}

	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_exit_app, null);

		Button exitBtn = dialogView.findViewById(R.id.exitBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		exitBtn.setOnClickListener(v -> finish());

		cancelBtn.setOnClickListener(v -> closeExitConfirmationDialog());

		builder.setView(dialogView);
		exitAppDialog = builder.create();
		exitAppDialog.show();
	}

	private void closeExitConfirmationDialog() {
		if (exitAppDialog != null && exitAppDialog.isShowing()) {
			exitAppDialog.dismiss();
		}
	}

	private void showRegisterUsingDialog() {

		builder = new AlertDialog.Builder(this);
		DialogRegisterUsingBinding dialogRegisterUsingBinding =
				DialogRegisterUsingBinding.inflate(getLayoutInflater());
		View dialogView = dialogRegisterUsingBinding.getRoot();

		dialogRegisterUsingBinding.googleBtn.setOnClickListener(v -> {
			intent = new Intent(LoginOrRegisterActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "Google");
			startActivity(intent);
			finish();

			closeRegisterUsingDialog();
		});

		dialogRegisterUsingBinding.emailBtn.setOnClickListener(v -> {
			intent = new Intent(LoginOrRegisterActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "Email");
			startActivity(intent);
			finish();

			closeRegisterUsingDialog();
		});

		dialogRegisterUsingBinding.cancelBtn.setOnClickListener(v -> {
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