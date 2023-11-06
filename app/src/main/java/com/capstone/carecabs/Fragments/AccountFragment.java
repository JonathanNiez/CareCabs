package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.FeedbackActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoggingOutActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Register.RegisterDriverActivity;
import com.capstone.carecabs.Register.RegisterPWDActivity;
import com.capstone.carecabs.Register.RegisterSeniorActivity;
import com.capstone.carecabs.ScanIDActivity;
import com.capstone.carecabs.TripsActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentAccountBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

public class AccountFragment extends Fragment implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "AccountFragment";
	private float textSizeSP;
	private float textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private DocumentReference documentReference;
	private String userType;
	private final String fontSize = StaticDataPasser.storeFontSize;
	private final String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private Intent intent;
	private AlertDialog.Builder builder;
	private AlertDialog signOutDialog, pleaseWaitDialog,
	noInternetDialog, registerNotCompleteDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private FragmentTransaction fragmentTransaction;
	private FragmentManager fragmentManager;
	private RequestManager requestManager;
	private VoiceAssistant voiceAssistant;
	private FragmentAccountBinding binding;

	@Override
	public void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			Objects.requireNonNull(getContext()).unregisterReceiver(networkChangeReceiver);
		}

		closeSignOutDialog();
		closePleaseWaitDialog();
		closeRegisterNotCompleteDialog();
		closeNoInternetDialog();
	}

	@Override
	public void onPause() {
		super.onPause();

		closeSignOutDialog();
		closePleaseWaitDialog();
		closeRegisterNotCompleteDialog();
		closeNoInternetDialog();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (requestManager != null) {
			requestManager.clear(binding.profilePic);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentAccountBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.disabilityTextView.setVisibility(View.GONE);
		binding.driverStatusTextView1.setVisibility(View.GONE);
		binding.driverStatusTextView2.setVisibility(View.GONE);
		binding.driverRatingTextView.setVisibility(View.GONE);
		binding.idNotScannedTextView.setVisibility(View.GONE);

		context = getContext();
		FirebaseApp.initializeApp(context);
		requestManager = Glide.with(this);

		getUserSettings();

		binding.personalInfoBtn.setOnClickListener(v -> goToPersonalInfoFragment());

		binding.editProfileBtn.setOnClickListener(v -> goToEditAccountFragment());

		binding.aboutBtn.setOnClickListener(v -> goToAboutFragment());

		binding.contactUsBtn.setOnClickListener(v -> goToContactUsFragment());

		binding.changePasswordBtn.setOnClickListener(v -> goToChangePasswordFragment());

		binding.feedbackBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), FeedbackActivity.class);
			startActivity(intent);
		});

		binding.backFloatingBtn.setOnClickListener(v -> backToHomeFragment());

		binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());

		binding.scanIDBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), ScanIDActivity.class);
			intent.putExtra("userType", userType);
			intent.putExtra("activityData", "fromMyProfile");
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		checkUserIfRegisterComplete();

	}

	private void getUserSettings() {

		setFontSize(fontSize);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("My Profile");
		}
	}

	private void logoutUser() {
		intent = new Intent(getActivity(), LoggingOutActivity.class);
		startActivity(intent);
		Objects.requireNonNull(getActivity()).finish();
	}

	private void checkUserIfRegisterComplete() {
		if (FirebaseMain.getUser() != null) {

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot.exists()) {
							boolean getUserRegisterStatus = documentSnapshot.getBoolean("isRegisterComplete");

							if (!getUserRegisterStatus) {
								showRegisterNotCompleteDialog();
							} else {
								loadUserProfileInfo();
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "checkUserIfRegisterComplete: " + e.getMessage()));

		} else {
			Intent intent = new Intent(context, LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	@SuppressLint("SetTextI18n")
	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {

			String userID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {

							closePleaseWaitDialog();

							String getProfilePicture = documentSnapshot.getString("profilePicture");
							String getUserType = documentSnapshot.getString("userType");
							String getFirstName = documentSnapshot.getString("firstname");
							String getLastName = documentSnapshot.getString("lastname");
							boolean getVerificationStatus = documentSnapshot.getBoolean("isVerified");
							String getFontSize = documentSnapshot.getString("fontSize");

							if (getUserType != null && getFontSize != null) {
								userType = getUserType;

								setFontSize(getFontSize);

								switch (userType) {
									case "Driver":
										boolean getDriverStatus = documentSnapshot.getBoolean("isAvailable");
										Double getDriverRatings = documentSnapshot.getDouble("driverRatings");
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

									case "Person with Disabilities (PWD)":
										String getDisability = documentSnapshot.getString("disability");

										binding.disabilityTextView.setVisibility(View.VISIBLE);
										binding.disabilityTextView.setText("Disability:\n" + getDisability);
										binding.userTypeImageView.setImageResource(R.drawable.pwd_64);

										break;

									case "Senior Citizen":
										binding.userTypeImageView.setImageResource(R.drawable.senior_64_2);

										break;
								}
							}

							if (getProfilePicture != null && !getProfilePicture.equals("default")) {
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
								binding.idNotScannedTextView.setVisibility(View.VISIBLE);

								Typeface typeface = ResourcesCompat.getFont(context, R.font.gabarito_bold);

								binding.scanIDBtn.setText("Scan ID (Scan your ID here)");
								binding.scanIDBtn.setTypeface(typeface);
								binding.scanIDBtn.setTextColor(getResources().getColor(R.color.red));

							} else {
								binding.imageViewVerificationMark.setImageResource(R.drawable.check_24);
								binding.scanIDBtn.setText("Rescan ID");
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
					})
					.addOnFailureListener(e -> {
						closePleaseWaitDialog();

						Log.e(TAG, "loadUserProfileInfo: " + e.getMessage());
					});
		} else {
			closePleaseWaitDialog();
		}
	}

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.myProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.idNotScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.feedbackBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}
	@Override
	public void onFontSizeChanged(boolean isChecked) {
		if (isChecked) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}
		binding.myProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.idNotScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverStatusTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverStatusTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.personalInfoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editProfileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.scanIDBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.aboutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.contactUsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.feedbackBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.signOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
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
			Objects.requireNonNull(getActivity()).finish();

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

		voiceAssistant.speak("Are you sure you want to Sign out?");

		signOutBtn.setOnClickListener(v -> logoutUser());

		cancelBtn.setOnClickListener(v -> closeSignOutDialog());

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
		Objects.requireNonNull(getContext()).registerReceiver(networkChangeReceiver, intentFilter);

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