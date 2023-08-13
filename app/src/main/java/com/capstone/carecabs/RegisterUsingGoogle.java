package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterUsingGoogle extends AppCompatActivity {

    private ImageButton imgBackBtn, passengerImgBtn, driverImgBtn;
    private Button loginHereBtn;
    private static final int RC_SIGN_IN = 69;
    private Intent intent;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInAccount googleSignInAccount;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String registerData;
    private String TAG = "RegisterUsingGoogle";
    private String userID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_using_google);

        imgBackBtn = findViewById(R.id.imgBackBtn);
        passengerImgBtn = findViewById(R.id.passengerImgBtn);
        driverImgBtn = findViewById(R.id.driverImgBtn);
        loginHereBtn = findViewById(R.id.loginHereBtn);

        loginHereBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

        driverImgBtn.setOnClickListener(v -> {
            intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
        });

        passengerImgBtn.setOnClickListener(v -> {
            showUserTypeDialog();
        });

        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

    }

    private void showUserTypeDialog() {

        final Dialog customDialog = new Dialog(this);
        customDialog.setContentView(R.layout.user_type_dialog);

        ImageButton seniorImgBtn = customDialog.findViewById(R.id.seniorImgBtn);
        ImageButton pwdImgBtn = customDialog.findViewById(R.id.pwdImgBtn);
        ImageButton imgBackBtn = customDialog.findViewById(R.id.imgBackBtn);

        seniorImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(RegisterUsingGoogle.this, Register.class);
                registerData = "senior";
                intent.putExtra("registerData", registerData);
                startActivity(intent);
                finish();

                customDialog.dismiss();
            }
        });

        pwdImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(RegisterUsingGoogle.this, Register.class);
                registerData = "pwd";
                intent.putExtra("registerData", registerData);
                startActivity(intent);
                finish();

                customDialog.dismiss();

            }
        });

        imgBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
            }
        });

        customDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                googleSignInAccount = googleSignInAccountTask.getResult(ApiException.class);
                fireBaseAuthWithGoogle(googleSignInAccount.getIdToken());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());

            }
        }

    }
    private void fireBaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        if (googleSignInAccount != null) {
            auth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "googleSignInWithCredential:success");
                        currentUser = auth.getCurrentUser();
                        userID = currentUser.getUid();

                        if (currentUser != null) {
                            String googleEmail = googleSignInAccount.getEmail();
                            String googleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

                            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);

                            Map<String, Object> registerUser = new HashMap<>();
                            registerUser.put("driverID", userID);
                            registerUser.put("email", googleEmail);
                            registerUser.put("profilePic", googleProfilePic);

                            databaseReference.setValue(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {
                                        intent = new Intent(RegisterUsingGoogle.this, LoggingIn.class);
//                                      intent.putExtra("registerData", getRegisterData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task.getException()));

                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    Log.e(TAG, e.getMessage());
                                }
                            });
                        }
                    } else {
                        Toast.makeText(RegisterUsingGoogle.this, "Login failed", Toast.LENGTH_SHORT).show();


                    }
                }
            });
        } else {
            Log.d(TAG, "googleSignInWithCredential:failed");

        }
    }

}