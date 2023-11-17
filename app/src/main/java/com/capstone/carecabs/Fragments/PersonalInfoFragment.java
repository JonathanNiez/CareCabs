package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentPersonalInfoBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

public class PersonalInfoFragment extends Fragment implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "FragmentPersonalInfo";
	private float textSizeSP;
	private float textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private AlertDialog.Builder builder;
	private AlertDialog pleaseWaitDialog, noInternetDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private RequestManager requestManager;
	private String fontSize = StaticDataPasser.storeFontSize;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private VoiceAssistant voiceAssistant;
	private FragmentPersonalInfoBinding binding;

	@Override
	public void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	public void onPause() {
		super.onPause();

		closePleaseWaitDialog();
		closeNoInternetDialog();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			getContext().unregisterReceiver(networkChangeReceiver);
		}

		closePleaseWaitDialog();
		closeNoInternetDialog();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (requestManager != null) {
			requestManager.clear(binding.idImageView);
			requestManager.clear(binding.vehicleImageView);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentPersonalInfoBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.vehicleInfoLayout.setVisibility(View.GONE);
		binding.medConTextView.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);

		context = getContext();
		FirebaseApp.initializeApp(context);
		requestManager = Glide.with(this);

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (isAdded()) {
			getUserSettings();
			loadUserProfileInfo();
		}
	}

	public void onBackPressed() {
		backToAccountFragment();
	}


	@SuppressLint("SetTextI18n")
	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {

			String userID = FirebaseMain.getUser().getUid();
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							closePleaseWaitDialog();

							String getUserType = documentSnapshot.getString("userType");
							String getFirstName = documentSnapshot.getString("firstname");
							String getLastName = documentSnapshot.getString("lastname");
							Long getAgeLong = documentSnapshot.getLong("age");
							int getAge = getAgeLong.intValue();
							String getEmail = documentSnapshot.getString("email");
							String getPhoneNumber = documentSnapshot.getString("phoneNumber");
							String getSex = documentSnapshot.getString("sex");
							String getBirthdate = documentSnapshot.getString("birthdate");
							String getAccountCreationDate = documentSnapshot.getString("accountCreationDate");
							String getRegisterType = documentSnapshot.getString("registerType");
							String getIDPicture = documentSnapshot.getString("idPicture");

							if (getIDPicture != null && !getIDPicture.equals("none")) {
								Glide.with(context)
										.load(getIDPicture)
										.placeholder(R.drawable.loading_gif)
										.into(binding.idImageView);
							}

							if (getUserType != null) {
								switch (getUserType) {
									case "Driver":
										binding.vehicleInfoLayout.setVisibility(View.VISIBLE);
										binding.idTypeTextView.setText("Driver's License");

										String getVehiclePicture = documentSnapshot.getString("vehiclePicture");
										String getVehicleColor = documentSnapshot.getString("vehicleColor");
										String getVehiclePlateNumber = documentSnapshot.getString("vehiclePlateNumber");

										if (getVehiclePicture != null && !getVehiclePicture.equals("none")) {
											Glide.with(context)
													.load(getVehiclePicture)
													.placeholder(R.drawable.loading_gif)
													.into(binding.vehicleImageView);
										}

										binding.vehicleColorTextView.setText("Vehicle Color: " + getVehicleColor);
										binding.vehiclePlateNumberTextView.setText("Vehicle Plate Number: " + getVehiclePlateNumber);

										break;

									case "Person with Disabilities (PWD)":
										binding.idTypeTextView.setText("PWD ID");

										String getDisability = documentSnapshot.getString("disability");

										binding.disabilityTextView.setVisibility(View.VISIBLE);
										binding.disabilityTextView.setText("Disabilities: " + getDisability);


										break;

									case "Senior Citizen":
										binding.idTypeTextView.setText("Senior Citizen ID");

										String getMedicalCondition = documentSnapshot.getString("medicalCondition");

										binding.medConTextView.setVisibility(View.VISIBLE);
										binding.medConTextView.setText("Medical Conditions: " + getMedicalCondition);

										break;
								}
							}

							binding.firstnameTextView.setText(getFirstName);
							binding.lastnameTextView.setText(getLastName);
							binding.userTypeTextView.setText(getUserType);
							binding.emailTextView.setText("Email: " + getEmail);
							binding.phoneTextView.setText("Phone No: " + getPhoneNumber);
							binding.birthdateTextView.setText("Birthdate: " + getBirthdate);
							binding.ageTextView.setText("Age: " + getAge);
							binding.sexTextView.setText("Sex: " + getSex);
							binding.accountCreationDateTextView.setText("Account creation date: " + getAccountCreationDate);
							binding.registerTypeTextView.setText("Register Type: " + getRegisterType);

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

	private void backToAccountFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void getUserSettings() {

		setFontSize(fontSize);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Personal Info");
		}
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.personalInfoTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.birthdateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.ageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.registerTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.accountCreationDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.idTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleColorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehiclePlateNumberTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
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