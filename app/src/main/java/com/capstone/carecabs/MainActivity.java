package com.capstone.carecabs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
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
import com.capstone.carecabs.Fragments.ChangeLanguageFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.Fragments.PersonalInfoFragment;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Utility.LocationPermissionChecker;
import com.capstone.carecabs.Utility.NotificationHelper;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityMainBinding;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {
	private Intent intent;
	private AlertDialog exitAppDialog;
	private AlertDialog.Builder builder;
	private final String TAG = "MainActivity";
	private boolean shouldExit = false;
	private BadgeDrawable badgeDrawable;
	private EditAccountFragment editAccountFragment;
	private DocumentReference documentReference;
	private DatabaseReference bookingReference;
	private ActivityMainBinding binding;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		badgeDrawable = binding.bottomNavigationView.getOrCreateBadge(R.id.myProfile);

		checkUserIfVerified();

		showFragment(new HomeFragment());

		binding.bottomNavigationView.setSelectedItemId(R.id.home);
		binding.bottomNavigationView.setOnItemSelectedListener(item -> {

			if (item.getItemId() == R.id.home) {
				showFragment(new HomeFragment());

			} else if (item.getItemId() == R.id.myProfile) {
				showFragment(new AccountFragment());

			} else if (item.getItemId() == R.id.map) {

				if (LocationPermissionChecker.isLocationPermissionGranted(this)) {
					getUserTypeForMap();
				} else {
					intent = new Intent(MainActivity.this, RequestLocationPermissionActivity.class);
					startActivity(intent);
					finish();
				}

			}
			return true;
		});
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
		} else if (currentFragment instanceof PersonalInfoFragment) {
			((PersonalInfoFragment) currentFragment).onBackPressed();

			return;
		} else if (currentFragment instanceof ChangeLanguageFragment) {
			((ChangeLanguageFragment) currentFragment).onBackPressed();

			return;
		}


		if (shouldExit) {
			super.onBackPressed(); // Exit the app
		} else {
			// Show an exit confirmation dialog
			showExitConfirmationDialog();
		}

	}

	private void exitApp() {
		shouldExit = true;
		onBackPressed();

		finish();
	}

	private void checkIfBookingIsAccepted() {

		bookingReference = FirebaseDatabase.getInstance()
				.getReference(StaticDataPasser.bookingCollection);

		bookingReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot != null && snapshot.exists()) {
					for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingData =
								locationSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingData != null) {

							if (passengerBookingData.getBookingStatus() == "") {
								showBookingIsAcceptedNotification();
							}
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());

			}
		});
	}

	private void showBookingIsAcceptedNotification() {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showBookingIsAcceptedNotificationNotification("CareCabs", "A Driver has accepted your Booking and is on the way to pick up you");
	}

	private void checkUserIfVerified() {
		if (FirebaseMain.getUser() != null) {

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {
					boolean getVerificationStatus = documentSnapshot.getBoolean("isVerified");

					badgeDrawable = binding.bottomNavigationView.getBadge(R.id.myProfile);

					if (!getVerificationStatus) {
						if (badgeDrawable != null) {
							badgeDrawable.setVisible(true);
							badgeDrawable.setNumber(1);
						}

						showProfileNotVerifiedNotification();

					} else {
						binding.bottomNavigationView.removeBadge(R.id.myProfile);
						getUserTypeToCheckIfBookingIsAccepted();
					}

				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
		} else {
			FirebaseMain.signOutUser();

			intent = new Intent(MainActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void checkWaitingPassengers() {
		bookingReference = FirebaseDatabase.getInstance()
				.getReference(StaticDataPasser.bookingCollection);
		bookingReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot != null && snapshot.exists()) {
					for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingData =
								bookingSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingData != null) {

							badgeDrawable = binding.bottomNavigationView.getBadge(R.id.map);

							if (passengerBookingData.getBookingStatus().equals("Waiting")) {
								showPassengersWaitingNotification();

								if (badgeDrawable != null) {
									badgeDrawable.setVisible(true);
									badgeDrawable.setNumber(1);
								}
							}else{
								if (badgeDrawable != null) {
									binding.bottomNavigationView.removeBadge(R.id.map);
								}
							}
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}

	private void getUserTypeForMap() {
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
					overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
					startActivity(intent);
					finish();
				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
		}
	}

	private void getUserTypeToCheckIfBookingIsAccepted() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {
					String getUserType = documentSnapshot.getString("userType");

					if (getUserType.equals("Senior Citizen") || getUserType.equals("Persons with Disability (PWD)")) {
						checkIfBookingIsAccepted();
					} else {
						checkWaitingPassengers();
					}

				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
		}
	}


	private void updateDriverStatus(boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);
		}
	}

	private void showProfileNotVerifiedNotification() {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showProfileNotVerifiedNotification("CareCabs",
				"Your Profile is not Verified. Please scan your ID to Verify");
	}

	private void showPassengersWaitingNotification() {
		NotificationHelper notificationHelper = new NotificationHelper(this);
		notificationHelper.showPassengersWaitingNotification("CareCabs",
				"There are Passenger(s) waiting right now. Go to Map to check their location");
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