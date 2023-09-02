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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanID;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditAccountFragment extends Fragment {

    private ImageButton imgBackBtn;
    private ImageView profilePic;
    private Button doneBtn, editFirstnameBtn,
            editLastnameBtn, scanIDBtn, editDisabilityBtn,
            editAgeBtn, editBirthdateBtn, editSexBtn;
    private TextView fullNameTextView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "EditAccountFragment";
    private Intent intent, cameraIntent, galleryIntent;
    private Calendar selectedDate;
    private AlertDialog.Builder builder;
    private AlertDialog editFirstNameDialog, editLastNameDialog,
            editDisabilityDialog, editSexDialog, editAgeDialog,
            cameraGalleryOptionsDialog, noInternetDialog;
    private Uri imageUri;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int STORAGE_PERMISSION_REQUEST = 102;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_account, container, false);

        initializeNetworkChecker();

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        FirebaseApp.initializeApp(getContext());

        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        profilePic = view.findViewById(R.id.profielPic);
        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        editFirstnameBtn = view.findViewById(R.id.editFirstnameBtn);
        editLastnameBtn = view.findViewById(R.id.editLastnameBtn);
        editDisabilityBtn = view.findViewById(R.id.editDisabilityBtn);
        editAgeBtn = view.findViewById(R.id.editAgeBtn);
        editBirthdateBtn = view.findViewById(R.id.editBirthdateBtn);
        editSexBtn = view.findViewById(R.id.editSexBtn);
        doneBtn = view.findViewById(R.id.doneBtn);
        scanIDBtn = view.findViewById(R.id.scanIDBtn);

        getUserType();
        checkPermission();

        editFirstnameBtn.setOnClickListener(v -> {
            showEditFirstNameDialog();
        });

        editLastnameBtn.setOnClickListener(v -> {
            showEditLastNameDialog();
        });

        editAgeBtn.setOnClickListener(v -> {
            showEditAgeDialog();
        });

        editSexBtn.setOnClickListener(v -> {
            showEditSexDialog();
        });

        editDisabilityBtn.setOnClickListener(v -> {
            showEditDisabilityDialog();
        });

        doneBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        profilePic.setOnClickListener(v -> {
            showOptionsDialog();
        });

        scanIDBtn.setOnClickListener(v -> {
            intent = new Intent(getActivity(), ScanID.class);
            startActivity(intent);
        });

        editBirthdateBtn.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        imgBackBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        return view;
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

    private void getUserType() {
        if (currentUser != null) {
            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String getFirstname, getLastname, getProfilePic, fullName,
                                getBirthdate, getSex, getDisability;
                        int getAge;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {
                            driverSnapshot = snapshot.child("driver").child(userID);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = driverSnapshot.child("birthdate").getValue(String.class);
                            getAge = driverSnapshot.child("age").getValue(Integer.class);
                            getSex = driverSnapshot.child("sex").getValue(String.class);

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;
                            StaticDataPasser.storeCurrentAge = getAge;

                            editFirstnameBtn.setText(getFirstname);
                            editLastnameBtn.setText(getLastname);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);

                        } else if (snapshot.child("senior").hasChild(userID)) {
                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getFirstname = seniorSnapshot.child("firstname").getValue(String.class);
                            getLastname = seniorSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = seniorSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = seniorSnapshot.child("birthdate").getValue(String.class);
                            getAge = seniorSnapshot.child("age").getValue(Integer.class);
                            getSex = seniorSnapshot.child("sex").getValue(String.class);

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;
                            StaticDataPasser.storeCurrentAge = getAge;

                            editFirstnameBtn.setText(getFirstname);
                            editLastnameBtn.setText(getLastname);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);


                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }
                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);

                        } else if (snapshot.child("pwd").hasChild(userID)) {
                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getFirstname = pwdSnapshot.child("firstname").getValue(String.class);
                            getLastname = pwdSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = pwdSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = pwdSnapshot.child("birthdate").getValue(String.class);
                            getAge = pwdSnapshot.child("age").getValue(Integer.class);
                            getSex = pwdSnapshot.child("sex").getValue(String.class);
                            getDisability = pwdSnapshot.child("disability").getValue(String.class);

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;
                            StaticDataPasser.storeCurrentAge = getAge;

                            editFirstnameBtn.setText(getFirstname);
                            editLastnameBtn.setText(getLastname);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);
                            editDisabilityBtn.setVisibility(View.VISIBLE);
                            editDisabilityBtn.setText("Disability: " + getDisability);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                        }
                    } else {
                        Log.e(TAG, "Not Exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });

        } else {
            intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
        }

    }

    private void loadUserProfileInfo() {

        if (currentUser != null) {
            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        String getFirstname, getLastname, getProfilePic, fullName,
                                getBirthdate, getSex, getDisability;
                        int getAge;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {
                            driverSnapshot = snapshot.child("driver").child(userID);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = driverSnapshot.child("birthdate").getValue(String.class);
                            getAge = driverSnapshot.child("age").getValue(Integer.class);
                            getSex = driverSnapshot.child("sex").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);

                        } else if (snapshot.child("senior").hasChild(userID)) {
                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getFirstname = seniorSnapshot.child("firstname").getValue(String.class);
                            getLastname = seniorSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = seniorSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = seniorSnapshot.child("birthdate").getValue(String.class);
                            getAge = seniorSnapshot.child("age").getValue(Integer.class);
                            getSex = seniorSnapshot.child("sex").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }
                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);

                        } else if (snapshot.child("pwd").hasChild(userID)) {
                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getFirstname = pwdSnapshot.child("firstname").getValue(String.class);
                            getLastname = pwdSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = pwdSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = pwdSnapshot.child("birthdate").getValue(String.class);
                            getAge = pwdSnapshot.child("age").getValue(Integer.class);
                            getSex = pwdSnapshot.child("sex").getValue(String.class);
                            getDisability = pwdSnapshot.child("disability").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            editDisabilityBtn.setVisibility(View.VISIBLE);
                            editBirthdateBtn.setText("Disability: " + getDisability);
                            editBirthdateBtn.setText("Birthdate: " + getBirthdate);
                            editAgeBtn.setText("Age: " + getAge);
                            editSexBtn.setText("Sex: " + getSex);
                        }
                    } else {
                        Log.e(TAG, "Not Exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });

        } else {
            intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
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
            if (editFirstNameDialog != null && editFirstNameDialog.isShowing()) {
                editFirstNameDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        editFirstNameDialog = builder.create();
        editFirstNameDialog.show();
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
            if (editLastNameDialog != null && editLastNameDialog.isShowing()) {
                editLastNameDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        editLastNameDialog = builder.create();
        editLastNameDialog.show();
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
            if (editAgeDialog != null && editAgeDialog.isShowing()) {
                editAgeDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        editAgeDialog = builder.create();
        editAgeDialog.show();
    }

    private void showEditSexDialog() {
        builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.edit_sex_dialog, null);

        Button editBtn = dialogView.findViewById(R.id.editBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        Spinner spinnerSexDialog = dialogView.findViewById(R.id.spinnerSexDialog);

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
                    return;
                } else {
                    String selectedSex = parent.getItemAtPosition(position).toString();
                    StaticDataPasser.storeSelectedSex = selectedSex;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        editBtn.setOnClickListener(v -> {

        });

        cancelBtn.setOnClickListener(v -> {
            if (editSexDialog != null && editSexDialog.isShowing()) {
                editSexDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        editSexDialog = builder.create();
        editSexDialog.show();
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
                    return;
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
            if (editDisabilityDialog != null && editDisabilityDialog.isShowing()) {
                editDisabilityDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        editDisabilityDialog = builder.create();
        editDisabilityDialog.show();
    }

    private void backToAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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
            if (cameraGalleryOptionsDialog != null && cameraGalleryOptionsDialog.isShowing()) {
                cameraGalleryOptionsDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        cameraGalleryOptionsDialog = builder.create();
        cameraGalleryOptionsDialog.show();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == CAMERA_REQUEST_CODE) {
                if (data != null) {

                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

                    imageUri = getImageUri(getContext(), imageBitmap);
                    profilePic.setImageURI(imageUri);

                    Toast.makeText(getContext(), "Image is Loaded from Camera", Toast.LENGTH_LONG).show();


                } else {
                    Toast.makeText(getContext(), "Image is not Selected", Toast.LENGTH_LONG).show();
                }

            } else if (requestCode == GALLERY_REQUEST_CODE) {
                if (data != null) {

                    imageUri = data.getData();
                    profilePic.setImageURI(imageUri);

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

        View dialogView = getLayoutInflater().inflate(R.layout.no_internet_dialog, null);

        Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

        tryAgainBtn.setOnClickListener(v -> {
            if (noInternetDialog != null && noInternetDialog.isShowing()){
                noInternetDialog.dismiss();

                boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(getContext());
                updateConnectionStatus(isConnected);
            }
        });

        builder.setView(dialogView);

        noInternetDialog = builder.create();
        noInternetDialog.show();
    }
    private void initializeNetworkChecker(){
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