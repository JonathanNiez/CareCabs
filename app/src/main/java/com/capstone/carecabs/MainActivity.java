package com.capstone.carecabs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Fragments.AboutFragment;
import com.capstone.carecabs.Fragments.AccountFragment;
import com.capstone.carecabs.Fragments.AppSettingsFragment;
import com.capstone.carecabs.Fragments.ChangeFontSizeFragment;
import com.capstone.carecabs.Fragments.ChangePasswordFragment;
import com.capstone.carecabs.Fragments.ContactUsFragment;
import com.capstone.carecabs.Fragments.EditAccountFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.Fragments.PersonalInfoFragment;
import com.capstone.carecabs.Utility.LocationPermissionChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {
	private Intent intent;
	private AlertDialog exitAppDialog;
	private AlertDialog.Builder builder;
	private final String TAG = "MainActivity";
	private boolean shouldExit = false;
	private EditAccountFragment editAccountFragment;
	private DocumentReference documentReference;
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

			} else if (item.getItemId() == R.id.myProfile) {
				showFragment(new AccountFragment());

			} else if (item.getItemId() == R.id.map) {

				if (LocationPermissionChecker.isLocationPermissionGranted(this)) {
					getUserType();
				} else {
					intent = new Intent(MainActivity.this, RequestLocationPermissionActivity.class);
					startActivity(intent);
					finish();
				}

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
		} else if (currentFragment instanceof AccountFragment) {
			((AccountFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof PersonalInfoFragment) {
			((PersonalInfoFragment) currentFragment).onBackPressed();

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

	private void getUserType() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {
					String getUserType = documentSnapshot.getString("userType");

					switch (getUserType) {
						case "Driver":
							intent = new Intent(MainActivity.this, MapDriverActivity.class);
							break;

						case "Senior Citizen":
						case "Persons with Disability (PWD)":
							intent = new Intent(MainActivity.this, MapPassengerActivity.class);

							break;

					}
					startActivity(intent);
					finish();
				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.e(TAG, e.getMessage());
				}
			});
		}
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

		Button exitBtn = dialogView.findViewById(R.id.exitBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		exitBtn.setOnClickListener(v -> {
			exitApp();
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