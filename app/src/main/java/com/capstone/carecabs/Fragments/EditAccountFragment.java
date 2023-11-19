package com.capstone.carecabs.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Register.RegisterPWDActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.DialogEmailVerificationLinkSentBinding;
import com.capstone.carecabs.databinding.DialogEnterBirthdateBinding;
import com.capstone.carecabs.databinding.FragmentEditAccountBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditAccountFragment extends Fragment implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "EditAccountFragment";
	private FragmentEditAccountBinding binding;
	private String fontSize = StaticDataPasser.storeFontSize;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private final String[] sexItem = {"Male", "Female"};
	private final String[] disabilityItem = {
			"Communication Disability",
			"Vision Impairment",
			"Deaf or hard of hearing",
			"Mental health conditions",
			"Intellectual disability",
			"Acquired brain injury",
			"Autism spectrum disorder",
			"Physical disability"
	};

	private final String[] monthItem = {
			"January",
			"February",
			"March",
			"April",
			"May",
			"June",
			"July",
			"August",
			"September",
			"October",
			"November",
			"December"
	};

	private String birthDate, month, sex, disability;
	private int age;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private Intent intent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog cameraGalleryOptionsDialog, noInternetDialog,
			profilePicUpdateSuccessDialog, profilePicUpdateFailedDialog,
			profilePicUpdateSuccessConfirmation, pleaseWaitDialog,
			enterBirthdateDialog, yourVehiclePictureDialog, emailVerificationSentDialog;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private static final int PROFILE_PICTURE_REQUEST_CODE = 103;
	private static final int VEHICLE_PICTURE_REQUEST_CODE = 104;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private DocumentReference userReference;
	private StorageReference profilePicturePath;
	private StorageReference vehiclePicturePath;
	private RequestManager requestManager;
	private VoiceAssistant voiceAssistant;

	@Override
	public void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (requestManager != null) {
			requestManager.clear(binding.profilePicture);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		closePleaseWaitDialog();
		closeNoInternetDialog();
		closeProfilePicUpdateFailed();
		closeProfilePicUpdateSuccess();
		closeCameraOrGalleryOptionsDialog();
	}

	@Override
	public void onPause() {
		super.onPause();

		closePleaseWaitDialog();
		closeNoInternetDialog();
		closeProfilePicUpdateFailed();
		closeProfilePicUpdateSuccess();
		closeCameraOrGalleryOptionsDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentEditAccountBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.editDisabilityLayout.setVisibility(View.GONE);
		binding.idNotScannedTextView.setVisibility(View.GONE);
		binding.vehicleInfoLayout.setVisibility(View.GONE);
		binding.emailNotVerifiedLayout.setVisibility(View.GONE);

		context = getContext();

		requestManager = Glide.with(context);
		FirebaseApp.initializeApp(context);

		binding.profilePicture.setOnClickListener(v -> {
			ImagePicker.with(EditAccountFragment.this)
					.crop()
					.compress(1024)
					.maxResultSize(1080, 1080)
					.start(PROFILE_PICTURE_REQUEST_CODE);
		});

		binding.vehicleImageView.setOnClickListener(v -> {
			ImagePicker.with(EditAccountFragment.this)
					.crop()
					.compress(1024)
					.maxResultSize(1080, 1080)
					.start(VEHICLE_PICTURE_REQUEST_CODE);
		});

		binding.doneBtn.setOnClickListener(v -> backToAccountFragment());

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		binding.sendEmailVerificationLinkBtn.setOnClickListener(v ->
				sendEmailVerificationLink(FirebaseMain.getUser().getEmail()));

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (isAdded()) {
			checkPermission();
			getUserSettings();
			loadUserProfileInfo();
		}
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void getUserSettings() {
		setFontSize(fontSize);
		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Edit Profile");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initializeEditTexts() {

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);

			binding.editFirstnameEditText.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Firstname"));
			binding.editLastnameEditText.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Lastname"));
			binding.editAgeEditText.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Age"));
			binding.editBirthdateBtn.setOnClickListener(v -> voiceAssistant.speak("Birthdate"));
			binding.sexDropDownMenu.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Sex"));
			binding.disabilityDropDownMenu.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Disability"));
			binding.editVehicleColorEditText.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Vehicle Color"));
			binding.editVehiclePlateNumberEditText.setOnFocusChangeListener((v, hasFocus) -> voiceAssistant.speak("Vehicle Plate Number"));
		}

		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		Map<String, Object> updateInfo = new HashMap<>();

		binding.editFirstnameImgBtn.setOnClickListener(v -> {
			if (TextUtils.isEmpty(binding.editFirstnameEditText.getText().toString().trim())) {
				Toast.makeText(context, "Firstname cannot be empty", Toast.LENGTH_SHORT).show();
				return;
			} else {
				updateInfo.put("firstname", binding.editFirstnameEditText.getText().toString());

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Firstname updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Firstname failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		binding.editLastnameImgBtn.setOnClickListener(v -> {
			if (TextUtils.isEmpty(binding.editLastnameEditText.getText().toString().trim())) {
				Toast.makeText(context, "Lastname cannot be empty", Toast.LENGTH_SHORT).show();
				return;
			} else {
				updateInfo.put("lastname", binding.editLastnameEditText.getText().toString());

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Lastname updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Lastname failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		binding.editAgeImgBtn.setOnClickListener(v -> {
			if (TextUtils.isEmpty(binding.editAgeEditText.getText().toString())) {
				Toast.makeText(context, "Age cannot be empty", Toast.LENGTH_SHORT).show();
				return;
			} else {
				updateInfo.put("age", Integer.parseInt(binding.editAgeEditText.getText().toString()));

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Age updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Age failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		binding.editBirthdateBtn.setOnClickListener(v -> showEnterBirthdateDialog());

		binding.editBirthdateImgBtn.setOnClickListener(v -> {
			if (birthDate != null) {

				updateInfo.put("birthdate", birthDate);
				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Birthdate updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Birthdate failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});


		ArrayAdapter<String> sexAdapter =
				new ArrayAdapter<>(context, R.layout.item_dropdown, sexItem);
		binding.sexDropDownMenu.setAdapter(sexAdapter);

		binding.sexDropDownMenu.setOnItemClickListener((parent, view, position, id) ->
				sex = parent.getItemAtPosition(position).toString());

		binding.editSexImgBtn.setOnClickListener(v -> {
			if (sex != null) {
				updateInfo.put("sex", sex);

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Sex updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Sex failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		ArrayAdapter<String> disabilityAdapter =
				new ArrayAdapter<>(context, R.layout.item_dropdown, disabilityItem);
		binding.disabilityDropDownMenu.setAdapter(disabilityAdapter);

		binding.disabilityDropDownMenu.setOnItemClickListener((parent, view, position, id) ->
				disability = parent.getItemAtPosition(position).toString());

		binding.editDisabilityImgBtn.setOnClickListener(v -> {
			if (disability != null) {

				updateInfo.put("disability", disability);

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Disability updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Disability failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		binding.vehicleColorImgBtn.setOnClickListener(v -> {
			if (TextUtils.isEmpty(binding.editVehicleColorEditText.getText().toString().trim())) {
				Toast.makeText(context, "Vehicle color cannot be empty", Toast.LENGTH_SHORT).show();
				return;
			} else {
				updateInfo.put("vehicleColor", binding.editVehicleColorEditText.getText().toString());

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Vehicle color updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Vehicle color failed to updated", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

		binding.vehicleColorImgBtn.setOnClickListener(v -> {
			if (TextUtils.isEmpty(binding.editVehiclePlateNumberEditText.getText().toString().trim())) {
				Toast.makeText(context, "Vehicle plate number cannot be empty", Toast.LENGTH_SHORT).show();
				return;
			} else {
				updateInfo.put("vehiclePlateNumber", binding.editVehiclePlateNumberEditText.getText().toString());

				userReference.update(updateInfo)
						.addOnSuccessListener(unused -> {
							loadUserProfileInfo();
							showToast("Vehicle plate number updated", 0);
						})
						.addOnFailureListener(e -> {
							showToast("Vehicle plate number failed to update", 1);
							Log.e(TAG, "initializeEditTexts: " + e.getMessage());
						});
			}
		});

	}

	private void showToast(String message, int duration) {
		Toast.makeText(context, message, duration).show();
	}

	private void showDatePickerDialog() {
		final Calendar currentDate = Calendar.getInstance();
		int year = currentDate.get(Calendar.YEAR);
		int month = currentDate.get(Calendar.MONTH);
		int day = currentDate.get(Calendar.DAY_OF_MONTH);

		DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
				(view, year1, monthOfYear, dayOfMonth) -> {
					selectedDate = Calendar.getInstance();
					selectedDate.set(year1, monthOfYear, dayOfMonth);

					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
					StaticDataPasser.storeCurrentBirthDate = String.valueOf(selectedDate.getTime());

				}, year, month, day);
		datePickerDialog.show();
	}

	private void sendEmailVerificationLink(String email) {
		if (FirebaseMain.getUser() != null) {
			if (!FirebaseMain.getUser().isEmailVerified()) {

				FirebaseMain.getUser().sendEmailVerification()
						.addOnSuccessListener(unused -> {

							showEmailVerificationSentDialog(email);
						})
						.addOnFailureListener(e -> {
							showToast("Failed to send an Email Verification Link", 1);
							Log.e(TAG, "sendEmailVerification - onFailure: " + e.getMessage());
						});

			}
		} else {
			showToast("Failed to send an Email Verification Link", 1);
			Log.e(TAG, "sendEmailVerification: current user is null");
		}
	}
	@SuppressLint("SetTextI18n")
	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			userReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

			userReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							closePleaseWaitDialog();

							initializeEditTexts();

							String getProfilePicture = documentSnapshot.getString("profilePicture");
							String getUserType = documentSnapshot.getString("userType");
							String getFirstName = documentSnapshot.getString("firstname");
							String getLastName = documentSnapshot.getString("lastname");
							Long getAgeLong = documentSnapshot.getLong("age");
							int getAge = getAgeLong.intValue();
							String getSex = documentSnapshot.getString("sex");
							boolean isVerified = documentSnapshot.getBoolean("isVerified");
							String getBirthdate = documentSnapshot.getString("birthdate");

							if (!isVerified) {
								binding.idNotScannedTextView.setVisibility(View.VISIBLE);

							}

							binding.emailTextView.setText(FirebaseMain.getUser().getEmail());
							binding.editFirstnameEditText.setText(getFirstName);
							binding.editLastnameEditText.setText(getLastName);
							binding.editBirthdateBtn.setText("Birthdate: " + getBirthdate);
							binding.editAgeEditText.setText(String.valueOf(getAge));

							if (!FirebaseMain.getUser().isEmailVerified()) {
								binding.emailNotVerifiedLayout.setVisibility(View.VISIBLE);
							}

							if (getUserType != null) {
								switch (getUserType) {
									case "Driver":
										binding.vehicleInfoLayout.setVisibility(View.VISIBLE);

										String getVehiclePicture = documentSnapshot.getString("vehiclePicture");
										String getVehicleColor = documentSnapshot.getString("vehicleColor");
										String getVehiclePlateNumber = documentSnapshot.getString("vehiclePlateNumber");

										if (getVehiclePicture != null && !getVehiclePicture.equals("none")) {
											Glide.with(context)
													.load(getVehiclePicture)
													.placeholder(R.drawable.loading_gif)
													.into(binding.vehicleImageView);
										}

										binding.editVehicleColorEditText.setText(getVehicleColor);
										binding.editVehiclePlateNumberEditText.setText(getVehiclePlateNumber);

										break;

									case "Person with Disabilities (PWD)":
										binding.editDisabilityLayout.setVisibility(View.VISIBLE);
										String getDisability = documentSnapshot.getString("disability");

										break;
								}
							}

							if (getSex != null) {
								switch (getSex) {
									case "Male":
										break;

									case "Female":
										break;
								}
							}

							if (getProfilePicture != null && !getProfilePicture.equals("default")) {
								Glide.with(context)
										.load(getProfilePicture)
										.centerCrop()
										.placeholder(R.drawable.loading_gif)
										.into(binding.profilePicture);
							}

						} else {
							closePleaseWaitDialog();
						}
					})
					.addOnFailureListener(e -> {
						closePleaseWaitDialog();

						Log.e(TAG, "loadUserProfileInfo: " + e.getMessage());
					});

		} else {
			intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}

	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		String fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

		float textSizeSP;
		float textHeaderSizeSP;
		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.editProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.tapImageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.idNotScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editFirstnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editLastnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editAgeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editBirthdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleInfoTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editVehicleColorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editVehiclePlateNumberEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.clickTheBtnTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.yourEmailNotVerifiedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}
	private void showEmailVerificationSentDialog(String email) {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		DialogEmailVerificationLinkSentBinding sentBinding =
				DialogEmailVerificationLinkSentBinding.inflate(getLayoutInflater());
		View dialogView = sentBinding.getRoot();

		sentBinding.emailTextView.setText(email);

		if (fontSize.equals("large")) {
			float textSize = 22;
			sentBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			sentBinding.bodyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			sentBinding.bodyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			sentBinding.emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}

		sentBinding.okayBtn.setOnClickListener(v -> closeEmailVerificationSentDialog());

		builder.setView(dialogView);
		emailVerificationSentDialog = builder.create();
		emailVerificationSentDialog.show();
	}

	private void closeEmailVerificationSentDialog() {
		if (emailVerificationSentDialog != null && emailVerificationSentDialog.isShowing()) {
			emailVerificationSentDialog.dismiss();
		}
	}
	private void showEnterBirthdateDialog() {
		builder = new AlertDialog.Builder(context);

		DialogEnterBirthdateBinding dialogEnterBirthdateBinding =
				DialogEnterBirthdateBinding.inflate(getLayoutInflater());
		View dialogView = dialogEnterBirthdateBinding.getRoot();

		ArrayAdapter<String> monthAdapter =
				new ArrayAdapter<>(context, R.layout.item_dropdown, monthItem);
		dialogEnterBirthdateBinding.monthDropDownMenu.setAdapter(monthAdapter);

		dialogEnterBirthdateBinding.monthDropDownMenu.setOnItemClickListener((parent, view, position, id)
				-> {
			month = parent.getItemAtPosition(position).toString();
			dialogEnterBirthdateBinding.monthTextView.setText(month);
		});

		dialogEnterBirthdateBinding.doneBtn.setOnClickListener(view -> {
			String year = dialogEnterBirthdateBinding.yearEditText.getText().toString();
			String day = dialogEnterBirthdateBinding.dayEditText.getText().toString();

			if (month == null ||
					year.isEmpty() ||
					day.isEmpty()) {

				Toast.makeText(context, "Please complete your Birthdate", Toast.LENGTH_SHORT).show();

			} else {
				birthDate = month + "-" + day + "-" + year;

				//Calculate age
				Calendar today = Calendar.getInstance();
				age = today.get(Calendar.YEAR) - Integer.parseInt(year);

				// Check if the user's birthday has already happened this year or not
				if (today.get(Calendar.DAY_OF_YEAR) < Integer.parseInt(year)) {
					age--;
				}

				binding.editBirthdateBtn.setText(birthDate);
				binding.editAgeEditText.setText(String.valueOf(age));

				closeEnterBirthdateDialog();
			}
		});

		dialogEnterBirthdateBinding.cancelBtn.setOnClickListener(v -> closeEnterBirthdateDialog());

		builder.setView(dialogView);
		enterBirthdateDialog = builder.create();
		enterBirthdateDialog.show();
	}

	private void closeEnterBirthdateDialog() {
		if (enterBirthdateDialog != null && enterBirthdateDialog.isShowing()) {
			enterBirthdateDialog.dismiss();
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

	private void showProfilePicUpdateSuccess() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_pic_update_success, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> closeProfilePicUpdateSuccess());

		builder.setView(dialogView);

		profilePicUpdateSuccessDialog = builder.create();
		profilePicUpdateSuccessDialog.show();
	}

	private void closeProfilePicUpdateSuccess() {
		if (profilePicUpdateSuccessDialog != null && profilePicUpdateSuccessDialog.isShowing()) {
			profilePicUpdateSuccessDialog.dismiss();

		}
	}

	private void showProfilePicUpdateFailed() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_pic_update_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			closeProfilePicUpdateFailed();
		});


		builder.setView(dialogView);

		profilePicUpdateFailedDialog = builder.create();
		profilePicUpdateFailedDialog.show();
	}

	private void closeProfilePicUpdateFailed() {
		if (profilePicUpdateFailedDialog != null && profilePicUpdateFailedDialog.isShowing()) {
			profilePicUpdateFailedDialog.dismiss();

		}
	}

	private void showProfilePicUpdateConfirmation() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_pic_change_confirm, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		noBtn.setOnClickListener(v -> {
			closeProfilePicUpdateConfirmation();
		});

		yesBtn.setOnClickListener(v -> {

		});


		builder.setView(dialogView);

		profilePicUpdateSuccessConfirmation = builder.create();
		profilePicUpdateSuccessConfirmation.show();
	}

	private void closeProfilePicUpdateConfirmation() {
		if (profilePicUpdateSuccessConfirmation != null && profilePicUpdateSuccessConfirmation.isShowing()) {
			profilePicUpdateSuccessConfirmation.dismiss();

		}
	}

	private void backToAccountFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void checkPermission() {
		// Check for camera permission
		if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(getActivity(),
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST);
		}

		// Check for storage permission
		if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(getActivity(),
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST);
		}
	}

	private void openGallery() {
		intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
			startActivityForResult(intent, GALLERY_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(getContext(), "Opened Gallery", Toast.LENGTH_LONG).show();

		} else {
			Toast.makeText(getContext(), "No gallery app found", Toast.LENGTH_LONG).show();
		}
	}

	private void openCamera() {
		intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
			startActivityForResult(intent, CAMERA_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(getContext(), "Opened Camera", Toast.LENGTH_LONG).show();


		} else {
			Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_LONG).show();
		}
	}

	private Uri getImageUri(Context context, Bitmap bitmap) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
		return Uri.parse(path);
	}

	private void uploadProfileImageToFirebaseStorage(String userID, Uri profilePictureUri) {

		StorageReference profilePictureReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		profilePicturePath = profilePictureReference.child("images/profilePictures/"
				+ System.currentTimeMillis() + "_" + userID + ".jpg");

		profilePicturePath.putFile(profilePictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					profilePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {
								closePleaseWaitDialog();
								showProfilePicUpdateSuccess();
								binding.profilePicture.setImageURI(profilePictureUri);
								String profilePictureURL = uri.toString();
								storeProfileImageURLInFireStore(userID, profilePictureURL);
							})
							.addOnFailureListener(e -> {
								closePleaseWaitDialog();
								showToast("Profile picture updated successfully", 0);
								Log.e(TAG, "uploadProfileImageToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					closePleaseWaitDialog();
					showProfilePicUpdateFailed();
					showToast("Profile picture failed to add", 0);
					Log.e(TAG, "uploadProfileImageToFirebaseStorage: " + e.getMessage());
				});
	}

	private void uploadVehicleImageToFirebaseStorage(String userID, Uri vehiclePictureUri) {

		StorageReference vehiclePictureReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		vehiclePicturePath = vehiclePictureReference.child("images/vehiclePictures/"
				+ System.currentTimeMillis() + "_" + userID + ".jpg");

		vehiclePicturePath.putFile(vehiclePictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					vehiclePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {
								closePleaseWaitDialog();
								binding.vehicleImageView.setImageURI(vehiclePictureUri);
								String vehicleImageUrl = uri.toString();
								storeVehicleImageURLInFireStore(vehicleImageUrl, userID);
							})
							.addOnFailureListener(e -> {
								closePleaseWaitDialog();
								showToast("Vehicle picture updated successfully", 0);
								Log.e(TAG, "uploadVehicleImageToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					closePleaseWaitDialog();
					showToast("Vehicle picture failed to update", 1);
					Log.e(TAG, "uploadVehicleImageToFirebaseStorage: " + e.getMessage());
				});
	}

	private void storeProfileImageURLInFireStore(String userID, String profilePictureURL) {

		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", profilePictureURL);

		userReference.update(profilePicture)
				.addOnSuccessListener(unused ->
						Toast.makeText(context, "Profile picture added successfully", Toast.LENGTH_SHORT).show())
				.addOnFailureListener(e -> {
					Toast.makeText(context, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "storeProfileImageURLInFireStore: addOnFailureListener " + e.getMessage());
				});
	}

	private void storeVehicleImageURLInFireStore(String imageUrl, String userID) {

		userReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> vehiclePicture = new HashMap<>();
		vehiclePicture.put("vehiclePicture", imageUrl);

		userReference.update(vehiclePicture)
				.addOnSuccessListener(unused ->
						Toast.makeText(context, "Profile picture added successfully", Toast.LENGTH_SHORT).show())
				.addOnFailureListener(e -> {
					Toast.makeText(context, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, e.getMessage());
				});
	}

	private void showCameraOrGalleryOptionsDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_camera_gallery, null);

		Button openCameraBtn = dialogView.findViewById(R.id.openCameraBtn);
		Button openGalleryBtn = dialogView.findViewById(R.id.openGalleryBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		openCameraBtn.setOnClickListener(v -> {
			openCamera();
		});

		openGalleryBtn.setOnClickListener(v -> {
			openGallery();
		});

		cancelBtn.setOnClickListener(v -> {
			closeCameraOrGalleryOptionsDialog();
		});

		builder.setView(dialogView);

		cameraGalleryOptionsDialog = builder.create();
		cameraGalleryOptionsDialog.show();
	}

	private void closeCameraOrGalleryOptionsDialog() {
		if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
			cameraGalleryOptionsDialog.dismiss();
		}
	}

	private void showUploadYourVehiclePictureDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_please_upload_the_picture_of_your_vehicle, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			closeUploadYourVehiclePictureDialog();
		});

		builder.setView(dialogView);

		yourVehiclePictureDialog = builder.create();
		yourVehiclePictureDialog.show();
	}

	private void closeUploadYourVehiclePictureDialog() {
		if (yourVehiclePictureDialog != null && yourVehiclePictureDialog.isShowing()) {
			yourVehiclePictureDialog.dismiss();
		}
	}

	private void showNoInternetDialog() {

		builder = new AlertDialog.Builder(getContext());
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && data != null) {

			showPleaseWaitDialog();

			if (requestCode == PROFILE_PICTURE_REQUEST_CODE) {

				Uri profilePictureUri = data.getData();
				uploadProfileImageToFirebaseStorage(FirebaseMain.getUser().getUid(), profilePictureUri);

			} else if (requestCode == VEHICLE_PICTURE_REQUEST_CODE) {

				Uri vehiclePictureUri = data.getData();
				uploadVehicleImageToFirebaseStorage(FirebaseMain.getUser().getUid(), vehiclePictureUri);

			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CAMERA_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG, "Camera Permission Granted");
			}
		} else if (requestCode == STORAGE_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG, "Gallery Permission Granted");
			}
		} else {
			Log.e(TAG, "Permission Denied");
		}
	}
}