package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoggingOutActivity;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.RegisterDriverActivity;
import com.capstone.carecabs.RegisterPWDActivity;
import com.capstone.carecabs.RegisterSeniorActivity;
import com.capstone.carecabs.ScanIDActivity;
import com.capstone.carecabs.TripsOverviewActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentAccountBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

public class AccountFragment extends Fragment {
	private DocumentReference documentReference;
	private final String TAG = "AccountFragment";
	private String userID;
	private Intent intent;
	private boolean shouldExit = false;

	private AlertDialog signOutDialog, pleaseWaitDialog,
			noInternetDialog, registerNotCompleteDialog,
			exitAppDialog;
	private AlertDialog.Builder builder;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private FragmentAccountBinding binding;
	private FragmentTransaction fragmentTransaction;
	private FragmentManager fragmentManager;

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			getContext().unregisterReceiver(networkChangeReceiver);
		}

		closeSignOutDialog();
		closePleaseWaitDialog();
		closeRegisterNotCompleteDialog();
		closeNoInternetDialog();
		closeExitConfirmationDialog();

	}


	@Override
	public void onPause() {
		super.onPause();

		closeSignOutDialog();
		closePleaseWaitDialog();
		closeRegisterNotCompleteDialog();
		closeNoInternetDialog();
		closeExitConfirmationDialog();

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		closeSignOutDialog();
		closePleaseWaitDialog();
		closeRegisterNotCompleteDialog();
		closeNoInternetDialog();
		closeExitConfirmationDialog();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentAccountBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.disabilityTextView.setVisibility(View.GONE);
		binding.medConTextView.setVisibility(View.GONE);
		binding.driverStatusTextView1.setVisibility(View.GONE);
		binding.driverStatusTextView2.setVisibility(View.GONE);
		binding.driverRatingTextView.setVisibility(View.GONE);
		binding.idScannedTextView.setVisibility(View.GONE);

		context = getContext();
		initializeNetworkChecker();
		FirebaseApp.initializeApp(context);

		binding.personalInfoBtn.setOnClickListener(v -> goToPersonalInfoFragment());

		binding.editProfileBtn.setOnClickListener(v -> goToEditAccountFragment());

		binding.aboutBtn.setOnClickListener(v -> goToAboutFragment());

		binding.contactUsBtn.setOnClickListener(v -> goToContactUsFragment());

		binding.appSettingsBtn.setOnClickListener(v -> goToAppSettingsFragment());

		binding.tripsBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), TripsOverviewActivity.class);
			startActivity(intent);
		});

		binding.changePasswordBtn.setOnClickListener(v -> goToChangePasswordFragment());

		binding.imgBackBtn.setOnClickListener(v -> backToHomeFragment());

		binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());

		binding.scanIDBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), ScanIDActivity.class);
			intent.putExtra("userType", "From Main");
			startActivity(intent);
			getActivity().finish();
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		checkUserIfRegisterComplete();

	}

	private void logoutUser() {
		intent = new Intent(getActivity(), LoggingOutActivity.class);
		startActivity(intent);
		getActivity().finish();
	}

	private void checkUserIfRegisterComplete() {
		if (FirebaseMain.getUser() != null) {
			String getUserID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(getUserID);
			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot.exists()) {
					boolean getUserRegisterStatus = documentSnapshot.getBoolean("isRegisterComplete");

					if (!getUserRegisterStatus) {
						showRegisterNotCompleteDialog();
					} else {
						loadUserProfileInfo();
					}
				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));

		} else {
			FirebaseMain.signOutUser();

			Intent intent = new Intent(context, LoginActivity.class);
			startActivity(intent);
			getActivity().finish();
		}
	}

	private void getCurrentFontSizeFromUserSetting() {

		switch (StaticDataPasser.storeFontSize) {
			case 15:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

				binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.appSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

				break;

			case 17:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

				binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.appSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

				break;

			case 19:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);

				binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.appSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);

				binding.personalInfoBtn.setHeight(62);
				binding.editProfileBtn.setHeight(62);
				binding.changePasswordBtn.setHeight(62);
				binding.editProfileBtn.setHeight(62);
				binding.aboutBtn.setHeight(62);
				binding.contactUsBtn.setHeight(62);
				binding.signOutBtn.setHeight(62);
				binding.scanIDBtn.setHeight(62);

				break;

			case 21:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

				binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.appSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

				binding.personalInfoBtn.setHeight(64);
				binding.editProfileBtn.setHeight(64);
				binding.changePasswordBtn.setHeight(64);
				binding.editProfileBtn.setHeight(64);
				binding.aboutBtn.setHeight(64);
				binding.contactUsBtn.setHeight(64);
				binding.signOutBtn.setHeight(64);
				binding.scanIDBtn.setHeight(64);

				break;
		}
	}

	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {
			getCurrentFontSizeFromUserSetting();

			userID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {

					closePleaseWaitDialog();

					String getProfilePicture = documentSnapshot.getString("profilePicture");
					String getUserType = documentSnapshot.getString("userType");
					String getFirstName = documentSnapshot.getString("firstname");
					String getLastName = documentSnapshot.getString("lastname");
					boolean getVerificationStatus = documentSnapshot.getBoolean("isVerified");

					switch (getUserType) {
						case "Driver":
							boolean getDriverStatus = documentSnapshot.getBoolean("isAvailable");
							Long getDriverRatingsLong = documentSnapshot.getLong("driverRating");
							int getDriverRatings = getDriverRatingsLong.intValue();
							Long getPassengersTransported = documentSnapshot.getLong("passengersTransported");

							binding.driverStatusTextView1.setVisibility(View.VISIBLE);
							binding.driverStatusTextView2.setVisibility(View.VISIBLE);
							binding.driverRatingTextView.setVisibility(View.VISIBLE);
							binding.userTypeImageView.setImageResource(R.drawable.driver_64);

							if (getDriverStatus) {
								binding.driverStatusTextView2.setTextColor(Color.BLUE);
								binding.driverStatusTextView2.setText("Available");

							} else {
								binding.driverStatusTextView2.setTextColor(Color.RED);
								binding.driverStatusTextView2.setText("Busy");

							}

							binding.driverRatingTextView.setText("Driver Rating: " + getDriverRatings);
							break;

						case "Persons with Disabilities (PWD)":
							String getDisability = documentSnapshot.getString("disability");

							binding.disabilityTextView.setVisibility(View.VISIBLE);
							binding.disabilityTextView.setText("Disabilities:\n" + getDisability);
							binding.userTypeImageView.setImageResource(R.drawable.pwd_64);


							break;

						case "Senior Citizen":
							String getMedicalCondition = documentSnapshot.getString("medicalCondition");

							binding.medConTextView.setVisibility(View.VISIBLE);
							binding.medConTextView.setText("Medical Conditions:\n" + getMedicalCondition);
							binding.userTypeImageView.setImageResource(R.drawable.senior_64_2);

							break;
					}

					if (!getProfilePicture.equals("default")) {
						Glide.with(context)
								.load(getProfilePicture)
								.centerCrop()
								.placeholder(R.drawable.loading_gif)
								.into(binding.profilePic);
					}

					if (!getVerificationStatus) {
						binding.imageViewVerificationMark.setImageResource(R.drawable.x_24);

						binding.verificationStatusTextView.setTextColor(
								getResources().getColor(R.color.light_red)
						);
						binding.verificationStatusTextView.setText("Not Verified");

						binding.idScannedTextView.setVisibility(View.VISIBLE);

						Typeface typeface = ResourcesCompat.getFont(context, R.font.opensans_bold);

						binding.scanIDBtn.setText("Scan ID (Scan your ID here)");
						binding.scanIDBtn.setTypeface(typeface);
						binding.scanIDBtn.setTextColor(getResources().getColor(R.color.red));
					} else {
						binding.imageViewVerificationMark.setImageResource(R.drawable.check_24);

						binding.verificationStatusTextView.setTextColor(
								getResources().getColor(R.color.green)
						);
						binding.verificationStatusTextView.setText("Verified");
					}

					binding.firstnameTextView.setText(getFirstName);
					binding.lastnameTextView.setText(getLastName);
					binding.userTypeTextView.setText(getUserType);

				} else {
					closePleaseWaitDialog();

				}
			}).addOnFailureListener(e -> {
				closePleaseWaitDialog();

				Log.e(TAG, e.getMessage());
			});
		} else {
			closePleaseWaitDialog();

		}
	}


	private void showExitConfirmationDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_app, null);

		Button exitBtn = dialogView.findViewById(R.id.exitBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		exitBtn.setOnClickListener(v -> {
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


	private void showRegisterNotCompleteDialog() {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_info_not_complete, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {

			switch (StaticDataPasser.storeRegisterUserType) {
				case "Driver":
					intent = new Intent(getActivity(), RegisterDriverActivity.class);

					break;

				case "Senior Citizen":
					intent = new Intent(getActivity(), RegisterSeniorActivity.class);

					break;

				case "Persons with Disabilities (PWD)":
					intent = new Intent(getActivity(), RegisterPWDActivity.class);

					break;

			}
			startActivity(intent);
			getActivity().finish();

		});

		builder.setView(dialogView);

		registerNotCompleteDialog = builder.create();
		registerNotCompleteDialog.show();
	}

	private void closeRegisterNotCompleteDialog() {
		if (registerNotCompleteDialog != null && registerNotCompleteDialog.isShowing()) {
			registerNotCompleteDialog.isShowing();
		}
	}

	private void showSignOutDialog() {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_sign_out, null);

		Button signOutBtn = dialogView.findViewById(R.id.signOutBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		signOutBtn.setOnClickListener(v -> {
			logoutUser();

		});

		cancelBtn.setOnClickListener(v -> {
			closeSignOutDialog();
		});

		builder.setView(dialogView);

		signOutDialog = builder.create();
		signOutDialog.show();
	}

	private void closeSignOutDialog() {
		if (signOutDialog != null && signOutDialog.isShowing()) {
			signOutDialog.dismiss();
		}
	}

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_please_wait, null);

		builder.setView(dialogView);

		pleaseWaitDialog = builder.create();
		pleaseWaitDialog.show();
	}

	private void closePleaseWaitDialog() {
		if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
			pleaseWaitDialog.dismiss();
		}
	}

	private void goToPersonalInfoFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new PersonalInfoFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToEditAccountFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new EditAccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToAppSettingsFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AppSettingsFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToAboutFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AboutFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToContactUsFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new ContactUsFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToChangePasswordFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new ChangePasswordFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void backToHomeFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new HomeFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(context);
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

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(context);
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
		getContext().registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(getContext());
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