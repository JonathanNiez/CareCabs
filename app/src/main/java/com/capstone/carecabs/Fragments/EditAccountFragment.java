package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.capstone.carecabs.ScanID;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditAccountFragment extends Fragment {

    private ImageButton imgBackBtn;
    private ImageView profilePic;
    private Button birthdateBtn, doneBtn, editFirstnameBtn, editLastameBtn, scanIDBtn;
    private TextView fullNameTextView;
    private Spinner spinnerDisability, spinnerSex;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "EditAccountFragment";
    private Intent intent;
    private Calendar selectedDate;
    private AlertDialog.Builder builder;
    private AlertDialog editFirstNameDialog, editLastNameDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_account, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        getUserType();

        spinnerDisability = view.findViewById(R.id.spinnerDisability);
        spinnerSex = view.findViewById(R.id.spinnerSex);
        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        profilePic = view.findViewById(R.id.profielPic);
        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        editFirstnameBtn = view.findViewById(R.id.editFirstnameBtn);
        editLastameBtn = view.findViewById(R.id.editLastnameBtn);
        birthdateBtn = view.findViewById(R.id.birthdateBtn);
        doneBtn = view.findViewById(R.id.doneBtn);
        scanIDBtn = view.findViewById(R.id.scanIDBtn);

        editFirstnameBtn.setOnClickListener(v -> {
            showEditFirstNameDialog();
        });

        editLastameBtn.setOnClickListener(v -> {

        });

        doneBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        scanIDBtn.setOnClickListener(v -> {
            intent = new Intent(getActivity(), ScanID.class);
            startActivity(intent);
        });

        birthdateBtn.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        imgBackBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.sex_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDisability.setVisibility(View.VISIBLE);
        spinnerDisability.setAdapter(adapter);
        spinnerDisability.setSelection(0);
        spinnerDisability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

                    birthdateBtn.setText(dateFormat.format(selectedDate.getTime()));

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

                        String getFirstname, getLastname, getProfilePic, fullName;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {
                            driverSnapshot = snapshot.child("driver").child(userID);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;

                            editFirstnameBtn.setText(getFirstname);
                            editLastameBtn.setText(getLastname);

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

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;

                            editFirstnameBtn.setText(getFirstname);
                            editLastameBtn.setText(getLastname);

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

                            StaticDataPasser.storeFirstName = getFirstname;
                            StaticDataPasser.storeLastName = getLastname;

                            editFirstnameBtn.setText(getFirstname);
                            editLastameBtn.setText(getLastname);

                            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                                    getContext(),
                                    R.array.disability_type,
                                    android.R.layout.simple_spinner_item
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerDisability.setVisibility(View.VISIBLE);
                            spinnerDisability.setAdapter(adapter);
                            spinnerDisability.setSelection(0);
                            spinnerDisability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    if (position == 0) {
                                        spinnerDisability.setSelection(0);
                                    } else {
                                        String selectedSex = parent.getItemAtPosition(position).toString();
                                        StaticDataPasser.storeSelectedDisability = selectedSex;
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                    spinnerDisability.setSelection(0);
                                }
                            });


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

                        Log.i(TAG, "Exist");

                        String getFirstname, getLastname, getProfilePic, fullName;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {
                            driverSnapshot = snapshot.child("driver").child(userID);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);

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

    private void showEditFirstNameDialog() {

        builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.edit_firstname_dialog, null);

        Button doneBtn = dialogView.findViewById(R.id.doneBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        EditText editFirstName = dialogView.findViewById(R.id.editFirstname);

        editFirstName.setText(StaticDataPasser.storeFirstName);

        doneBtn.setOnClickListener(v -> {

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

    private void backToAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

}