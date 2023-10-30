package com.capstone.carecabs.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentEditAccountBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditAccountFragment extends Fragment implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "EditAccountFragment";
	private float textSizeSP;
	private float textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private Intent intent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog editFirstNameDialog, editLastNameDialog,
			editDisabilityDialog, editSexDialog, editAgeDialog,
			cameraGalleryOptionsDialog, noInternetDialog,
			profilePicUpdateSuccessDialog, profilePicUpdateFailedDialog,
			profilePicUpdateSuccessConfirmation, pleaseWaitDialog,
			birthdateInputChoiceDialog, yourVehiclePictureDialog;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private static final int PROFILE_PICTURE_REQUEST_CODE = 103;
	private static final int VEHICLE_PICTURE_REQUEST_CODE = 104;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private DocumentReference documentReference;
	private StorageReference storageReference;
	private StorageReference profilePicturePath;
	private StorageReference vehiclePicturePath;
	private RequestManager requestManager;
	private VoiceAssistant voiceAssistant;
	private FragmentEditAccountBinding binding;

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

		closeEditFirstNameDialog();
		closeEditLastNameDialog();
		closeEditAgeDialog();
		closeEditSexDialog();
		closeEditDisabilityDialog();
		closeNoInternetDialog();
		closeProfilePicUpdateFailed();
		closeProfilePicUpdateSuccess();
		closeCameraOrGalleryOptionsDialog();
	}

	@Override
	public void onPause() {
		super.onPause();

		closeEditFirstNameDialog();
		closeEditLastNameDialog();
		closeEditAgeDialog();
		closeEditSexDialog();
		closeEditDisabilityDialog();
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
		binding.editMedicalConditionLayout.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);
		binding.medicalConditionTextView.setVisibility(View.GONE);
		binding.idNotScannedTextView.setVisibility(View.GONE);
		binding.vehicleInfoLayout.setVisibility(View.GONE);

		context = getContext();
		if (context != null) {
			requestManager = Glide.with(context);
			FirebaseApp.initializeApp(context);

			checkPermission();
			getUserSettings();
			loadUserProfileInfo();
		}

		binding.profilePicture.setOnClickListener(v -> {
			ImagePicker.with(EditAccountFragment.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start(PROFILE_PICTURE_REQUEST_CODE);
		});

		binding.vehicleImageView.setOnClickListener(v -> {
			ImagePicker.with(EditAccountFragment.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start(VEHICLE_PICTURE_REQUEST_CODE);
		});

		binding.doneBtn.setOnClickListener(v -> {
			backToAccountFragment();
		});

		binding.backFloatingBtn.setOnClickListener(v -> {
			backToAccountFragment();
		});

		return view;
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void getUserSettings() {
		String fontSize = StaticDataPasser.storeFontSize;
		String voiceAssistantToggle = StaticDataPasser.storeVoiceAssistantState;

		setFontSize(fontSize);

		if (voiceAssistantToggle.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Edit Profile");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initializeEditTexts() {

		binding.editFirstnameImgBtn.setVisibility(View.GONE);
		binding.editLastnameImgBtn.setVisibility(View.GONE);
		binding.editAgeImgBtn.setVisibility(View.GONE);
		binding.editSexImgBtn.setVisibility(View.GONE);
		binding.editDisabilityImgBtn.setVisibility(View.GONE);
		binding.editMedicalConditionImgBtn.setVisibility(View.GONE);
		binding.vehicleColorImgBtn.setVisibility(View.GONE);
		binding.vehiclePlateNumberImgBtn.setVisibility(View.GONE);

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		Map<String, Object> updateInfo = new HashMap<>();

		binding.editFirstnameSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editFirstnameEditText.setEnabled(true);
				binding.editFirstnameEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editFirstnameImgBtn.setVisibility(View.VISIBLE);
				binding.editFirstnameImgBtn.setOnClickListener(v -> {
					if (TextUtils.isEmpty(binding.editFirstnameEditText.getText().toString().trim())) {
						Toast.makeText(context, "Firstname cannot be empty", Toast.LENGTH_SHORT).show();
						return;
					} else {
						updateInfo.put("firstname", binding.editFirstnameEditText.getText().toString());

						documentReference.update(updateInfo)
								.addOnSuccessListener(unused -> {
									loadUserProfileInfo();

									Toast.makeText(context, "Firstname updated", Toast.LENGTH_LONG).show();
									binding.editFirstnameEditText.setEnabled(false);
									binding.editFirstnameSwitch.setChecked(false);
								})
								.addOnFailureListener(e -> {
									Toast.makeText(context, "Firstname failed to update", Toast.LENGTH_LONG).show();

									Log.e(TAG, e.getMessage());
								});
					}
				});
			} else {
				binding.editFirstnameEditText.setEnabled(false);
				binding.editFirstnameEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editFirstnameImgBtn.setVisibility(View.GONE);
			}
		});

		binding.editLastnameSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editLastnameEditText.setEnabled(true);
				binding.editLastnameEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editLastnameImgBtn.setVisibility(View.VISIBLE);
				binding.editLastnameImgBtn.setOnClickListener(v -> {
					if (TextUtils.isEmpty(binding.editLastnameEditText.getText().toString().trim())) {
						Toast.makeText(context, "Lastname cannot be empty", Toast.LENGTH_SHORT).show();
						return;
					} else {
						updateInfo.put("lastname", binding.editLastnameEditText.getText().toString());

						documentReference.update(updateInfo)
								.addOnSuccessListener(unused -> {
									loadUserProfileInfo();

									Toast.makeText(context, "Lastname updated", Toast.LENGTH_LONG).show();
									binding.editLastnameEditText.setEnabled(false);
									binding.editLastnameSwitch.setChecked(false);
								})
								.addOnFailureListener(e -> {
									Toast.makeText(context, "Lastname failed to update", Toast.LENGTH_LONG).show();

									Log.e(TAG, e.getMessage());
								});
					}
				});
			} else {
				binding.editLastnameEditText.setEnabled(false);
				binding.editLastnameEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editLastnameImgBtn.setVisibility(View.GONE);
			}
		});

		binding.editAgeSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editAgeEditText.setEnabled(true);
				binding.editAgeEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editAgeImgBtn.setVisibility(View.VISIBLE);
				binding.editAgeImgBtn.setOnClickListener(v -> {
					if (TextUtils.isEmpty(binding.editAgeEditText.getText().toString())) {
						Toast.makeText(context, "Age cannot be empty", Toast.LENGTH_SHORT).show();
						return;
					} else {
						updateInfo.put("age", Integer.parseInt(binding.editAgeEditText.getText().toString()));

						documentReference.update(updateInfo)
								.addOnSuccessListener(unused -> {
									loadUserProfileInfo();

									Toast.makeText(context, "Age updated", Toast.LENGTH_LONG).show();
									binding.editAgeEditText.setEnabled(false);
									binding.editAgeSwitch.setChecked(false);
								})
								.addOnFailureListener(e -> {
									Toast.makeText(context, "Age failed to update", Toast.LENGTH_LONG).show();

									Log.e(TAG, e.getMessage());
								});
					}
				});
			} else {
				binding.editAgeEditText.setEnabled(false);
				binding.editAgeEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editAgeImgBtn.setVisibility(View.GONE);
			}
		});

		binding.editBirthdateSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editBirthdateBtn.setEnabled(true);
				binding.editBirthdateBtn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editBirthdateBtn.setOnClickListener(v -> {

					showEnterBirthdateDialog();
				});
			} else {
				binding.editBirthdateBtn.setEnabled(false);
				binding.editBirthdateBtn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
			}
		});

		binding.editSexSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editSexSpinner.setEnabled(true);
				binding.editSexSpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editSexImgBtn.setVisibility(View.VISIBLE);

				ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
						context,
						R.array.sex_options,
						android.R.layout.simple_spinner_item
				);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				binding.editSexSpinner.setAdapter(adapter);
				binding.editSexSpinner.setSelection(0);
				binding.editSexSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						if (position == 0) {
							binding.editSexSpinner.setSelection(0);
						} else {
							binding.editSexImgBtn.setOnClickListener(v -> {
								updateInfo.put("sex", parent.getItemAtPosition(position).toString());
								documentReference.update(updateInfo)
										.addOnSuccessListener(unused -> {
											loadUserProfileInfo();

											Toast.makeText(context, "Sex updated", Toast.LENGTH_LONG).show();
											binding.editSexSpinner.setEnabled(false);
											binding.editSexSwitch.setChecked(false);
										})
										.addOnFailureListener(e -> {
											Toast.makeText(context, "Sex failed to update", Toast.LENGTH_LONG).show();

											Log.e(TAG, e.getMessage());
										});
							});
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						binding.editSexSpinner.setSelection(0);
					}
				});
			} else {
				binding.editSexSpinner.setEnabled(false);
				binding.editSexSpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editSexImgBtn.setVisibility(View.GONE);
			}
		});

		binding.editDisabilitySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editDisabilitySpinner.setEnabled(true);
				binding.editDisabilitySpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editDisabilityImgBtn.setVisibility(View.VISIBLE);

				ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
						context,
						R.array.sex_options,
						android.R.layout.simple_spinner_item
				);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				binding.editDisabilitySpinner.setAdapter(adapter);
				binding.editDisabilitySpinner.setSelection(0);
				binding.editDisabilitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						if (position == 0) {
							binding.editDisabilitySpinner.setSelection(0);
						} else {
							binding.editDisabilityImgBtn.setOnClickListener(v -> {
								updateInfo.put("disability", parent.getItemAtPosition(position).toString());
								documentReference.update(updateInfo)
										.addOnSuccessListener(unused -> {
											loadUserProfileInfo();

											Toast.makeText(context, "Disability updated", Toast.LENGTH_LONG).show();
											binding.editDisabilitySpinner.setEnabled(false);
											binding.editDisabilitySwitch.setChecked(false);
										})
										.addOnFailureListener(e -> {
											Toast.makeText(context, "Disability failed to update", Toast.LENGTH_LONG).show();

											Log.e(TAG, e.getMessage());
										});
							});
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						binding.editDisabilitySpinner.setSelection(0);
					}
				});
			} else {
				binding.editDisabilitySpinner.setEnabled(false);
				binding.editDisabilitySpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editDisabilityImgBtn.setVisibility(View.GONE);
			}
		});

		binding.editMedicalConditionSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.editMedicalConditionSpinner.setEnabled(true);
				binding.editMedicalConditionSpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.editMedicalConditionImgBtn.setVisibility(View.VISIBLE);

				ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
						context,
						R.array.senior_citizen_medical_condition,
						android.R.layout.simple_spinner_item
				);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				binding.editMedicalConditionSpinner.setAdapter(adapter);
				binding.editMedicalConditionSpinner.setSelection(0);
				binding.editMedicalConditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						if (position == 0) {
							binding.editMedicalConditionSpinner.setSelection(0);
						} else {
							binding.editMedicalConditionImgBtn.setOnClickListener(v -> {
								updateInfo.put("medicalCondition", parent.getItemAtPosition(position).toString());

								documentReference.update(updateInfo)
										.addOnSuccessListener(unused -> {
											loadUserProfileInfo();

											Toast.makeText(context, "Medical Condition updated", Toast.LENGTH_LONG).show();
											binding.editMedicalConditionSpinner.setEnabled(false);
											binding.editMedicalConditionSwitch.setChecked(false);
										})
										.addOnFailureListener(e -> {
											Toast.makeText(context, "Medical Condition failed to update", Toast.LENGTH_LONG).show();

											Log.e(TAG, e.getMessage());
										});
							});
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						binding.editMedicalConditionSpinner.setSelection(0);
					}
				});
			} else {
				binding.editMedicalConditionSpinner.setEnabled(false);
				binding.editMedicalConditionSpinner.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.editMedicalConditionImgBtn.setVisibility(View.GONE);
			}
		});

		binding.vehicleColorSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.vehicleColorEditText.setEnabled(true);
				binding.vehicleColorEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.vehicleColorImgBtn.setVisibility(View.VISIBLE);
				binding.vehicleColorImgBtn.setOnClickListener(v -> {
					if (TextUtils.isEmpty(binding.vehicleColorEditText.getText().toString().trim())) {
						Toast.makeText(context, "Vehicle color cannot be empty", Toast.LENGTH_SHORT).show();
						return;
					} else {
						updateInfo.put("vehicleColor", binding.vehicleColorEditText.getText().toString());

						documentReference.update(updateInfo)
								.addOnSuccessListener(unused -> {
									loadUserProfileInfo();

									Toast.makeText(context, "Vehicle color updated", Toast.LENGTH_LONG).show();
									binding.vehicleColorEditText.setEnabled(false);
									binding.vehicleColorSwitch.setChecked(false);
								})
								.addOnFailureListener(e -> {
									Toast.makeText(context, "Vehicle color failed to update", Toast.LENGTH_LONG).show();

									Log.e(TAG, e.getMessage());
								});
					}
				});
			} else {
				binding.vehicleColorEditText.setEnabled(false);
				binding.vehicleColorEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.vehicleColorImgBtn.setVisibility(View.GONE);
			}
		});

		binding.vehiclePlateNumberSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				binding.vehiclePlateNumberEditText.setEnabled(true);
				binding.vehiclePlateNumberEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
				binding.vehiclePlateNumberImgBtn.setVisibility(View.VISIBLE);
				binding.vehicleColorImgBtn.setOnClickListener(v -> {
					if (TextUtils.isEmpty(binding.vehiclePlateNumberEditText.getText().toString().trim())) {
						Toast.makeText(context, "Vehicle plate number cannot be empty", Toast.LENGTH_SHORT).show();
						return;
					} else {
						updateInfo.put("vehiclePlateNumber", binding.vehiclePlateNumberEditText.getText().toString());

						documentReference.update(updateInfo)
								.addOnSuccessListener(unused -> {
									loadUserProfileInfo();

									Toast.makeText(context, "Vehicle plate number updated", Toast.LENGTH_LONG).show();
									binding.vehiclePlateNumberEditText.setEnabled(false);
									binding.vehiclePlateNumberSwitch.setChecked(false);
								})
								.addOnFailureListener(e -> {
									Toast.makeText(context, "Vehicle plate number failed to update", Toast.LENGTH_LONG).show();

									Log.e(TAG, "initializeEditTexts: " + e.getMessage());
								});
					}
				});
			} else {
				binding.vehiclePlateNumberEditText.setEnabled(false);
				binding.vehiclePlateNumberEditText.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
				binding.vehiclePlateNumberImgBtn.setVisibility(View.GONE);
			}
		});

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

							initializeEditTexts();

							String getProfilePicture = documentSnapshot.getString("profilePicture");
							String getUserType = documentSnapshot.getString("userType");
							String getFirstName = documentSnapshot.getString("firstname");
							String getLastName = documentSnapshot.getString("lastname");
							Long getAgeLong = documentSnapshot.getLong("age");
							int getAge = getAgeLong.intValue();
							String getEmail = documentSnapshot.getString("email");
							String getPhoneNumber = documentSnapshot.getString("phoneNumber");
							String getSex = documentSnapshot.getString("sex");
							boolean getVerificationStatus = documentSnapshot.getBoolean("isVerified");
							String getBirthdate = documentSnapshot.getString("birthdate");

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

										binding.vehicleColorEditText.setText("Vehicle Color: " + getVehicleColor);
										binding.vehiclePlateNumberEditText.setText("Vehicle Plate Number: " + getVehiclePlateNumber);

										break;

									case "Person with Disabilities (PWD)":
										String getDisability = documentSnapshot.getString("disability");
										binding.editDisabilityLayout.setVisibility(View.VISIBLE);
										binding.disabilityTextView.setText("Disability: " + getDisability);

										break;

									case "Senior Citizen":
										String getMedicalCondition = documentSnapshot.getString("medicalCondition");

										binding.editMedicalConditionLayout.setVisibility(View.VISIBLE);
										binding.medicalConditionTextView.setVisibility(View.VISIBLE);
										binding.medicalConditionTextView.setText(getMedicalCondition);

										break;
								}
							}

							if (getSex != null) {
								switch (getSex) {
									case "Male":
										binding.editSexSpinner.setSelection(1);
										break;

									case "Female":
										binding.editSexSpinner.setSelection(2);
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

							if (!getVerificationStatus) {
								binding.idNotScannedTextView.setVisibility(View.VISIBLE);

							}

							StaticDataPasser.storeFirstName = getFirstName;
							StaticDataPasser.storeLastName = getLastName;
							StaticDataPasser.storeCurrentAge = getAge;
							StaticDataPasser.storeCurrentBirthDate = getBirthdate;
							StaticDataPasser.storeSelectedSex = getSex;

							binding.firstnameTextView.setText(getFirstName);
							binding.lastnameTextView.setText(getLastName);
							binding.editFirstnameEditText.setText(getFirstName);
							binding.editLastnameEditText.setText(getLastName);
							binding.editBirthdateBtn.setText("Birthdate: " + getBirthdate);
							binding.editAgeEditText.setText(String.valueOf(getAge));
							binding.sexTextView.setText("Sex: " + getSex);

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

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.editProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.tapImageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.idNotScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.guideTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

		binding.editFirstnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editLastnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editAgeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editBirthdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleInfoTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleColorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehiclePlateNumberEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
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

		binding.editProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.lastnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.tapImageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.idNotScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.guideTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

		binding.editFirstnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editLastnameEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editAgeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.editBirthdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.sexTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleInfoTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleColorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehiclePlateNumberEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void showEnterBirthdateDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_enter_birthdate, null);

		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		Button doneBtn = dialogView.findViewById(R.id.doneBtn);
		TextView monthTextView = dialogView.findViewById(R.id.monthTextView);
		TextView dayTextView = dialogView.findViewById(R.id.dayTextView);
		TextView yearTextView = dialogView.findViewById(R.id.yearTextView);
		EditText yearEditText = dialogView.findViewById(R.id.yearEditText);
		EditText dayEditText = dialogView.findViewById(R.id.dayEditText);
		Spinner spinnerMonth = dialogView.findViewById(R.id.spinnerMonth);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				context,
				R.array.month,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerMonth.setAdapter(adapter);
		spinnerMonth.setSelection(0);
		spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					spinnerMonth.setSelection(0);
				} else {
					String selectedMonth = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedMonth = selectedMonth;

					monthTextView.setText(selectedMonth);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				spinnerMonth.setSelection(0);
			}
		});

		dayEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				dayTextView.setText(enteredText);
			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		yearEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				String enteredText = charSequence.toString();
				yearTextView.setText(enteredText);

			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});

		doneBtn.setOnClickListener(view -> {
			String year = yearEditText.getText().toString();
			String day = dayEditText.getText().toString();
			if (StaticDataPasser.storeSelectedMonth.equals("Month")
					|| year.isEmpty() || day.isEmpty()) {

				Toast.makeText(context, "Please enter your Date of Birth", Toast.LENGTH_SHORT).show();
			} else {
				String fullBirthdate = StaticDataPasser.storeSelectedMonth + "-" + day + "-" + year;

				//Calculate age
				Calendar today = Calendar.getInstance();
				int age = today.get(Calendar.YEAR) - Integer.parseInt(year);

				// Check if the user's birthday has already happened this year or not
				if (today.get(Calendar.DAY_OF_YEAR) < Integer.parseInt(year)) {
					age--;
				}

				StaticDataPasser.storeBirthdate = fullBirthdate;
				StaticDataPasser.storeCurrentAge = age;

				binding.editBirthdateBtn.setText(fullBirthdate);
				binding.editAgeEditText.setText(String.valueOf(age));

				closeEnterBirthdateDialog();
			}
		});

		cancelBtn.setOnClickListener(v -> {
			closeEnterBirthdateDialog();
		});

		builder.setView(dialogView);

		birthdateInputChoiceDialog = builder.create();
		birthdateInputChoiceDialog.show();
	}

	private void closeEnterBirthdateDialog() {
		if (birthdateInputChoiceDialog != null && birthdateInputChoiceDialog.isShowing()) {
			birthdateInputChoiceDialog.dismiss();
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

		okBtn.setOnClickListener(v -> {
			closeProfilePicUpdateSuccess();
		});


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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_pic_update_success, null);

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

	private void showEditFirstNameDialog() {

		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.edit_firstname_dialog, null);

		Button editBtn = dialogView.findViewById(R.id.editBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		EditText editFirstName = dialogView.findViewById(R.id.editFirstname);

		editFirstName.setText(StaticDataPasser.storeFirstName);

		editBtn.setOnClickListener(v -> {

		});

		cancelBtn.setOnClickListener(v -> {
			closeEditFirstNameDialog();
		});

		builder.setView(dialogView);

		editFirstNameDialog = builder.create();
		editFirstNameDialog.show();
	}

	private void closeEditFirstNameDialog() {
		if (editFirstNameDialog != null && editFirstNameDialog.isShowing()) {
			editFirstNameDialog.dismiss();
		}
	}

	private void showEditLastNameDialog() {

		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.edit_lastname_dialog, null);

		Button editBtn = dialogView.findViewById(R.id.editBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		EditText editLastname = dialogView.findViewById(R.id.editLastname);

		editLastname.setText(StaticDataPasser.storeLastName);

		editBtn.setOnClickListener(v -> {

		});

		cancelBtn.setOnClickListener(v -> {
			closeEditLastNameDialog();
		});

		builder.setView(dialogView);

		editLastNameDialog = builder.create();
		editLastNameDialog.show();
	}

	private void closeEditLastNameDialog() {
		if (editLastNameDialog != null && editLastNameDialog.isShowing()) {
			editLastNameDialog.dismiss();
		}
	}

	private void showEditAgeDialog() {

		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.edit_age_dialog, null);

		Button editBtn = dialogView.findViewById(R.id.editBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		EditText editAge = dialogView.findViewById(R.id.editAge);

		String ageToString = String.valueOf(StaticDataPasser.storeCurrentAge);
		editAge.setText(ageToString);

		editBtn.setOnClickListener(v -> {

		});

		cancelBtn.setOnClickListener(v -> {
			closeEditAgeDialog();
		});

		builder.setView(dialogView);

		editAgeDialog = builder.create();
		editAgeDialog.show();
	}

	private void closeEditAgeDialog() {
		if (editAgeDialog != null && editAgeDialog.isShowing()) {
			editAgeDialog.dismiss();
		}
	}

	private void showEditSexDialog() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.edit_sex_dialog, null);

		Button editBtn = dialogView.findViewById(R.id.editBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		Spinner spinnerSexDialog = dialogView.findViewById(R.id.spinnerSex);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getContext(),
				R.array.sex_options,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSexDialog.setAdapter(adapter);
		spinnerSexDialog.setSelection(0);
		spinnerSexDialog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					spinnerSexDialog.setSelection(0);
				} else {
					String selectedSex = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedSex = selectedSex;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		if (StaticDataPasser.storeSelectedSex.equals("Male")) {
			spinnerSexDialog.setSelection(1);
		} else {
			spinnerSexDialog.setSelection(2);
		}

		editBtn.setOnClickListener(v -> {

		});

		cancelBtn.setOnClickListener(v -> {
			closeEditSexDialog();
		});

		builder.setView(dialogView);

		editSexDialog = builder.create();
		editSexDialog.show();
	}

	private void closeEditSexDialog() {
		if (editSexDialog != null && editSexDialog.isShowing()) {
			editSexDialog.dismiss();
		}
	}

	private void showEditDisabilityDialog() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.edit_disability_dialog, null);

		Button editBtn = dialogView.findViewById(R.id.editBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
		Spinner spinnerDisabilityDialog = dialogView.findViewById(R.id.spinnerDisabilityDialog);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getContext(),
				R.array.disability_type,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerDisabilityDialog.setAdapter(adapter);
		spinnerDisabilityDialog.setSelection(0);
		spinnerDisabilityDialog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					spinnerDisabilityDialog.setSelection(0);
				} else {
					String selectedDisability = parent.getItemAtPosition(position).toString();
					StaticDataPasser.storeSelectedDisability = selectedDisability;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		editBtn.setOnClickListener(v -> {

		});

		cancelBtn.setOnClickListener(v -> {
			closeEditDisabilityDialog();
		});

		builder.setView(dialogView);

		editDisabilityDialog = builder.create();
		editDisabilityDialog.show();
	}

	private void closeEditDisabilityDialog() {
		if (editDisabilityDialog != null && editDisabilityDialog.isShowing()) {
			editDisabilityDialog.dismiss();
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
		profilePicturePath = profilePictureReference.child("images/profilePictures/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		profilePicturePath.putFile(profilePictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					profilePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								binding.profilePicture.setImageURI(profilePictureUri);
								String profilePictureURL = uri.toString();
								storeProfileImageURLInFireStore(userID, profilePictureURL);
							})
							.addOnFailureListener(e -> {
								Toast.makeText(context, "Profile picture failed to add", Toast.LENGTH_SHORT).show();

								Log.e(TAG, "uploadProfileImageToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> Log.e(TAG, "uploadProfileImageToFirebaseStorage: addOnFailureListener " + e.getMessage()));
	}

	private void uploadVehicleImageToFirebaseStorage(String userID, Bitmap bitmap) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] data = baos.toByteArray();

		StorageReference vehiclePictureReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		vehiclePicturePath = vehiclePictureReference.child("images/vehiclePictures/" + System.currentTimeMillis() + "_" + userID + ".jpg");

		vehiclePicturePath.putBytes(data)
				.addOnSuccessListener(taskSnapshot -> {

					vehiclePicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								String imageUrl = uri.toString();
								storeVehicleImageURLInFireStore(imageUrl, userID);
							})
							.addOnFailureListener(e -> {
								Toast.makeText(context, "Profile picture failed to add", Toast.LENGTH_SHORT).show();

								Log.e(TAG, e.getMessage());
							});
				})
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
	}

	private void storeProfileImageURLInFireStore(String userID, String profilePictureURL) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> profilePicture = new HashMap<>();
		profilePicture.put("profilePicture", profilePictureURL);

		documentReference.update(profilePicture)
				.addOnSuccessListener(unused ->
						Toast.makeText(context, "Profile picture added successfully", Toast.LENGTH_SHORT).show())
				.addOnFailureListener(e -> {
					Toast.makeText(context, "Profile picture failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "storeProfileImageURLInFireStore: addOnFailureListener " + e.getMessage());
				});
	}

	private void storeVehicleImageURLInFireStore(String imageUrl, String userID) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(userID);

		Map<String, Object> vehiclePicture = new HashMap<>();
		vehiclePicture.put("vehiclePicture", imageUrl);

		documentReference.update(vehiclePicture)
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
	//			int pixel = 0;
//			for (int n = 0; n < imageSize; n++) {
//				for (int i = 0; i < imageSize; i++) {
//					int val = intValues[pixel++];
//					byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
//					byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
//					byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
//				}

//			}

	private void identifyCar(Bitmap bitmap) {
//		try {
//			ModelUnquant model = ModelUnquant.newInstance(context);
//
//			// Creates inputs for reference.
//			TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
//			int imageSize = 224;
//			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
//			byteBuffer.order(ByteOrder.nativeOrder());
//
//			//TODO
//			int[] intValues = new int[bitmap.getWidth() * bitmap.getHeight()];
//			bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//			for (int pixelValue : intValues) {
//				byteBuffer.putFloat(((pixelValue >> 16) & 0xFF) * (1.f / 255.f));
//				byteBuffer.putFloat(((pixelValue >> 8) & 0xFF) * (1.f / 255.f));
//				byteBuffer.putFloat((pixelValue & 0xFF) * (1.f / 255.f));
//			}
//			byteBuffer.rewind(); // Set the position back to 0
//			inputFeature0.loadBuffer(byteBuffer);
//
//			// Runs model inference and gets result.
//			ModelUnquant.Outputs outputs = model.process(inputFeature0);
//			TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
//			float[] confidences = outputFeature0.getFloatArray();
//			int maxPos = 0;
//			float maxConfidence = 0;
//			for (int i = 0; i < confidences.length; i++) {
//				if (confidences[i] > maxConfidence) {
//					maxConfidence = confidences[i];
//					maxPos = i;
//				}
//			}
//
//			String[] classes = {"Car", "Not Car"};
//			String predictedClass;
//			if (maxPos == 0) {
//				binding.vehicleImageView.setImageBitmap(bitmap);
//				uploadVehicleImageToFirebaseStorage(FirebaseMain.getUser().getUid(), bitmap);
//			} else {
//				showUploadYourVehiclePictureDialog();
//			}
//
//			// Releases model resources if no longer used.
//			model.close();
//
//		} catch (IOException e) {
//			Log.e(TAG, e.getMessage());
//		}
	}

	public Bitmap uriToBitmap(Context context, Uri uri) throws IOException {
		InputStream inputStream = context.getContentResolver().openInputStream(uri);
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		if (inputStream != null) {
			inputStream.close();
		}
		return bitmap;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && data != null) {

			Uri imageUri = data.getData();

			Bitmap bitmap = null;
			try {
				bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
			bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);


			if (requestCode == PROFILE_PICTURE_REQUEST_CODE) {

				Uri profilePictureUri = data.getData();
				uploadProfileImageToFirebaseStorage(FirebaseMain.getUser().getUid(), profilePictureUri);

			} else if (requestCode == VEHICLE_PICTURE_REQUEST_CODE) {

				identifyCar(bitmap);

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