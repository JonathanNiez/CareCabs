package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
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

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private ImageButton imgBackBtn, userTypeImageBtn;
    private EditText email, password, confirmPassword;
    private Button nextBtn;
    private LinearLayout progressBarLayout, googleRegisterLayout;
    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInAccount googleSignInAccount;
    private GoogleSignInOptions googleSignInOptions;
    private String userID;
    private String TAG = "Register";
    private Intent intent;
    private static final int RC_SIGN_IN = 69;
    private AlertDialog pleaseWaitDialog, noInternetDialog, userTypeImageDialog;
    private NetworkConnectivityChecker networkConnectivityChecker;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        networkConnectivityChecker = new NetworkConnectivityChecker(this);
        checkInternetConnection();

        auth = FirebaseAuth.getInstance();

        imgBackBtn = findViewById(R.id.imgBackBtn);
        nextBtn = findViewById(R.id.nextBtn);
        userTypeImageBtn = findViewById(R.id.userTypeImageBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        googleRegisterLayout = findViewById(R.id.googleRegisterLayout);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        intent = getIntent();
        String getRegisterData = intent.getStringExtra("registerData");
        String getRegisterType = intent.getStringExtra("registerType");
        StaticDataPasser.storeRegisterType = getRegisterType;
        StaticDataPasser.storeRegisterData = getRegisterData;

        userTypeImageBtn.setOnClickListener(v -> {
            showUserTypeImageDialog();
        });

        if (getRegisterType != null && getRegisterData != null){
            if (getRegisterType.equals("googleRegister")) {
                intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);

                googleRegisterLayout.setVisibility(View.VISIBLE);
            }
        }else{
            return;
        }


        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, RegisterUserType.class);
            startActivity(intent);
            finish();
        });

        nextBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);

            String stringEmail = email.getText().toString().trim();
            String stringPassword = password.getText().toString();
            String stringConfirmPassword = confirmPassword.getText().toString();
            String hashedPassword = BCrypt.hashpw(stringPassword, BCrypt.gensalt());
            StaticDataPasser.storeHashedPassword = hashedPassword;

            if (stringEmail.isEmpty()) {
                email.setError("Please enter your Email");
                progressBarLayout.setVisibility(View.GONE);
                nextBtn.setVisibility(View.VISIBLE);

            } else if (stringPassword.isEmpty()) {
                email.setError("Please enter your Password");
                progressBarLayout.setVisibility(View.GONE);
                nextBtn.setVisibility(View.VISIBLE);

            } else if (!stringConfirmPassword.equals(stringPassword)) {
                confirmPassword.setError("Password did not matched");
                progressBarLayout.setVisibility(View.GONE);
                nextBtn.setVisibility(View.VISIBLE);

            } else {
                switch (getRegisterData) {
                    case "Driver":

                        Glide.with(this).load(R.drawable.driver).into(userTypeImageBtn);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                currentUser = auth.getCurrentUser();
                                userID = currentUser.getUid();

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("driverID", userID);
                                registerUser.put("userType", getRegisterData);
                                registerUser.put("email", stringEmail);
                                registerUser.put("password", hashedPassword);
                                registerUser.put("status", "Not Verified");
                                registerUser.put("profilePic", "default");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task12 -> {

                                    if (task12.isSuccessful()) {
                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterDriver.class);
                                        intent.putExtra("registerData", getRegisterData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task12.getException()));

                                        StaticDataPasser.storeHashedPassword = null;

                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                    }
                                }).addOnFailureListener(e -> {
                                    StaticDataPasser.storeHashedPassword = null;

                                    progressBarLayout.setVisibility(View.GONE);
                                    nextBtn.setVisibility(View.VISIBLE);
                                    e.printStackTrace();
                                });
                            }
                        }).addOnFailureListener(e -> {
                            StaticDataPasser.storeHashedPassword = null;

                            progressBarLayout.setVisibility(View.GONE);
                            nextBtn.setVisibility(View.VISIBLE);
                            e.printStackTrace();
                        });

                        break;
                    case "Persons with Disability (PWD)":
                        Glide.with(this).load(R.drawable.pwd).into(userTypeImageBtn);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                currentUser = auth.getCurrentUser();
                                userID = currentUser.getUid();

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("userType", getRegisterData);
                                registerUser.put("email", stringEmail);
                                registerUser.put("password", hashedPassword);
                                registerUser.put("status", "Not Verified");
                                registerUser.put("profilePic", "default");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task13 -> {

                                    if (task13.isSuccessful()) {
                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterPWD.class);
                                        intent.putExtra("registerData", getRegisterData);
                                        startActivity(intent);
                                        finish();


                                    } else {
                                        Log.e(TAG, String.valueOf(task13.getException()));

                                        StaticDataPasser.storeHashedPassword = null;

                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                    }
                                }).addOnFailureListener(e -> {
                                    StaticDataPasser.storeHashedPassword = null;

                                    progressBarLayout.setVisibility(View.GONE);
                                    nextBtn.setVisibility(View.VISIBLE);
                                    e.printStackTrace();
                                });
                            }
                        }).addOnFailureListener(e -> {
                            StaticDataPasser.storeHashedPassword = null;

                            progressBarLayout.setVisibility(View.GONE);
                            nextBtn.setVisibility(View.VISIBLE);
                            e.printStackTrace();
                        });

                        break;
                    case "Senior Citizen":
                        Glide.with(this).load(R.drawable.senior).into(userTypeImageBtn);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                currentUser = auth.getCurrentUser();
                                userID = currentUser.getUid();

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("senior").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("userType", getRegisterData);
                                registerUser.put("email", stringEmail);
                                registerUser.put("password", hashedPassword);
                                registerUser.put("status", "Not Verified");
                                registerUser.put("profilePic", "default");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task1 -> {

                                    if (task1.isSuccessful()) {
                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterSenior.class);
                                        intent.putExtra("registerData", getRegisterData);
                                        startActivity(intent);
                                        finish();


                                    } else {
                                        Log.e(TAG, String.valueOf(task1.getException()));

                                        StaticDataPasser.storeHashedPassword = null;

                                        nextBtn.setVisibility(View.VISIBLE);
                                        progressBarLayout.setVisibility(View.GONE);

                                    }
                                }).addOnFailureListener(e -> {
                                    StaticDataPasser.storeHashedPassword = null;

                                    progressBarLayout.setVisibility(View.GONE);
                                    nextBtn.setVisibility(View.VISIBLE);
                                    e.printStackTrace();
                                });
                            }
                        }).addOnFailureListener(e -> {
                            StaticDataPasser.storeHashedPassword = null;

                            progressBarLayout.setVisibility(View.GONE);
                            nextBtn.setVisibility(View.VISIBLE);
                            e.printStackTrace();
                        });

                        break;
                }
            }
        });
    }

    private void showUserTypeImageDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.you_are_signing_as_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);
        TextView registerAsTextView = dialogView.findViewById(R.id.registerAsTextView);

        registerAsTextView.setText("You are Registering as a " + StaticDataPasser.storeRegisterData);

        okBtn.setOnClickListener(v -> {
            if (userTypeImageDialog != null && userTypeImageDialog.isShowing()) {
                userTypeImageDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        userTypeImageDialog = builder.create();
        userTypeImageDialog.show();
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
            showPleaseWaitDialog();
            switch (StaticDataPasser.storeRegisterData) {
                case "Driver":
                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "googleSignInWithCredential:success");
                            currentUser = auth.getCurrentUser();
                            if (currentUser != null) {
                                userID = currentUser.getUid();

                                String googleEmail = googleSignInAccount.getEmail();
                                String googleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("driverID", userID);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("email", googleEmail);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);
                                registerUser.put("status", "Not Verified");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task1 -> {

                                    if (task1.isSuccessful()) {

                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterDriver.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task1.getException()));

                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
                            }
                        } else {
                            Toast.makeText(Register.this, "Register failed", Toast.LENGTH_LONG).show();

                        }
                    });

                    break;
                case "Persons with Disability (PWD)":
                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "googleSignInWithCredential:success");
                            currentUser = auth.getCurrentUser();
                            userID = currentUser.getUid();

                            if (currentUser != null) {
                                String googleEmail = googleSignInAccount.getEmail();
                                String googleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("email", googleEmail);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);
                                registerUser.put("status", "Not Verified");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task12 -> {

                                    if (task12.isSuccessful()) {
                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterPWD.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task12.getException()));



                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
                            }
                        } else {
                            Toast.makeText(Register.this, "Register failed", Toast.LENGTH_LONG).show();
                        }
                    });

                    break;
                case "Senior Citizen":
                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "googleSignInWithCredential:success");
                            currentUser = auth.getCurrentUser();
                            userID = currentUser.getUid();

                            if (currentUser != null) {
                                String googleEmail = googleSignInAccount.getEmail();
                                String googleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("senior").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("email", googleEmail);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);
                                registerUser.put("status", "Not Verified");

                                databaseReference.setValue(registerUser).addOnCompleteListener(task13 -> {

                                    if (task13.isSuccessful()) {

                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterSenior.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        StaticDataPasser.storeHashedPassword = null;

                                        Log.e(TAG, String.valueOf(task13.getException()));
                                    }
                                }).addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    StaticDataPasser.storeHashedPassword = null;
                                });
                            }
                        } else {
                            Toast.makeText(Register.this, "Register failed", Toast.LENGTH_LONG).show();
                            StaticDataPasser.storeHashedPassword = null;

                        }
                    });

                    break;
            }
        } else {
            Log.d(TAG, "googleSignInWithCredential:failed");
        }
    }


    private void showPleaseWaitDialog() {

        builder = new AlertDialog.Builder(this);

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

    private void showNoInternetDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.no_internet_dialog, null);

        Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

        tryAgainBtn.setOnClickListener(v -> {
            checkInternetConnection();
        });

        builder.setView(dialogView);

        noInternetDialog = builder.create();
        noInternetDialog.show();
    }

    private void checkInternetConnection() {
        if (networkConnectivityChecker.isConnected()) {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
        } else {
            showNoInternetDialog();
        }
    }

}