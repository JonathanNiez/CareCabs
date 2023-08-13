package com.capstone.carecabs.Fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
import com.capstone.carecabs.EditAccountFragment;
import com.capstone.carecabs.LoggingOut;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountFragment extends Fragment {
    private Button signOutBtn, editProfileBtn, changePasswordBtn;
    private ImageButton imgBackBtn;
    private ImageView profilePic;
    private TextView fullNameTextView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String TAG = "AccountFragment";
    private String userID;
    private Intent intent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        signOutBtn = view.findViewById(R.id.signOutBtn);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        profilePic = view.findViewById(R.id.profielPic);
        imgBackBtn = view.findViewById(R.id.imgBackBtn);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        loadUserProfileInfo();

        editProfileBtn.setOnClickListener(v -> {
            goToEditAccountFragment();
        });

        imgBackBtn.setOnClickListener(v -> {
            backToHomeFragment();
        });

        signOutBtn.setOnClickListener(v -> {
            logoutUser();
        });

        return view;
    }

    private void logoutUser() {
        intent = new Intent(getActivity(), LoggingOut.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void loadUserProfileInfo() {

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

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loadingif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }

                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);

                        } else if (snapshot.child("senior").hasChild(userID)) {
                            buildAndDisplayNotification();

                            seniorSnapshot = snapshot.child("senior").child(userID);
                            getFirstname = seniorSnapshot.child("firstname").getValue(String.class);
                            getLastname = seniorSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = seniorSnapshot.child("profilePic").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loadingif).into(profilePic);
                            } else {
                                profilePic.setImageResource(R.drawable.account);
                            }
                            fullName = String.format("%s %s", getFirstname, getLastname);
                            fullNameTextView.setText(fullName);

                        } else if (snapshot.child("pwd").hasChild(userID)) {
                            buildAndDisplayNotification();

                            pwdSnapshot = snapshot.child("pwd").child(userID);
                            getFirstname = pwdSnapshot.child("firstname").getValue(String.class);
                            getLastname = pwdSnapshot.child("lastname").getValue(String.class);
                            getProfilePic = pwdSnapshot.child("profilePic").getValue(String.class);

                            if (!getProfilePic.equals("default")) {
                                Glide.with(getContext()).load(getProfilePic).placeholder(R.drawable.loadingif).into(profilePic);
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