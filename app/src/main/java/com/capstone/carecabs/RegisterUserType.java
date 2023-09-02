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
import android.widget.Toast;

import com.capstone.carecabs.Utility.NetworkChangeReceiver;
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
import com.google.firebase.auth.SignInMethodQueryResult;

public class RegisterUserType extends AppCompatActivity {

    private ImageButton imgBackBtn, passengerImgBtn, driverImgBtn;
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
    private AlertDialog userTypeDialog, emailAlreadyRegisteredDialog, noInternetDialog;
    private static final int RC_SIGN_IN = 69;
    private NetworkChangeReceiver networkChangeReceiver;


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

        imgBackBtn = findViewById(R.id.imgBackBtn);
        passengerImgBtn = findViewById(R.id.passengerImgBtn);
        driverImgBtn = findViewById(R.id.driverImgBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        googleRegisterLayout = findViewById(R.id.googleRegisterLayout);

        if (getRegisterType != null) {
            if (getRegisterType.equals("googleRegister")) {
                googleRegisterLayout.setVisibility(View.VISIBLE);

                intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);
            } else {
                registerType = "emailRegister";
            }
        } else {
            return;
        }

        cancelBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
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

    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private void closeUserTypeDialog() {
        if (userTypeDialog != null && userTypeDialog.isShowing()) {
            userTypeDialog.dismiss();
        }
    }

    private void showUserTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

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

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        String getGoogleEmail = acct.getEmail();
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(getGoogleEmail)
                .addOnCompleteListener(task -> {
                    SignInMethodQueryResult result = task.getResult();
                    if (result.getSignInMethods().size() > 0) {

                        showEmailAlreadyRegisteredDialog();

//                        auth.signInWithCredential(credential).addOnCompleteListener(task1 -> {
//                            if (task1.isSuccessful()) {
//                                intent = new Intent(RegisterUserType.this, LoggingIn.class);
//                                startActivity(intent);
//                                finish();
//                            } else {
//                                Toast.makeText(RegisterUserType.this, "Failed to Login", Toast.LENGTH_LONG).show();
//
//                            }
//                        }).addOnFailureListener(e -> {
//                            Toast.makeText(RegisterUserType.this, "Failed to Login", Toast.LENGTH_LONG).show();
//                            e.printStackTrace();
//                        });

                    } else {
                        showEmailAlreadyRegisteredDialog();
                    }
                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to Login", Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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