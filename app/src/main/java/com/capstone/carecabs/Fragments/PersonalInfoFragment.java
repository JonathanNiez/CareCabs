package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;

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
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentPersonalInfoBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

public class PersonalInfoFragment extends Fragment {
	private DocumentReference documentReference;
	private final String TAG = "FragmentPersonalInfo";
	private String userID;
	private Intent intent;
	private AlertDialog signOutDialog, pleaseWaitDialog,
			noInternetDialog, registerNotCompleteDialog;
	private AlertDialog.Builder builder;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private FragmentTransaction fragmentTransaction;
	private FragmentManager fragmentManager;
	private FragmentPersonalInfoBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		binding = FragmentPersonalInfoBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.medConTextView.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);

		context = getContext();
		initializeNetworkChecker();
		loadUserProfileInfo();
		getCurrentFontSizeFromUserSetting();
		FirebaseApp.initializeApp(context);

		binding.imgBackBtn.setOnClickListener(v -> backToAccountFragment());

		return view;
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
	public void onPause() {
		super.onPause();

		closePleaseWaitDialog();
		closeNoInternetDialog();

	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void getCurrentFontSizeFromUserSetting() {

		switch (StaticDataPasser.storeFontSize) {
			case 15:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.birthdateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.ageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.accountCreationDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.signInTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

				break;

			case 17:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.birthdateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.ageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.accountCreationDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.signInTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

				break;

			case 19:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.birthdateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.ageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.accountCreationDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.signInTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);

				break;

			case 21:
				binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.userTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.birthdateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.ageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.medConTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.accountCreationDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.signInTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

				break;
		}
	}

	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {
			userID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection).document(userID);

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
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

					switch (getUserType) {

						case "Persons with Disabilities (PWD)":
							String getDisability = documentSnapshot.getString("disability");

							binding.disabilityTextView.setVisibility(View.VISIBLE);
							binding.disabilityTextView.setText("Disabilities: " + getDisability);


							break;

						case "Senior Citizen":
							String getMedicalCondition = documentSnapshot.getString("medicalCondition");

							binding.medConTextView.setVisibility(View.VISIBLE);
							binding.medConTextView.setText("Medical Conditions: " + getMedicalCondition);

							break;
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
					binding.signInTypeTextView.setText("Register Type: " + getRegisterType);

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

	private void backToAccountFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
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