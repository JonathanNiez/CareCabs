package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RegisterPWD extends AppCompatActivity {

    private Button doneBtn, scanIDBtn, birthdateBtn, ageBtn;
    private ImageButton imgBackBtn;
    private EditText firstname, lastname;
    private Spinner spinnerDisability, spinnerSex;
    private LinearLayout progressBarLayout;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "RegisterPWD";
    private Intent intent;
    private Calendar selectedDate;
    private AlertDialog.Builder builder;
    private AlertDialog noInternetDialog;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_pwd);

        initializeNetworkChecker();

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        FirebaseApp.initializeApp(this);

        intent = getIntent();
        String getRegisterData = intent.getStringExtra("registerData");

        doneBtn = findViewById(R.id.doneBtn);
        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        birthdateBtn = findViewById(R.id.birthdateBtn);
        imgBackBtn = findViewById(R.id.imgBackBtn);
        spinnerSex = findViewById(R.id.spinnerSex);
        spinnerDisability = findViewById(R.id.spinnerDisability);
        ageBtn = findViewById(R.id.ageBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);

        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, RegisterUserType.class);
            startActivity(intent);
            finish();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sex_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(adapter);
        spinnerSex.setSelection(0);
        spinnerSex.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    Toast.makeText(RegisterPWD.this, "Please select your sex", Toast.LENGTH_SHORT).show();
                } else {
                    String selectedSex = parent.getItemAtPosition(position).toString();
                    StaticDataPasser.storeSelectedSex = selectedSex;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when nothing is selected
            }
        });

        ArrayAdapter<CharSequence> disabilityAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.disability_type,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDisability.setVisibility(View.VISIBLE);
        spinnerDisability.setAdapter(disabilityAdapter);
        spinnerDisability.setSelection(0);
        spinnerDisability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    spinnerDisability.setSelection(0);
                } else {
                    String selectedDisability = parent.getItemAtPosition(position).toString();
                    StaticDataPasser.storeSelectedDisability = selectedDisability;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerDisability.setSelection(0);
            }
        });


        birthdateBtn.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        doneBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            doneBtn.setVisibility(View.GONE);

            String stringFirstname = firstname.getText().toString().trim();
            String stringLastname = lastname.getText().toString().trim();

            if (stringFirstname.isEmpty() || stringLastname.isEmpty()
                    || StaticDataPasser.storeCurrentBirthDate == null
                    || StaticDataPasser.storeCurrentAge == 0
                    || Objects.equals(StaticDataPasser.storeSelectedSex, "Select your sex")
                    || Objects.equals(StaticDataPasser.storeSelectedDisability, "Select your Disability")) {
                Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();
                progressBarLayout.setVisibility(View.GONE);
                doneBtn.setVisibility(View.VISIBLE);

            } else {
                if (currentUser != null) {
                    userID = currentUser.getUid();

                    if (getRegisterData.equals("Persons with Disabilities (PWD)")) {
                        databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                        Map<String, Object> registerUser = new HashMap<>();
                        registerUser.put("firstname", stringFirstname);
                        registerUser.put("lastname", stringLastname);
                        registerUser.put("disability", StaticDataPasser.storeSelectedDisability);
                        registerUser.put("age", StaticDataPasser.storeCurrentAge);
                        registerUser.put("birthdate", StaticDataPasser.storeCurrentBirthDate);
                        registerUser.put("sex", StaticDataPasser.storeSelectedSex);
                        registerUser.put("userType", "Persons with Disabilities (PWD)");

                        databaseReference.updateChildren(registerUser).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                progressBarLayout.setVisibility(View.GONE);
                                doneBtn.setVisibility(View.VISIBLE);

                                StaticDataPasser.storeSelectedSex = null;
                                StaticDataPasser.storeCurrentAge = 0;
                                StaticDataPasser.storeCurrentBirthDate = null;
                                StaticDataPasser.storeSelectedDisability = null;

                                showRegisterSuccessNotification();

                                intent = new Intent(RegisterPWD.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e(TAG, String.valueOf(task.getException()));

                                progressBarLayout.setVisibility(View.GONE);
                                doneBtn.setVisibility(View.VISIBLE);
                            }
                        }).addOnFailureListener(e -> {
                            progressBarLayout.setVisibility(View.GONE);
                            doneBtn.setVisibility(View.VISIBLE);

                            Log.e(TAG, e.getMessage());
                        });
                    }
                } else {
                    auth.signOut();

                    intent = new Intent(this, Login.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private void calculateAge() {
        if (selectedDate != null) {
            // Calculate the age based on the selected birthdate
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR);

            // Check if the user's birthday has already happened this year or not
            if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            // Update the ageTextView with the calculated age
            ageBtn.setText("Age: " + age);
            StaticDataPasser.storeCurrentAge = age;
        }
    }

    private void showDatePickerDialog() {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);

                    // Update the birthdateTextView with the selected date in a desired format
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    birthdateBtn.setText("Birthdate: " + dateFormat.format(selectedDate.getTime()));
                    StaticDataPasser.storeCurrentBirthDate = dateFormat.format(selectedDate.getTime());

                    // Calculate the age and display it
                    calculateAge();
                }, year, month, day);
        datePickerDialog.show();
    }


    private void showRegisterSuccessNotification() {
        String channelId = "registration_channel_id"; // Change this to your desired channel ID
        String channelName = "CareCabs"; // Change this to your desired channel name
        int notificationId = 3; // Change this to a unique ID for each notification

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Registration Successful")
                .setContentText("You have successfully registered as a PWD");

        Notification notification = builder.build();
        notificationManager.notify(notificationId, notification);
    }

    private void showNoInternetDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.no_internet_dialog, null);

        Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

        tryAgainBtn.setOnClickListener(v -> {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();

                boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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
        registerReceiver(networkChangeReceiver, intentFilter);

        // Initial network status check
        boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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