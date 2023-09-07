package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.LoggingOut;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
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

public class AccountFragment extends Fragment {
    private Button signOutBtn, editProfileBtn, changePasswordBtn,
            secuAndPriBtn, appSettingsBtn, aboutBtn, contactUsBtn;
    private ImageButton imgBackBtn;
    private ImageView profilePic;
    private TextView fullNameTextView, userTypeTextView,
            ageTextView, statusTextView, driverStatusTextView,
            birthdateTextView, setTextView, disabilityTextView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String TAG = "AccountFragment";
    private String userID;
    private Intent intent;
    private AlertDialog signOutDialog, pleaseWaitDialog, noInternetDialog,
            profileInfoNotCompleteDialog;
    private AlertDialog.Builder builder;
    private NetworkChangeReceiver networkChangeReceiver;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        context = getContext();
        initializeNetworkChecker();

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        FirebaseApp.initializeApp(context);

        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        userTypeTextView = view.findViewById(R.id.userTypeTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        statusTextView = view.findViewById(R.id.statusTextView);
        driverStatusTextView = view.findViewById(R.id.driverStatusTextView);
        birthdateTextView = view.findViewById(R.id.birthdateTextView);
        setTextView = view.findViewById(R.id.sexTextView);
        disabilityTextView = view.findViewById(R.id.disabilityTextView);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        signOutBtn = view.findViewById(R.id.signOutBtn);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        profilePic = view.findViewById(R.id.profielPic);
        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        secuAndPriBtn = view.findViewById(R.id.secAndPriBtn);
        appSettingsBtn = view.findViewById(R.id.appSettingsBtn);
        contactUsBtn = view.findViewById(R.id.contactUsBtn);
        aboutBtn = view.findViewById(R.id.aboutBtn);

        loadUserProfileInfo();

        editProfileBtn.setOnClickListener(v -> goToEditAccountFragment());

        secuAndPriBtn.setOnClickListener(v -> {

        });

        aboutBtn.setOnClickListener(v -> goToAboutFragment());

        contactUsBtn.setOnClickListener(v -> goToContactUsFragment());

        appSettingsBtn.setOnClickListener(v -> goToAppSettingsFragment());

        changePasswordBtn.setOnClickListener(v -> goToChangePasswordFragment());

        imgBackBtn.setOnClickListener(v -> backToHomeFragment());

        signOutBtn.setOnClickListener(v -> showSignOutDialog());

        return view;
    }

    private void logoutUser() {
        intent = new Intent(getActivity(), LoggingOut.class);
        startActivity(intent);
        getActivity().finish();

        StaticDataPasser.storeSelectedSex = null;
        StaticDataPasser.storeSelectedDisability = null;
        StaticDataPasser.storeCurrentBirthDate = null;
        StaticDataPasser.storeRegisterType = null;
        StaticDataPasser.storeRegisterData = null;
        StaticDataPasser.storeCurrentAge = 0;
        StaticDataPasser.storeFirstName = null;
        StaticDataPasser.storeLastName = null;
        StaticDataPasser.storeHashedPassword = null;
    }

    private void getTheUserType() {
        showPleaseWaitDialog();

        if (currentUser != null) {
            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {


                    //TODO: user info empty
                    if (snapshot.exists()) {
                        String getUserType;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {
                            driverSnapshot = snapshot.child("driver").child(userID);
                            getUserType = driverSnapshot.child("userType").getValue(String.class);

                            closePleaseWaitDialog();

                        } else if (snapshot.child("senior").hasChild(userID)) {
                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getUserType = seniorSnapshot.child("userType").getValue(String.class);

                            closePleaseWaitDialog();

                        } else if (snapshot.child("pwd").hasChild(userID)) {
                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getUserType = pwdSnapshot.child("userType").getValue(String.class);

                            closePleaseWaitDialog();
                        }
                    } else {
                        closePleaseWaitDialog();

                        Log.e(TAG, "Not Exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    closePleaseWaitDialog();

                    Log.e(TAG, error.getMessage());
                }
            });

        } else {
            closePleaseWaitDialog();
            showIncompleteProfileInfoDialog();
        }
    }

    private void loadUserProfileInfo() {
        showPleaseWaitDialog();

        if (currentUser != null) {
            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        String getFirstname, getLastname, getProfilePic, fullName,
                                getUserType, getBirthdate,
                                getSex, getUserVerificationStatus,
                                getDisability;
                        int getAge;
                        Boolean getDriverStatus;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {

                            driverSnapshot = snapshot.child("driver").child(userID);
                            getUserType = driverSnapshot.child("userType").getValue(String.class);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getUserVerificationStatus = driverSnapshot.child("verificationStatus").getValue(String.class);
                            getAge = driverSnapshot.child("age").getValue(Integer.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = driverSnapshot.child("birthdate").getValue(String.class);
                            getSex = driverSnapshot.child("sex").getValue(String.class);
                            getDriverStatus = driverSnapshot.child("isAvailable").getValue(Boolean.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(context).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);
                            birthdateTextView.setText("Birthdate: " + getBirthdate);
                            setTextView.setText("Sex: " + getSex);

                            if (getUserVerificationStatus.equals("Verified")) {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
                            } else {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getUserVerificationStatus);

                            driverStatusTextView.setVisibility(View.VISIBLE);
                            if (getDriverStatus) {
                                driverStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
                                driverStatusTextView.setText("Driver Status: Available");
                            } else {
                                driverStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                                driverStatusTextView.setText("Driver Status: Busy");

                            }

                            closePleaseWaitDialog();

                        } else if (snapshot.child("senior").hasChild(userID)) {

                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getUserVerificationStatus = seniorSnapshot.child("verificationStatus").getValue(String.class);
                            getAge = seniorSnapshot.child("age").getValue(Integer.class);
                            getUserType = seniorSnapshot.child("userType").getValue(String.class);
                            getFirstname = seniorSnapshot.child("firstname").getValue(String.class);
                            getLastname = seniorSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = seniorSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = seniorSnapshot.child("birthdate").getValue(String.class);
                            getSex = seniorSnapshot.child("sex").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(context).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }
                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);
                            birthdateTextView.setText("Birthdate: " + getBirthdate);
                            setTextView.setText("Sex: " + getSex);

                            if (getUserVerificationStatus.equals("Verified")) {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

                            } else {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getUserVerificationStatus);

                            closePleaseWaitDialog();


                        } else if (snapshot.child("pwd").hasChild(userID)) {

                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getUserVerificationStatus = pwdSnapshot.child("verificationStatus").getValue(String.class);
                            getUserType = pwdSnapshot.child("userType").getValue(String.class);
                            getAge = pwdSnapshot.child("age").getValue(Integer.class);
                            getFirstname = pwdSnapshot.child("firstname").getValue(String.class);
                            getLastname = pwdSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = pwdSnapshot.child("profilePic").getValue(String.class);
                            getBirthdate = pwdSnapshot.child("birthdate").getValue(String.class);
                            getSex = pwdSnapshot.child("sex").getValue(String.class);
                            getDisability = pwdSnapshot.child("disability").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(context).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);
                            birthdateTextView.setText("Birthdate: " + getBirthdate);
                            setTextView.setText("Sex: " + getSex);
                            disabilityTextView.setVisibility(View.VISIBLE);
                            disabilityTextView.setText("Disability: " + getDisability);

                            if (getUserVerificationStatus.equals("Verified")) {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

                            } else {
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getUserVerificationStatus);

                            closePleaseWaitDialog();

                        }
                    } else {
                        closePleaseWaitDialog();

                        Log.e(TAG, "Not Exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    closePleaseWaitDialog();

                    Log.e(TAG, error.getMessage());
                }
            });

        } else {
            closePleaseWaitDialog();
            showIncompleteProfileInfoDialog();

            intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (networkChangeReceiver != null) {
            getContext().unregisterReceiver(networkChangeReceiver);
        }

        closeSignOutDialog();
        closePleaseWaitDialog();
        closeIncompleteProfileInfoDialog();
        closeNoInternetDialog();
    }


    @Override
    public void onPause() {
        super.onPause();

        closeSignOutDialog();
        closePleaseWaitDialog();
        closeIncompleteProfileInfoDialog();
        closeNoInternetDialog();
    }

    private void showIncompleteProfileInfoDialog() {
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.profile_info_not_complete_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            getTheUserType();

            intent = new Intent(getActivity(), LoggingOut.class);
            startActivity(intent);
            getActivity().finish();
        });

        builder.setView(dialogView);

        profileInfoNotCompleteDialog = builder.create();
        profileInfoNotCompleteDialog.show();
    }

    private void closeIncompleteProfileInfoDialog() {
        if (profileInfoNotCompleteDialog != null && profileInfoNotCompleteDialog.isShowing()) {
            profileInfoNotCompleteDialog.isShowing();
        }
    }

    private void showSignOutDialog() {
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sign_out, null);

        Button signOutBtn = dialogView.findViewById(R.id.signOutBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        signOutBtn.setOnClickListener(v -> {
            logoutUser();

        });

        cancelBtn.setOnClickListener(v -> {
            closeSignOutDialog();
        });

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


    private void goToEditAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new EditAccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToAppSettingsFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AppSettingsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToAboutFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AboutFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToContactUsFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new ContactUsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToChangePasswordFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new ChangePasswordFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void backToHomeFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
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