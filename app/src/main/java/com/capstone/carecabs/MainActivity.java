package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Fragments.AboutFragment;
import com.capstone.carecabs.Fragments.AccountFragment;
import com.capstone.carecabs.Fragments.AppSettingsFragment;
import com.capstone.carecabs.Fragments.ChangeFontSizeFragment;
import com.capstone.carecabs.Fragments.ChangePasswordFragment;
import com.capstone.carecabs.Fragments.ContactUsFragment;
import com.capstone.carecabs.Fragments.EditAccountFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
	private Intent intent;
	private AlertDialog exitAppDialog;
	private AlertDialog.Builder builder;
	private final String TAG = "MainActivity";
	private boolean shouldExit = false;
	private EditAccountFragment editAccountFragment;
	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		showFragment(new HomeFragment());

		binding.bottomNavigationView.setSelectedItemId(R.id.home);
		binding.bottomNavigationView.setOnItemSelectedListener(item -> {

			if (item.getItemId() == R.id.home) {
				showFragment(new HomeFragment());

			} else if (item.getItemId() == R.id.account) {
				showFragment(new AccountFragment());

			} else if (item.getItemId() == R.id.map) {
				intent = new Intent(MainActivity.this, MapActivity.class);
				startActivity(intent);
			}
			return true;
		});
	}


	private void exitApp() {
		shouldExit = true;
		onBackPressed();

		finish();
	}

	@Override
	public void onBackPressed() {
		Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

		if (currentFragment instanceof AboutFragment) {
			((AboutFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof ContactUsFragment) {
			((ContactUsFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof EditAccountFragment) {
			((EditAccountFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof ChangePasswordFragment) {
			((ChangePasswordFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof AppSettingsFragment) {
			((AppSettingsFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof ChangeFontSizeFragment) {
			((ChangeFontSizeFragment) currentFragment).onBackPressed();

			return;
		}


		if (shouldExit) {
			super.onBackPressed(); // Exit the app
		} else {
			// Show an exit confirmation dialog
			showExitConfirmationDialog();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		updateDriverStatus(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		closeExitConfirmationDialog();
		updateDriverStatus(false);
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeExitConfirmationDialog();
		updateDriverStatus(false);
	}

	private void updateDriverStatus(boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);
		}
	}

	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_app, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			exitApp();
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

	private void showFragment(Fragment fragment) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, fragment);
		fragmentTransaction.commit();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

		if (editAccountFragment != null) {
			editAccountFragment.onActivityResult(requestCode, resultCode, data);
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

		if (editAccountFragment != null) {
			editAccountFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}

	}

}