package com.capstone.carecabs;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Chat.ChatOverviewActivity;
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
import com.capstone.carecabs.Map.MapDriverActivity;
import com.capstone.carecabs.Map.MapPassengerActivity;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Utility.NotificationHelper;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityMainBinding;
import com.capstone.carecabs.databinding.DialogEnableLocationServiceBinding;
import com.capstone.carecabs.databinding.DialogExitAppBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
	private final String TAG = "MainActivity";
	private static final String THEME_NORMAL = "normal";
	private static final String THEME_CONTRAST = "contrast";
	private static final int REQUEST_ENABLE_LOCATION = 1;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private Intent intent;
	private AlertDialog.Builder builder;
	private AlertDialog exitAppDialog, enableLocationServiceDialog;
	private Fragment currentFragment;
	private boolean shouldExit = false;
	private EditAccountFragment editAccountFragment;
	private DocumentReference documentReference;
	private VoiceAssistant voiceAssistant;
	private ActivityMainBinding binding;

	@Override
	protected void onResume() {
		super.onResume();

//		updateDriverStatus(true);
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		closeExitConfirmationDialog();
		closeEnableLocationServiceDialog();

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.shutdown();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		closeExitConfirmationDialog();
		closeEnableLocationServiceDialog();
//		updateDriverStatus(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		checkUserIfVerified();

		if (getIntent() != null && getIntent().hasExtra("activityData")) {
			intent = getIntent();
			String activityData = intent.getStringExtra("activityData");

			if (activityData != null) {
				if (activityData.equals("fromMyProfile")) {
					showFragment(new AccountFragment());

				}
			}

		} else {
			showFragment(new HomeFragment());
		}

		binding.settingsFloatingBtn.setOnClickListener(v -> showSettingsBottomSheet());

		binding.bottomNavigationView.setSelectedItemId(R.id.home);
		binding.bottomNavigationView.setOnItemSelectedListener(item -> {

			if (item.getItemId() == R.id.home) {
				showFragment(new HomeFragment());
			} else if (item.getItemId() == R.id.chat) {
				intent = new Intent(MainActivity.this, ChatOverviewActivity.class);
				startActivity(intent);

			}
			return true;
		});

	}

	@Override
	public void onBackPressed() {
		currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

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
			super.onBackPressed();
		} else {
			showExitConfirmationDialog();
		}
	}

	private void showSettingsBottomSheet() {
		currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

		SettingsBottomSheet.FontSizeChangeListener fontSizeChangeListener = null;
		SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();

		if (currentFragment instanceof HomeFragment) {
			fontSizeChangeListener = (HomeFragment) currentFragment;

		} else if (currentFragment instanceof AccountFragment) {
			fontSizeChangeListener = (AccountFragment) currentFragment;

		} else if (currentFragment instanceof AboutFragment) {
			fontSizeChangeListener = (AboutFragment) currentFragment;

		} else if (currentFragment instanceof ContactUsFragment) {
			fontSizeChangeListener = (ContactUsFragment) currentFragment;

		} else if (currentFragment instanceof EditAccountFragment) {
			fontSizeChangeListener = (EditAccountFragment) currentFragment;

		} else if (currentFragment instanceof ChangePasswordFragment) {
			fontSizeChangeListener = (ChangePasswordFragment) currentFragment;

		} else if (currentFragment instanceof AppSettingsFragment) {
			fontSizeChangeListener = (AppSettingsFragment) currentFragment;

		} else if (currentFragment instanceof PersonalInfoFragment) {
			fontSizeChangeListener = (PersonalInfoFragment) currentFragment;
		}

		if (fontSizeChangeListener != null) {
			settingsBottomSheet.setFontSizeChangeListener(fontSizeChangeListener);
		}
		settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
	}

	private void exitApp() {
		shouldExit = true;
		finishAffinity();
		super.onBackPressed();
	}

	private void retrieveAndStoreFCMToken() {
		FirebaseMessaging.getInstance().getToken()
				.addOnSuccessListener(this::updateFCMTokenInFireStore)
				.addOnFailureListener(e -> Log.e(TAG, "retrieveAndStoreFCMToken: " + e.getMessage()));
	}

	private void updateFCMTokenInFireStore(String token) {
		DocumentReference documentReference = FirebaseFirestore.getInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		documentReference.update("fcmToken", token)
				.addOnSuccessListener(aVoid -> {

					StaticDataPasser.storeFCMToken = token;

					Log.i(TAG, "Device token stored: " + StaticDataPasser.storeFCMToken);
				})
				.addOnFailureListener(e -> Log.e(TAG, "updateFCMTokenInFireStore: " + e.getMessage()));
	}

	private void getUserSettingsFromFireStore() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot.exists()) {
							String getTheme = documentSnapshot.getString("theme");
							String getFontSize = documentSnapshot.getString("fontSize");
							String getVoiceAssistant = documentSnapshot.getString("voiceAssistant");

							if (getTheme != null && getFontSize != null && getVoiceAssistant != null) {

								setTheme(getTheme);

								StaticDataPasser.storeTheme = getTheme;
								StaticDataPasser.storeFontSize = getFontSize;
								StaticDataPasser.storeVoiceAssistantState = getVoiceAssistant;
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "getThemeSettingsFromFireStore - onFailure: " + e.getMessage()));
		}
	}

	private void setTheme(String theme) {
		if (theme.equals(THEME_CONTRAST)) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
		} else if (theme.equals(THEME_NORMAL)) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		} else {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}
	}

	private void checkLocationService() {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!isGpsEnabled && !isNetworkEnabled) {

			showEnableLocationServiceDialog();
		} else {
			getUserTypeForMap();
		}
	}

	private void checkIfBookingIsAccepted() {
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		bookingReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingData =
								locationSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingData != null) {

							if (passengerBookingData.getBookingStatus().equals("Standby")) {
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
		notificationHelper.showBookingIsAcceptedNotificationNotification("CareCabs",
				"A Driver has accepted your Booking and is on the way to pick up you");
	}

	private void checkUserIfVerified() {
		if (FirebaseMain.getUser() != null) {

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							boolean isVerified = documentSnapshot.getBoolean("isVerified");

							if (isVerified) {
								retrieveAndStoreFCMToken();
								getUserTypeToCheckBookingStatus();
								getUserSettingsFromFireStore();

							} else {
								showProfileNotVerifiedNotification();

							}

						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "checkUserIfVerified: " + e.getMessage()));
		} else {
			intent = new Intent(MainActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void checkForWaitingPassengers() {
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);
		bookingReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					int waitingPassengersCount = 0;

					for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingData =
								bookingSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingData != null) {

							if (passengerBookingData.getBookingStatus().equals("Waiting")) {
								showPassengersWaitingNotification();
								waitingPassengersCount++;

							} else {

							}
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, "checkForWaitingPassengers: onCancelled " + error.getMessage());
			}
		});

	}

	private void getUserTypeForMap() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							String getUserType = documentSnapshot.getString("userType");

							if (getUserType != null) {
								switch (getUserType) {
									case "Driver":
										intent = new Intent(MainActivity.this, MapDriverActivity.class);
										break;

									case "Senior Citizen":
									case "Person with Disabilities (PWD)":

										intent = new Intent(MainActivity.this, MapPassengerActivity.class);

										break;

								}
							}
							startActivity(intent);
							finish();
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "getUserTypeForMap: " + e.getMessage()));
		}
	}

	private void getUserTypeToCheckBookingStatus() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							String getUserType = documentSnapshot.getString("userType");

							if (getUserType != null ){
								if (getUserType.equals("Driver")) {

									checkForWaitingPassengers();
								} else {
									checkIfBookingIsAccepted();

								}
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "getUserTypeToCheckBookingStatus: " + e.getMessage()));
		}

	}

	private void updateDriverStatus(boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(FirebaseMain.userCollection)
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
				"There are Passenger(s) waiting right now.\nGo to Map to check their location");
	}

	private void showEnableLocationServiceDialog() {
		builder = new AlertDialog.Builder(this);

		DialogEnableLocationServiceBinding binding = DialogEnableLocationServiceBinding.inflate(getLayoutInflater());
		View dialogView = binding.getRoot();

		binding.enableLocationServiceBtn.setOnClickListener(v -> {
			intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, REQUEST_ENABLE_LOCATION);

			closeEnableLocationServiceDialog();
		});

		builder.setView(dialogView);

		enableLocationServiceDialog = builder.create();
		enableLocationServiceDialog.show();
	}

	private void closeEnableLocationServiceDialog() {
		if (enableLocationServiceDialog != null && enableLocationServiceDialog.isShowing()) {
			enableLocationServiceDialog.dismiss();
		}
	}

	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(this);

		DialogExitAppBinding dialogExitAppBinding = DialogExitAppBinding.inflate(getLayoutInflater());
		View dialogView = dialogExitAppBinding.getRoot();

		if (fontSize.equals("large")) {
			dialogExitAppBinding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogExitAppBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			dialogExitAppBinding.cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			dialogExitAppBinding.exitBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Are you sure you want to exit the App?");
		}

		dialogExitAppBinding.exitBtn.setOnClickListener(v -> {
			exitApp();

			closeExitConfirmationDialog();
		});

		dialogExitAppBinding.cancelBtn.setOnClickListener(v -> closeExitConfirmationDialog());

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

		if (requestCode == REQUEST_ENABLE_LOCATION) {
			// Check if the user enabled location services after going to settings.
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (isGpsEnabled || isNetworkEnabled) {
				getUserTypeForMap();
			} else {
				// Location services are still not enabled, you can show a message to the user.
				Toast.makeText(this, "Location services are still disabled.", Toast.LENGTH_SHORT).show();
			}
		}

		editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

		if (editAccountFragment != null) {
			editAccountFragment.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

		if (editAccountFragment != null) {
			editAccountFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}