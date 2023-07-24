package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterPassenger extends AppCompatActivity {

    private ImageButton imgBackBtn;
    private EditText username, password, confirmPassword;
    private Button nextBtn;
    private LinearLayout progressBarLayout;
    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "RegisterPassenger";
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_passenger);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        imgBackBtn = findViewById(R.id.imgBackBtn);
        nextBtn = findViewById(R.id.nextBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);

        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, RegisterUserType.class);
            startActivity(intent);
            finish();
        });

        nextBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);

            final String stringUsername = username.getText().toString().trim();
            final String stringPassword = password.getText().toString().trim();
            final String stringConfirmPassword = confirmPassword.getText().toString().trim();

            if (stringUsername.isEmpty() || stringPassword.isEmpty() || stringConfirmPassword.isEmpty()) {
                return;
            } else if (!stringConfirmPassword.equals(stringPassword)) {
                return;
            } else {
                auth.createUserWithEmailAndPassword(stringUsername, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            userID = currentUser.getUid();

                            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);

                            Map<String, Object> registerUser = new HashMap<>();
                            registerUser.put("userID", userID);
                            registerUser.put("username", stringUsername);
                            registerUser.put("password", stringPassword);

                            databaseReference.setValue(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {
                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                        intent = new Intent(RegisterPassenger.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressBarLayout.setVisibility(View.GONE);
                                    nextBtn.setVisibility(View.VISIBLE);
                                    Log.e(TAG, e.getMessage());

                                }
                            });

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBarLayout.setVisibility(View.GONE);
                        nextBtn.setVisibility(View.VISIBLE);
                        Log.e(TAG, e.getMessage());
                    }
                });
            }

        });
    }
}