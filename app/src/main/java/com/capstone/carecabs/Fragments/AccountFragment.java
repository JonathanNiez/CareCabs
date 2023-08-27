package com.capstone.carecabs.Fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
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
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountFragment extends Fragment {
    private Button signOutBtn, editProfileBtn, changePasswordBtn, secuAndPriBtn;
    private ImageButton imgBackBtn;
    private ImageView profilePic;
    private TextView fullNameTextView, userTypeTextView, ageTextView, statusTextView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String TAG = "AccountFragment";
    private String userID;
    private Intent intent;
    private AlertDialog signOutDialog, loadingDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        userTypeTextView = view.findViewById(R.id.userTypeTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        statusTextView = view.findViewById(R.id.statusTextView);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        signOutBtn = view.findViewById(R.id.signOutBtn);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        profilePic = view.findViewById(R.id.profielPic);
        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        secuAndPriBtn = view.findViewById(R.id.secAndPriBtn);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        loadUserProfileInfo();

        editProfileBtn.setOnClickListener(v -> {
            goToEditAccountFragment();
        });

        secuAndPriBtn.setOnClickListener(v -> {

        });

        changePasswordBtn.setOnClickListener(v -> {

        });

        imgBackBtn.setOnClickListener(v -> {
            backToHomeFragment();
        });

        signOutBtn.setOnClickListener(v -> {
            showSignOutDialog();
        });

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
        StaticDataPasser.storeHashedPassword = null;    }

    private void loadUserProfileInfo() {
        showLoadingDialog();
        if (currentUser != null) {
            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        String getFirstname, getLastname, getProfilePic, fullName, getUserType, getAge, getStatus;

                        DataSnapshot driverSnapshot, seniorSnapshot, pwdSnapshot;

                        if (snapshot.child("driver").hasChild(userID)) {

                            driverSnapshot = snapshot.child("driver").child(userID);
                            getStatus  = driverSnapshot.child("status").getValue(String.class);
                            getUserType = driverSnapshot.child("userType").getValue(String.class);
                            getFirstname = driverSnapshot.child("firstname").getValue(String.class);
                            getAge = String.valueOf(driverSnapshot.child("age").getValue());
                            getLastname = driverSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = driverSnapshot.child("profilePic").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loading_gif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);

                            if (getUserType.equals("Verified")){
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

                            }else{
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getStatus);

                            closeLoadingDialog();

                        } else if (snapshot.child("senior").hasChild(userID)) {

                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getStatus  = seniorSnapshot.child("status").getValue(String.class);
                            getAge = String.valueOf(seniorSnapshot.child("age").getValue());
                            getUserType = seniorSnapshot.child("userType").getValue(String.class);
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
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);

                            if (getUserType.equals("Verified")){
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

                            }else{
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getStatus);

                            closeLoadingDialog();


                        } else if (snapshot.child("pwd").hasChild(userID)) {

                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getStatus  = pwdSnapshot.child("status").getValue(String.class);
                            getUserType = pwdSnapshot.child("userType").getValue(String.class);
                            getAge = String.valueOf(pwdSnapshot.child("age").getValue());
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
                            userTypeTextView.setText(getUserType);
                            ageTextView.setText("Age: " + getAge);

                            if (getUserType.equals("Verified")){
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

                            }else{
                                statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            }
                            statusTextView.setText("Status: " + getStatus);

                            closeLoadingDialog();

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

    private void showSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.sign_out_dialog, null);

        Button signOutBtn = dialogView.findViewById(R.id.signOutBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        signOutBtn.setOnClickListener(v -> {
            logoutUser();

        });

        cancelBtn.setOnClickListener(v -> {
            if (signOutDialog != null && signOutDialog.isShowing()) {
                signOutDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        signOutDialog = builder.create();
        signOutDialog.show();
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.loading_dialog, null);

        builder.setView(dialogView);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void closeLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }


    private void goToEditAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new EditAccountFragment());
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

    private void createNotificationChannel() {
        String channelId = "channel_id";
        String channelName = "CareCabs";
        String channelDescription = "You have Successfully Registered";

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void buildAndDisplayNotification() {
        int notificationId = 1;
        String channelId = "channel_id";

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getContext(), channelId)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("CareCabs")
                .setContentText("Displaying Profile Info")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Display the notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

}