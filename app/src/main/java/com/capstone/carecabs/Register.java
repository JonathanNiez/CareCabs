package com.capstone.carecabs;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
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
    private EditText email, password, confirmPassword, phoneNumber;
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
    private boolean shouldExit = false;
    private Intent intent;
    private static final int RC_SIGN_IN = 69;
    private AlertDialog pleaseWaitDialog, noInternetDialog, userTypeImageDialog,
            ageInfoDialog, registerFailedDialog, cancelRegisterDialog;
    private AlertDialog.Builder builder;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeNetworkChecker();

        auth = FirebaseAuth.getInstance();
        FirebaseApp.initializeApp(this);

        imgBackBtn = findViewById(R.id.imgBackBtn);
        nextBtn = findViewById(R.id.nextBtn);
        userTypeImageBtn = findViewById(R.id.userTypeImageBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        phoneNumber = findViewById(R.id.phoneNumber);

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

        if (getRegisterType != null && getRegisterData != null) {
            if (getRegisterType.equals("googleRegister")) {
                intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);

                showPleaseWaitDialog();

            } else if (getRegisterData.equals("Senior Citizen")) {
                showAgeInfoDialog();
            } else if (getRegisterType.equals("googleRegister") &&
                    getRegisterData.equals("Senior Citizen")) {
                showAgeInfoDialog();
            }
        } else {
            return;
        }

        imgBackBtn.setOnClickListener(v -> {
            showCancelRegisterDialog();
        });

        nextBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);

            String stringEmail = email.getText().toString().trim();
            String stringPassword = password.getText().toString();
            String stringConfirmPassword = confirmPassword.getText().toString();
            String stringPhoneNumber = phoneNumber.getText().toString().trim();
            String prefixPhoneNumber = "+63" + stringPhoneNumber;
            String hashedPassword = BCrypt.hashpw(stringPassword, BCrypt.gensalt());
            StaticDataPasser.storeHashedPassword = hashedPassword;

            if (stringEmail.isEmpty() || stringPassword.isEmpty()
                    || stringPhoneNumber.isEmpty()) {
                email.setError("Please enter your Email");
                progressBarLayout.setVisibility(View.GONE);
                nextBtn.setVisibility(View.VISIBLE);

            } else if (!stringConfirmPassword.equals(stringPassword)) {
                confirmPassword.setError("Password did not matched");
                progressBarLayout.setVisibility(View.GONE);
                nextBtn.setVisibility(View.VISIBLE);

            } else {
                switch (getRegisterData) {
                    case "Driver":

                        userTypeImageBtn.invalidate();
                        Glide.with(this).load(R.drawable.driver_24).centerCrop().placeholder(R.drawable.loading_gif).into(userTypeImageBtn);
//                        userTypeImageBtn.setImageResource(R.drawable.driver_24);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                currentUser = auth.getCurrentUser();

                                if (currentUser != null) {
                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("driverID", userID);
                                    registerUser.put("userType", getRegisterData);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);
                                    registerUser.put("profilePic", "default");
                                    registerUser.put("phoneNumber", prefixPhoneNumber);

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
                                            showRegisterFailedDialog();

                                            StaticDataPasser.storeHashedPassword = null;

                                            nextBtn.setVisibility(View.VISIBLE);
                                            progressBarLayout.setVisibility(View.GONE);

                                            Log.e(TAG, String.valueOf(task12.getException()));
                                        }
                                    });

                                } else {
                                    showRegisterFailedDialog();

                                    Log.e(TAG, "currentUser is null");
                                }
                            } else {
                                showRegisterFailedDialog();

                                StaticDataPasser.storeHashedPassword = null;

                                progressBarLayout.setVisibility(View.GONE);
                                nextBtn.setVisibility(View.VISIBLE);

                                Log.e(TAG, String.valueOf(task.getException()));
                            }
                        });
                        break;

                    case "Persons with Disability (PWD)":

                        userTypeImageBtn.invalidate();
                        Glide.with(this).load(R.drawable.pwd_24).centerCrop().placeholder(R.drawable.loading_gif).into(userTypeImageBtn);
//                        userTypeImageBtn.setImageResource(R.drawable.pwd);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                currentUser = auth.getCurrentUser();

                                if (currentUser != null) {

                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("userID", userID);
                                    registerUser.put("userType", getRegisterData);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);
                                    registerUser.put("profilePic", "default");
                                    registerUser.put("phoneNumber", prefixPhoneNumber);

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

                                            showRegisterFailedDialog();

                                            StaticDataPasser.storeHashedPassword = null;

                                            nextBtn.setVisibility(View.VISIBLE);
                                            progressBarLayout.setVisibility(View.GONE);

                                        }
                                    });

                                } else {
                                    showRegisterFailedDialog();

                                    Log.e(TAG, "currentUser is null");
                                }
                            } else {
                                showRegisterFailedDialog();

                                StaticDataPasser.storeHashedPassword = null;

                                nextBtn.setVisibility(View.VISIBLE);
                                progressBarLayout.setVisibility(View.GONE);

                                Log.e(TAG, String.valueOf(task.getException()));
                            }
                        });
                        break;

                    case "Senior Citizen":

//                        userTypeImageBtn.invalidate();
                        Glide.with(this).load(R.drawable.senior_24png).centerCrop().placeholder(R.drawable.loading_gif).into(userTypeImageBtn);
//                        userTypeImageBtn.setImageResource(R.drawable.senior);

                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                currentUser = auth.getCurrentUser();

                                if (currentUser != null) {

                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child("senior").child(userID);

                                    Map<String, Object> registerUser = new HashMap<>();
                                    registerUser.put("userID", userID);
                                    registerUser.put("userType", getRegisterData);
                                    registerUser.put("email", stringEmail);
                                    registerUser.put("password", hashedPassword);
                                    registerUser.put("profilePic", "default");
                                    registerUser.put("phoneNumber", prefixPhoneNumber);

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
                                            showRegisterFailedDialog();

                                            Log.e(TAG, String.valueOf(task1.getException()));

                                            StaticDataPasser.storeHashedPassword = null;

                                            nextBtn.setVisibility(View.VISIBLE);
                                            progressBarLayout.setVisibility(View.GONE);

                                        }
                                    });

                                } else {
                                    showRegisterFailedDialog();

                                    Log.e(TAG, "currentUser is null");
                                }
                            } else {
                                showRegisterFailedDialog();

                                StaticDataPasser.storeHashedPassword = null;

                                progressBarLayout.setVisibility(View.GONE);
                                nextBtn.setVisibility(View.VISIBLE);

                                Log.e(TAG, String.valueOf(task.getException()));
                            }
                        });
                        break;
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (shouldExit) {
            super.onBackPressed(); // Exit the app
        } else {
            // Show an exit confirmation dialog
            showCancelRegisterDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeRegisterFailedDialog();
        closePleaseWaitDialog();
        closeAgeInfoDialog();
        closeCancelRegisterDialog();
        closeNoInternetDialog();
        closeUserTypeImageDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }

        closeRegisterFailedDialog();
        closePleaseWaitDialog();
        closeAgeInfoDialog();
        closeCancelRegisterDialog();
        closeNoInternetDialog();
        closeUserTypeImageDialog();
    }

    private void showAgeInfoDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.age_required_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            closeAgeInfoDialog();
        });

        builder.setView(dialogView);

        ageInfoDialog = builder.create();
        ageInfoDialog.show();
    }

    private void closeAgeInfoDialog() {
        if (ageInfoDialog != null && ageInfoDialog.isShowing()) {
            ageInfoDialog.dismiss();
        }
    }

    private void showUserTypeImageDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_you_are_registering_as, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);
        TextView registerAsTextView = dialogView.findViewById(R.id.registerAsTextView);

        registerAsTextView.setText(StaticDataPasser.storeRegisterData);

        okBtn.setOnClickListener(v -> {
            closeUserTypeImageDialog();
        });

        builder.setView(dialogView);

        userTypeImageDialog = builder.create();
        userTypeImageDialog.show();
    }

    private void closeUserTypeImageDialog() {
        if (userTypeImageDialog != null && userTypeImageDialog.isShowing()) {
            userTypeImageDialog.dismiss();
        }
    }

    private void showCancelRegisterDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.cancel_register_dialog, null);

        Button yesBtn = dialogView.findViewById(R.id.yesBtn);
        Button noBtn = dialogView.findViewById(R.id.noBtn);

        yesBtn.setOnClickListener(v -> {
            auth.signOut();

            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

        noBtn.setOnClickListener(v -> {
            closeCancelRegisterDialog();
        });

        builder.setView(dialogView);

        cancelRegisterDialog = builder.create();
        cancelRegisterDialog.show();
    }

    private void closeCancelRegisterDialog() {
        if (cancelRegisterDialog != null && cancelRegisterDialog.isShowing()) {
            cancelRegisterDialog.dismiss();
        }
    }

    private void showPleaseWaitDialog() {
        builder = new AlertDialog.Builder(this);
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

    private void showNoInternetDialog() {
        builder = new AlertDialog.Builder(this);
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

            boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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
        registerReceiver(networkChangeReceiver, intentFilter);

        // Initial network status check
        boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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

    private void showRegisterFailedDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.register_failed_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            closeRegisterFailedDialog();
        });

        builder.setView(dialogView);

        registerFailedDialog = builder.create();
        registerFailedDialog.show();

    }

    private void closeRegisterFailedDialog() {
        if (registerFailedDialog != null && registerFailedDialog.isShowing()) {
            registerFailedDialog.dismiss();
        }
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

    //register using google
    private void fireBaseAuthWithGoogle(String idToken) {
        showPleaseWaitDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        if (googleSignInAccount != null) {
            String googleEmail = googleSignInAccount.getEmail();
            String googleProfilePic = String.valueOf(googleSignInAccount.getPhotoUrl());

            switch (StaticDataPasser.storeRegisterData) {
                case "Driver":

                    userTypeImageBtn.invalidate();
                    userTypeImageBtn.setImageResource(R.drawable.driver);

                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            currentUser = auth.getCurrentUser();

                            if (currentUser != null) {
                                userID = currentUser.getUid();
                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("driver").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("driverID", userID);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("email", googleEmail);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);

                                databaseReference.setValue(registerUser).addOnCompleteListener(task1 -> {

                                    if (task1.isSuccessful()) {

                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterDriver.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                        Log.d(TAG, "googleSignInWithCredential:success");

                                    } else {
                                        showRegisterFailedDialog();
                                        closePleaseWaitDialog();

                                        Log.e(TAG, String.valueOf(task1.getException()));

                                    }
                                });
                            } else {
                                showRegisterFailedDialog();

                                Log.e(TAG, "currentUser is null");
                            }


                        } else {
                            showRegisterFailedDialog();
                            closePleaseWaitDialog();

                            auth.signOut();

                            intent = new Intent(this, Login.class);
                            startActivity(intent);
                            finish();

                            Log.e(TAG, String.valueOf(task.getException()));
                        }
                    });
                    break;

                case "Persons with Disability (PWD)":

                    userTypeImageBtn.invalidate();
                    userTypeImageBtn.setImageResource(R.drawable.pwd);

                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            currentUser = auth.getCurrentUser();

                            if (currentUser != null) {

                                userID = currentUser.getUid();

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("pwd").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("email", googleEmail);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);

                                databaseReference.setValue(registerUser).addOnCompleteListener(task12 -> {

                                    if (task12.isSuccessful()) {
                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterPWD.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                        Log.d(TAG, "googleSignInWithCredential:success");
                                    } else {
                                        showRegisterFailedDialog();
                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        Log.e(TAG, String.valueOf(task12.getException()));
                                    }
                                });

                            } else {
                                showRegisterFailedDialog();

                                Log.e(TAG, "currentUser is null");
                            }


                        } else {
                            showRegisterFailedDialog();
                            closePleaseWaitDialog();

                            StaticDataPasser.storeHashedPassword = null;

                            Log.e(TAG, String.valueOf(task.getException()));
                        }
                    });
                    break;

                case "Senior Citizen":
                    userTypeImageBtn.invalidate();
                    userTypeImageBtn.setImageResource(R.drawable.senior_64);

                    auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {

                            currentUser = auth.getCurrentUser();

                            if (currentUser != null) {
                                userID = currentUser.getUid();

                                databaseReference = FirebaseDatabase.getInstance().getReference("users").child("senior").child(userID);

                                Map<String, Object> registerUser = new HashMap<>();
                                registerUser.put("userID", userID);
                                registerUser.put("userType", StaticDataPasser.storeRegisterData);
                                registerUser.put("email", googleEmail);
                                registerUser.put("password", StaticDataPasser.storeHashedPassword);
                                registerUser.put("profilePic", googleProfilePic);

                                databaseReference.setValue(registerUser).addOnCompleteListener(task13 -> {

                                    if (task13.isSuccessful()) {

                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        intent = new Intent(Register.this, RegisterSenior.class);
                                        intent.putExtra("registerData", StaticDataPasser.storeRegisterData);
                                        startActivity(intent);
                                        finish();

                                        Log.d(TAG, "googleSignInWithCredential:success");

                                    } else {
                                        showRegisterFailedDialog();
                                        closePleaseWaitDialog();

                                        StaticDataPasser.storeHashedPassword = null;

                                        Log.e(TAG, String.valueOf(task13.getException()));
                                    }
                                });

                            } else {
                                showRegisterFailedDialog();

                                Log.e(TAG, "currentUser is null");
                            }

                        } else {
                            showRegisterFailedDialog();
                            closePleaseWaitDialog();

                            StaticDataPasser.storeHashedPassword = null;

                            Log.e(TAG, String.valueOf(task.getException()));

                        }
                    });
                    break;
            }
        } else {
            showRegisterFailedDialog();
            closePleaseWaitDialog();

            Log.e(TAG, "googleSignInWithCredential:failed");
            Log.e(TAG, "googleSignInWithCredential:googleUser is null");
        }
    }

}