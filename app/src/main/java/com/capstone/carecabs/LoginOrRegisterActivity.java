package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.capstone.carecabs.databinding.ActivityLoginOrRegisterBinding;

public class LoginOrRegisterActivity extends AppCompatActivity {

	private ActivityLoginOrRegisterBinding binding;
	private Intent intent;
	private AlertDialog exitAppDialog, registerUsingDialog;
	private AlertDialog.Builder builder;
	private boolean shouldExit = false;

	@Override
	protected void onPause() {
		super.onPause();

		closeRegisterUsingDialog();
		closeExitConfirmationDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

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
			overridePendingTransition(R.anim.popup_enter, R.anim.popup_exit);
			finish();
		});

		binding.registerBtn.setOnClickListener(v -> {
			showRegisterUsingDialog();
		});

	}

	@Override
	public void onBackPressed() {

		if (shouldExit) {
			super.onBackPressed();
		} else {
			showExitConfirmationDialog();

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

	private void showRegisterUsingDialog() {

		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_using, null);

		ImageButton googleImgBtn = dialogView.findViewById(R.id.googleImgBtn);
		ImageButton emailImgBtn = dialogView.findViewById(R.id.emailImgBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		googleImgBtn.setOnClickListener(v -> {
			intent = new Intent(LoginOrRegisterActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "googleRegister");
			startActivity(intent);
			overridePendingTransition(R.anim.popup_enter, R.anim.popup_exit);
			finish();

			closeRegisterUsingDialog();
		});

		emailImgBtn.setOnClickListener(v -> {
			intent = new Intent(LoginOrRegisterActivity.this, RegisterUserTypeActivity.class);
			intent.putExtra("registerType", "emailRegister");
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
			finish();

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

}