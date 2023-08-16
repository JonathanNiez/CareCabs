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
import android.widget.Toast;

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

    private ImageButton imgBackBtn;
    private EditText email, password, confirmPassword;
    private Button nextBtn;
    private LinearLayout progressBarLayout;
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
    private AlertDialog customDialog, noInternetDialog;
    private NetworkConnectivityChecker networkConnectivityChecker;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        networkConnectivityChecker = new NetworkConnectivityChecker(this);
        checkInternetConnection();

        auth = FirebaseAuth.getInstance();

        createNotificationChannel();

        imgBackBtn = findViewById(R.id.imgBackBtn);
        nextBtn = findViewById(R.id.nextBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        intent = getIntent();
        String getRegisterData = intent.getStringExtra("registerData");
        String getRegisterType = intent.getStringExtra("registerType");
        StaticDataPasser.registerType = getRegisterType;
        StaticDataPasser.registerData = getRegisterData;

        if (getRegisterType.equals("googleRegister")) {
            intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
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
            String stringPassword = password.getText().toString().trim();
            String stringConfirmPassword = confirmPassword.getText().toString().trim();
            String hashedPassword = BCrypt.hashpw(stringPassword, BCrypt.gensalt());

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
                    case "driver":
                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    currentUser = auth.getCurrentUser();
                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("driverID", userID);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);

                                    databaseReference.setValue(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()) {
                                                nextBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);

                                                buildAndDisplayNotification();

                                                intent = new Intent(Register.this, RegisterDriver.class);
                                                intent.putExtra("registerData", getRegisterData);
                                                startActivity(intent);
                                                finish();

                                            } else {
                                                Log.e(TAG, String.valueOf(task.getException()));

                                                nextBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);

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

                        break;
                    case "pwd":
                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    currentUser = auth.getCurrentUser();
                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("userID", userID);
                                    registerUser.put("userType", getRegisterData);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);

                                    databaseReference.setValue(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()) {
                                                nextBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);

                                                buildAndDisplayNotification();

                                                intent = new Intent(Register.this, RegisterPWD.class);
                                                intent.putExtra("registerData", getRegisterData);
                                                startActivity(intent);
                                                finish();


                                            } else {
                                                Log.e(TAG, String.valueOf(task.getException()));

                                                nextBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);

                                            }
                                        }
                                    }).addOnFailureListener(e -> {
                                        progressBarLayout.setVisibility(View.GONE);
                                        nextBtn.setVisibility(View.VISIBLE);
                                        Log.e(TAG, e.getMessage());
                                    });
                                }
                            }
                        }).addOnFailureListener(e -> {
                            progressBarLayout.setVisibility(View.GONE);
                            nextBtn.setVisibility(View.VISIBLE);
                            Log.e(TAG, e.getMessage());
                        });

                        break;
                    case "senior":
                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    currentUser = auth.getCurrentUser();
                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("senior").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("userID", userID);
                                    registerUser.put("userType", getRegisterData);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);
                                    registerUser.put("status", "not_verified");

                                    databaseReference.setValue(registerUser).addOnCompleteListener(task1 -> {

                                        if (task1.isSuccessful()) {
                                            nextBtn.setVisibility(View.VISIBLE);
                                            progressBarLayout.setVisibility(View.GONE);

                                            buildAndDisplayNotification();

                                            intent = new Intent(Register.this, RegisterSenior.class);
                                            intent.putExtra("registerData", getRegisterData);
                                            startActivity(intent);
                                            finish();


                                        } else {
                                            Log.e(TAG, String.valueOf(task1.getException()));

                                            nextBtn.setVisibility(View.VISIBLE);
                                            progressBarLayout.setVisibility(View.GONE);

                                        }
                                    }).addOnFailureListener(e -> {
                                        progressBarLayout.setVisibility(View.GONE);
                                        nextBtn.setVisibility(View.VISIBLE);
                                        Log.e(TAG, e.getMessage());
                                    });
                                }
                            }
                        }).addOnFailureListener(e -> {
                            progressBarLayout.setVisibility(View.GONE);
                            nextBtn.setVisibility(View.VISIBLE);
                            Log.e(TAG, e.getMessage());
                        });

                        break;
                }
            }
        });
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
            switch (StaticDataPasser.registerData) {
                case "driver":
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

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("driverID", userID);
                                    registerUser.put("email", googleEmail);
                                    registerUser.put("profilePic", googleProfilePic);

                                    databaseReference.setValue(registerUser).addOnCompleteListener(task1 -> {

                                        if (task1.isSuccessful()) {
                                            buildAndDisplayNotification();
                                            closePleaseWaitDialog();

                                            intent = new Intent(Register.this, RegisterDriver.class);
                                            intent.putExtra("registerData", StaticDataPasser.registerData);
                                            startActivity(intent);
                                            finish();

                                        } else {
                                            Log.e(TAG, String.valueOf(task1.getException()));

                                        }
                                    }).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
                                }
                            } else {
                                Toast.makeText(Register.this, "Login failed", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });

                    break;
                case "pwd":
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
                                registerUser.put("profilePic", googleProfilePic);

                                databaseReference.setValue(registerUser).addOnCompleteListener(task12 -> {

                                    if (task12.isSuccessful()) {
                                        buildAndDisplayNotification();
                                        closePleaseWaitDialog();

                                        intent = new Intent(Register.this, RegisterPWD.class);
                                        intent.putExtra("registerData", StaticDataPasser.registerData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task12.getException()));

                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
                            }
                        } else {
                            Toast.makeText(Register.this, "Register failed", Toast.LENGTH_SHORT).show();


                        }
                    });

                    break;
                case "senior":
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
                                registerUser.put("email", googleEmail);
                                registerUser.put("profilePic", googleProfilePic);

                                databaseReference.setValue(registerUser).addOnCompleteListener(task13 -> {

                                    if (task13.isSuccessful()) {
                                        buildAndDisplayNotification();

                                        closePleaseWaitDialog();

                                        intent = new Intent(Register.this, RegisterSenior.class);
                                        intent.putExtra("registerData", StaticDataPasser.registerData);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Log.e(TAG, String.valueOf(task13.getException()));

                                    }
                                }).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
                            }
                        } else {
                            Toast.makeText(Register.this, "Register failed", Toast.LENGTH_SHORT).show();


                        }
                    });

                    break;
            }
        } else {
            Log.d(TAG, "googleSignInWithCredential:failed");
        }
    }

    private void createNotificationChannel() {
        String channelId = "channel_id";
        String channelName = "CareCabs";
        String channelDescription = "You have Successfully Registered";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("CareCabs")
                .setContentText("You have Successfully Registered")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Display the notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void showPleaseWaitDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.please_wait_dialog, null);

        builder.setView(dialogView);

        customDialog = builder.create();
        customDialog.show();
    }

    private void closePleaseWaitDialog() {
        if (customDialog != null && customDialog.isShowing()) {
            customDialog.dismiss();
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