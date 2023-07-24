package com.capstone.carecabs.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.capstone.carecabs.GetStarted;
import com.capstone.carecabs.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private TextView usernameTextView;
    private Button logoutBtn;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "HomeFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        displayUserInfo();

        logoutBtn = view.findViewById(R.id.logoutBtn);
        usernameTextView = view.findViewById(R.id.usernameTextView);

        logoutBtn.setOnClickListener(v -> {
            logoutUser();
        });

        return view;
    }
    private void displayUserInfo(){
        if (currentUser != null){

            userID = currentUser.getUid();

            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String getFirstname = snapshot.child("firstname").getValue(String.class);
                        String getLastname = snapshot.child("lastname").getValue(String.class);

                        String fullName = String.format("%s %s", getFirstname, getLastname);
                        usernameTextView.setText(fullName);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private void logoutUser() {
        auth.signOut();
        // Optional: Navigate to the login activity or any other appropriate activity
        // For example, if you have a main activity that handles the navigation, you can navigate to it.
        Intent intent = new Intent(getActivity(), GetStarted.class);
        startActivity(intent);
        getActivity().finish(); // Optional: Finish the current activity (fragment) to prevent the user from going back after logging out
    }
}