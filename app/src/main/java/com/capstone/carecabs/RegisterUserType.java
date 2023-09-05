package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterUserType extends AppCompatActivity {

    private ImageButton passengerImgBtn, driverImgBtn;
    private LinearLayout googleRegisterLayout;
    private Button cancelBtn;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private GoogleSignInOptions googleSignInOptions;
    private GoogleSignInAccount googleSignInAccount;
    private GoogleSignInClient googleSignInClient;
    private Intent intent;
    private String registerData, registerType;
    private AlertDialog.Builder builder;
    private AlertDialog userTypeDialog, emailAlreadyRegisteredDialog,
            noInternetDialog, cancelRegisterDialog;
    private static final int RC_SIGN_IN = 69;
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean shouldExit = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user_type);

        initializeNetworkChecker();

        intent = getIntent();
        //From Login
        String getRegisterType = intent.getStringExtra("registerType");

        auth = FirebaseAuth.getInstance();
        FirebaseApp.initializeApp(this);

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        passengerImgBtn = findViewById(R.id.passengerImgBtn);
        driverImgBtn = findViewById(R.id.driverImgBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        googleRegisterLayout = findViewById(R.id.googleRegisterLayout);

        if (getRegisterType != null) {
            if (getRegisterType.equals("googleRegister")) {
                googleRegisterLayout.setVisibility(View.VISIBLE);

                registerType = "googleRegister";
            } else {
                registerType = "emailRegister";
            }
        } else {
            return;
        }

        cancelBtn.setOnClickListener(v -> {
            showCancelRegisterDialog();
        });


        driverImgBtn.setOnClickListener(v -> {
            intent = new Intent(this, Register.class);
            registerData = "Driver";
            intent.putExtra("registerData", registerData);
            intent.putExtra("registerType", registerType);
            startActivity(intent);

        });

        passengerImgBtn.setOnClickListener(v -> {
            showUserTypeDialog();
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        closeCancelRegisterDialog();
        closeUserTypeDialog();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }

        closeCancelRegisterDialog();
        closeUserTypeDialog();
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

    private void showCancelRegisterDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.cancel_register_dialog, null);

        Button yesBtn = dialogView.findViewById(R.id.yesBtn);
        Button noBtn = dialogView.findViewById(R.id.noBtn);

        yesBtn.setOnClickListener(v -> {
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

    private void closeUserTypeDialog() {
        if (userTypeDialog != null && userTypeDialog.isShowing()) {
            userTypeDialog.dismiss();
        }
    }

    private void showUserTypeDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.user_type_dialog, null);

        ImageButton seniorImgBtn = dialogView.findViewById(R.id.seniorImgBtn);
        ImageButton pwdImgBtn = dialogView.findViewById(R.id.pwdImgBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        seniorImgBtn.setOnClickListener(v -> {
            intent = new Intent(RegisterUserType.this, Register.class);
            registerData = "Senior Citizen";
            intent.putExtra("registerData", registerData);
            intent.putExtra("registerType", registerType);
            startActivity(intent);
            finish();

            closeUserTypeDialog();
        });

        pwdImgBtn.setOnClickListener(v -> {
            intent = new Intent(RegisterUserType.this, Register.class);
            registerData = "Persons with Disability (PWD)";
            intent.putExtra("registerData", registerData);
            intent.putExtra("registerType", registerType);
            startActivity(intent);
            finish();

            closeUserTypeDialog();
        });

        cancelBtn.setOnClickListener(v -> {
            closeUserTypeDialog();
        });

        builder.setView(dialogView);

        userTypeDialog = builder.create();
        userTypeDialog.show();
    }

    private void showEmailAlreadyRegisteredDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.email_is_already_registered_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (emailAlreadyRegisteredDialog != null && emailAlreadyRegisteredDialog.isShowing()) {
                emailAlreadyRegisteredDialog.dismiss();
            }
            intent = new Intent(RegisterUserType.this, Register.class);
            startActivity(intent);
            finish();
        });

        builder.setView(dialogView);

        emailAlreadyRegisteredDialog = builder.create();
        emailAlreadyRegisteredDialog.show();
    }

    private void showNoInternetDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.no_internet_dialog, null);

        Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

        tryAgainBtn.setOnClickListener(v -> {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();

                boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
                updateConnectionStatus(isConnected);
            }
        });

        builder.setView(dialogView);

        noInternetDialog = builder.create();
        noInternetDialog.show();
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

}