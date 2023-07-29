package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.capstone.carecabs.Static.StaticDataPasser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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

public class RegisterDriver extends AppCompatActivity {

    private Button doneBtn, birthdateBtn;
    private EditText firstname, lastname;
    private TextView ageTextView, birthdateTextView;
    private Spinner spinnerSex;
    private LinearLayout progressBarLayout;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "RegisterDriver";
    private Intent intent;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);

        doneBtn = findViewById(R.id.doneBtn);
        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        birthdateBtn = findViewById(R.id.birthdateBtn);
        ageTextView = findViewById(R.id.ageTextView);
        birthdateTextView = findViewById(R.id.birthdateTextView);
        spinnerSex = findViewById(R.id.spinnerSex);
        progressBarLayout = findViewById(R.id.progressBarLayout);

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
                    Toast.makeText(RegisterDriver.this, "Please select your sex", Toast.LENGTH_SHORT).show();
                } else {
                    String selectedSex = parent.getItemAtPosition(position).toString();
                    StaticDataPasser.selectedSex = selectedSex;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when nothing is selected
            }
        });

        birthdateBtn.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        auth = FirebaseAuth.getInstance();

        intent = getIntent();
        String getRegisterData = intent.getStringExtra("registerData");

        doneBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            doneBtn.setVisibility(View.GONE);

            String stringFirstname = firstname.getText().toString().trim();
            String stringLastname = lastname.getText().toString().trim();

            if (stringFirstname.isEmpty() || stringLastname.isEmpty()
                    || StaticDataPasser.currentBirthDate == null
                    || StaticDataPasser.currentAge == 0
            || Objects.equals(StaticDataPasser.selectedSex, "Select your sex")) {
                Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();
                progressBarLayout.setVisibility(View.GONE);
                doneBtn.setVisibility(View.VISIBLE);

            } else {
                currentUser = auth.getCurrentUser();
                userID = currentUser.getUid();

                if (getRegisterData.equals("driver")) {
                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                    Map<String, Object> registerUser = new HashMap<>();
                    registerUser.put("firstname", stringFirstname);
                    registerUser.put("lastname", stringLastname);
                    registerUser.put("age", StaticDataPasser.currentAge);
                    registerUser.put("profilePic", "default");
                    registerUser.put("isAvailable", true);
                    registerUser.put("birthdate", StaticDataPasser.currentBirthDate);
                    registerUser.put("sex", StaticDataPasser.selectedSex);

                    databaseReference.updateChildren(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                progressBarLayout.setVisibility(View.GONE);
                                doneBtn.setVisibility(View.VISIBLE);

                                StaticDataPasser.selectedSex = null;
                                StaticDataPasser.currentAge = 0;
                                StaticDataPasser.currentBirthDate = null;

                                intent = new Intent(RegisterDriver.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBarLayout.setVisibility(View.GONE);
                            doneBtn.setVisibility(View.VISIBLE);

                            Log.e(TAG, e.getMessage());
                        }
                    });
                }
            }
        });
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
            ageTextView.setText("Age: " + String.valueOf(age));
            StaticDataPasser.currentAge = age;
        }
    }

    private void showDatePickerDialog() {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        selectedDate = Calendar.getInstance();
                        selectedDate.set(year, monthOfYear, dayOfMonth);

                        // Update the birthdateTextView with the selected date in a desired format
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        birthdateTextView.setText("Birthdate: " + dateFormat.format(selectedDate.getTime()));
                        //TODO: date and time
                        StaticDataPasser.currentBirthDate = String.valueOf(selectedDate.getTime());

                        // Calculate the age and display it
                        calculateAge();
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

}