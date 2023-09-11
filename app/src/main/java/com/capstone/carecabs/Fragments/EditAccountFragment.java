package com.capstone.carecabs.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanID;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentEditAccountBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditAccountFragment extends Fragment {
	private String userID;
	private final String TAG = "EditAccountFragment";
	private Intent intent, cameraIntent, galleryIntent;
	private Calendar selectedDate;
	private AlertDialog.Builder builder;
	private AlertDialog editFirstNameDialog, editLastNameDialog,
			editDisabilityDialog, editSexDialog, editAgeDialog,
			cameraGalleryOptionsDialog, noInternetDialog,
			profilePicUpdateSuccessDialog, profilePicUpdateFailedDialog,
			profilePicUpdateSuccessConfirmation, pleaseWaitDialog;
	private Uri imageUri;
	private static final int CAMERA_REQUEST_CODE = 1;
	private static final int GALLERY_REQUEST_CODE = 2;
	private static final int CAMERA_PERMISSION_REQUEST = 101;
	private static final int STORAGE_PERMISSION_REQUEST = 102;
	private NetworkChangeReceiver networkChangeReceiver;
	private Context context;
	private StorageReference storageRef;
	private StorageReference imagesRef;
	private StorageReference fileRef;
	private RequestManager requestManager;
	private DocumentReference documentReference;

	private FragmentEditAccountBinding binding;

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (requestManager != null) {
			requestManager.clear(binding.imgBtnProfilePic);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentEditAccountBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.editMedConBtn.setVisibility(View.GONE);
		binding.editDisabilityBtn.setVisibility(View.GONE);
		binding.idScannedTextView.setVisibility(View.GONE);

		context = getContext();
		initializeNetworkChecker();

		requestManager = Glide.with(context);
		FirebaseApp.initializeApp(context);

		storageRef = FirebaseStorage.getInstance().getReference();
		imagesRef = storageRef.child("images");

		loadUserProfileInfo();

		binding.editFirstnameBtn.setOnClickListener(v -> {
			showEditFirstNameDialog();
		});

		binding.editLastnameBtn.setOnClickListener(v -> {
			showEditLastNameDialog();
		});

		binding.editAgeBtn.setOnClickListener(v -> {
			showEditAgeDialog();
		});

		binding.editSexBtn.setOnClickListener(v -> {
			showEditSexDialog();
		});

		binding.editDisabilityBtn.setOnClickListener(v -> {
			showEditDisabilityDialog();
		});

		binding.doneBtn.setOnClickListener(v -> {
			backToAccountFragment();
		});

		binding.imgBtnProfilePic.setOnClickListener(v -> {
			showOptionsDialog();
			checkPermission();

//            ImagePicker.with(this)
//                    .crop()                    //Crop image(Optional), Check Customization for more option
//                    .compress(1024)            //Final image size will be less than 1 MB(Optional)
//                    .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
//                    .start();

		});

		binding.scanIDBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), ScanID.class);
			startActivity(intent);
		});

		binding.editBirthdateBtn.setOnClickListener(v -> {
			showDatePickerDialog();
		});

		binding.imgBackBtn.setOnClickListener(v -> {
			backToAccountFragment();
		});

		return view;
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
		closeOptionsDialog();
		closeProfilePicUpdateFailed();
		closeProfilePicUpdateSuccess();
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
		closeOptionsDialog();
		closeProfilePicUpdateFailed();
		closeProfilePicUpdateSuccess();
	}

	public void onBackPressed() {
		backToAccountFragment();
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

	private void loadUserProfileInfo() {
		showPleaseWaitDialog();

		if (FirebaseMain.getUser() != null) {
			userID = FirebaseMain.getUser().getUid();

			userID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection).document(userID);

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {
					closePleaseWaitDialog();

					String getProfilePicture = documentSnapshot.getString("profilePicture");
					String getUserType = documentSnapshot.getString("userType");
					String getFirstName = documentSnapshot.getString("firstname");
					String getLastName = documentSnapshot.getString("lastname");
					Long getAgeLong = documentSnapshot.getLong("age");
					int getAge = getAgeLong.intValue();
					String fullName = getFirstName + " " + getLastName;
					String getEmail = documentSnapshot.getString("email");
					String getPhoneNumber = documentSnapshot.getString("phoneNumber");
					String getSex = documentSnapshot.getString("sex");
					String getVerificationStatus = documentSnapshot.getString("verificationStatus");
					String getBirthdate = documentSnapshot.getString("birthdate");

					switch (getUserType) {
						case "Driver":

							break;

						case "Persons with Disabilities (PWD)":
							String getDisability = documentSnapshot.getString("disability");
							binding.editDisabilityBtn.setVisibility(View.VISIBLE);
							binding.editDisabilityBtn.setText("Disability: " + getDisability);
							binding.userTypeImageView.setImageResource(R.drawable.pwd_64);


							break;

						case "Senior Citizen":
							String getMedicalCondition = documentSnapshot.getString("medicalCondition");

							binding.editMedConBtn.setVisibility(View.VISIBLE);
							binding.editMedConBtn.setText(getMedicalCondition);
							binding.userTypeImageView.setImageResource(R.drawable.senior_64_2);

							break;
					}

					if (!getProfilePicture.equals("default")) {
						Glide.with(context).load(getProfilePicture).centerCrop().placeholder(R.drawable.loading_gif).into(binding.imgBtnProfilePic);
					}

					if (getVerificationStatus.equals("Not Verified")) {
						binding.idScannedTextView.setVisibility(View.VISIBLE);
					}

					StaticDataPasser.storeFirstName = getFirstName;
					StaticDataPasser.storeLastName = getLastName;
					StaticDataPasser.storeCurrentAge = getAge;
					StaticDataPasser.storeCurrentBirthDate = getBirthdate;
					StaticDataPasser.storeSelectedSex = getSex;

					binding.fullNameTextView.setText(fullName);
					binding.editFirstnameBtn.setText(getFirstName);
					binding.editLastnameBtn.setText(getLastName);
					binding.userTypeTextView.setText(getUserType);
					binding.editBirthdateBtn.setText("Birthdate: " + getBirthdate);
					binding.editAgeBtn.setText("Age: " + getAge);
					binding.editSexBtn.setText("Sex: " + getSex);

				} else {
					closePleaseWaitDialog();

				}
			}).addOnFailureListener(e -> {
				closePleaseWaitDialog();

				Log.e(TAG, e.getMessage());
			});

		} else {
			intent = new Intent(getActivity(), Login.class);
			startActivity(intent);
		}

	}

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.please_wait_dialog, null);

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

        if (StaticDataPasser.storeSelectedSex.equals("Male")){
			spinnerSexDialog.setSelection(1);
        }else {
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

	private void showOptionsDialog() {
		builder = new AlertDialog.Builder(getContext());

		View dialogView = getLayoutInflater().inflate(R.layout.camera_gallery_dialog, null);

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
			closeOptionsDialog();
		});

		builder.setView(dialogView);

		cameraGalleryOptionsDialog = builder.create();
		cameraGalleryOptionsDialog.show();
	}

	private void closeOptionsDialog() {
		if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
			cameraGalleryOptionsDialog.dismiss();
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
		galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		galleryIntent.setType("image/*");
		if (galleryIntent.resolveActivity(getActivity().getPackageManager()) != null) {
			startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
			if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
				cameraGalleryOptionsDialog.dismiss();
			}
			Toast.makeText(getContext(), "Opened Gallery", Toast.LENGTH_LONG).show();

		} else {
			Toast.makeText(getContext(), "No gallery app found", Toast.LENGTH_LONG).show();
		}
	}

	private void openCamera() {
		cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
			startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
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

	private void uploadImageToFirebaseStorage(Uri imageUri) {
		fileRef = imagesRef.child(imageUri.getLastPathSegment());
		fileRef.putFile(imageUri).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {
				fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

					String imageUrl = uri.toString();
					Map<String, Object> data = new HashMap<>();
					data.put("profilePicUrl", uri.toString());
					binding.imgBtnProfilePic.setImageURI(imageUri);
					uploadImageUriInDatabase(imageUrl);

				}).addOnFailureListener(e -> {

					showProfilePicUpdateFailed();
					Log.e(TAG, e.getMessage());
				});
			} else {
				showProfilePicUpdateFailed();
				Log.e(TAG, String.valueOf(task.getException()));
			}
		});
	}

	private void uploadImageUriInDatabase(String xImageUrl) {
		if (FirebaseMain.getUser() != null) {
			userID = FirebaseMain.getUser().getUid();


		} else {
			intent = new Intent(getActivity(), Login.class);
			startActivity(intent);

			Log.e(TAG, "currentUser is null");
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CAMERA_REQUEST_CODE) {
				if (data != null) {

					Bundle extras = data.getExtras();
					Bitmap imageBitmap = (Bitmap) extras.get("data");

					imageUri = getImageUri(context, imageBitmap);
					uploadImageToFirebaseStorage(imageUri);

					Toast.makeText(context, "Image is Loaded from Camera", Toast.LENGTH_LONG).show();


				} else {
					Toast.makeText(context, "Image is not Selected", Toast.LENGTH_LONG).show();
				}

			} else if (requestCode == GALLERY_REQUEST_CODE) {
				if (data != null) {

					imageUri = data.getData();
					uploadImageToFirebaseStorage(imageUri);

					Toast.makeText(getContext(), "Image is Loaded from Gallery", Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(getContext(), "Image is not Selected", Toast.LENGTH_LONG).show();
				}
			}

		} else {
			Toast.makeText(getContext(), "Image is not Selected", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
}