package com.capstone.carecabs;

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

import java.util.List;

public class Login extends AppCompatActivity {

    private TextView registerTextView, loginUsingTextView;
    private Button loginBtn;
    private ImageButton googleImgBtn;
    private EditText email, password;
    private LinearLayout progressBarLayout, googleSignInLayout;
    private Intent intent;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private GoogleSignInOptions googleSignInOptions;
    private GoogleSignInAccount googleSignInAccount;
    private GoogleSignInClient googleSignInClient;
    private String TAG = "Login";
    private boolean shouldExit = false;
    private static final int RC_SIGN_IN = 69;
    private AlertDialog noInternetDialog, emailDialog,
            emailNotRegisteredDialog, incorrectEmailOrPasswordDialog,
            pleaseWaitDialog, registerUsingDialog, loginFailedDialog,
            exitAppDialog;
    private NetworkChangeReceiver networkChangeReceiver;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeNetworkChecker();

        auth = FirebaseAuth.getInstance();
        FirebaseApp.initializeApp(this);

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        registerTextView = findViewById(R.id.registerTextView);
        loginBtn = findViewById(R.id.loginBtn);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginUsingTextView = findViewById(R.id.loginUsingTextView);
        googleImgBtn = findViewById(R.id.googleImgBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        googleSignInLayout = findViewById(R.id.googleSignInLayout);

        registerTextView.setOnClickListener(v -> {
            showRegisterUsingDialog();
        });

        googleImgBtn.setOnClickListener(v -> {
            showPleaseWaitDialog();

            intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
        });

        loginBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);

            final String stringEmail = email.getText().toString().trim();
            final String stringPassword = password.getText().toString();

            if (stringEmail.isEmpty()) {
                email.setError("Please Enter your Email");

                progressBarLayout.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);

            } else if (stringPassword.isEmpty()) {
                password.setError("Please Enter your Password");
                progressBarLayout.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);

            } else {
                showPleaseWaitDialog();
                googleSignInLayout.setVisibility(View.GONE);

                auth.fetchSignInMethodsForEmail(stringEmail)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                progressBarLayout.setVisibility(View.GONE);
                                loginBtn.setVisibility(View.VISIBLE);

                                List<String> signInMethods = task.getResult().getSignInMethods();
                                if (signInMethods != null && !signInMethods.isEmpty()) {
                                    auth.signInWithEmailAndPassword(stringEmail, stringPassword)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    closePleaseWaitDialog();

                                                    intent = new Intent(Login.this, LoggingIn.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                    finish();

                                                    Log.i(TAG, "Login Success");
                                                } else {
                                                    closePleaseWaitDialog();
                                                    showLoginFailedDialog();

                                                    progressBarLayout.setVisibility(View.GONE);
                                                    loginBtn.setVisibility(View.VISIBLE);

                                                    Log.e(TAG, String.valueOf(task1.getException()));
                                                }
                                            });

                                } else {
                                    showIncorrectEmailOrPasswordDialog();
                                    closePleaseWaitDialog();

                                    Log.e(TAG, String.valueOf(task.getException()));
                                }

                            } else {
                                showLoginFailedDialog();
                                closePleaseWaitDialog();

                                Log.e(TAG, String.valueOf(task.getException()));
                            }

                        });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }

        closeExitConfirmationDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeExitConfirmationDialog();
    }

    @Override
    public void onBackPressed() {

        if (shouldExit) {
            super.onBackPressed(); // Exit the app
        } else {
            // Show an exit confirmation dialog
            showExitConfirmationDialog();
        }

    }

    private void showExitConfirmationDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.exit_app_dialog, null);

        Button yesBtn = dialogView.findViewById(R.id.yesBtn);
        Button noBtn = dialogView.findViewById(R.id.noBtn);

        yesBtn.setOnClickListener(v -> {
            finish();
        });

        noBtn.setOnClickListener(v -> {
            if (exitAppDialog != null && exitAppDialog.isShowing()) {
                exitAppDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        exitAppDialog = builder.create();
        exitAppDialog.show();
    }

    private void closeExitConfirmationDialog() {
        if (exitAppDialog != null && exitAppDialog.isShowing()) {
            exitAppDialog.dismiss();
        }
    }

    private void showEmailNotRegisterDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.email_not_registered_dialog, null);

        Button noBtn = dialogView.findViewById(R.id.noBtn);
        Button yesBtn = dialogView.findViewById(R.id.yesBtn);

        noBtn.setOnClickListener(v -> {
            if (emailNotRegisteredDialog != null && emailNotRegisteredDialog.isShowing()) {
                emailNotRegisteredDialog.dismiss();
            }
        });

        yesBtn.setOnClickListener(v -> {
            if (emailNotRegisteredDialog != null && emailNotRegisteredDialog.isShowing()) {
                emailNotRegisteredDialog.dismiss();
            }

            intent = new Intent(this, Register.class);
            startActivity(intent);
            finish();
        });

        builder.setView(dialogView);

        emailNotRegisteredDialog = builder.create();
        emailNotRegisteredDialog.show();
    }

    private void showIncorrectEmailOrPasswordDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.incorrect_email_or_password_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (incorrectEmailOrPasswordDialog != null && incorrectEmailOrPasswordDialog.isShowing()) {
                incorrectEmailOrPasswordDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        incorrectEmailOrPasswordDialog = builder.create();
        incorrectEmailOrPasswordDialog.show();
    }

    private void showEmailAlreadyRegisteredDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.email_is_already_registered_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            emailDialog.dismiss();
        });

        builder.setView(dialogView);

        emailDialog = builder.create();
        emailDialog.show();
    }

    private void showRegisterUsingDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.register_using_dialog, null);

        ImageButton googleImgBtn = dialogView.findViewById(R.id.googleImgBtn);
        ImageButton emailImgBtn = dialogView.findViewById(R.id.emailImgBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        googleImgBtn.setOnClickListener(v -> {
            intent = new Intent(Login.this, RegisterUserType.class);
            intent.putExtra("registerType", "googleRegister");
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

            closeRegisterUsingDialog();
        });

        emailImgBtn.setOnClickListener(v -> {
            intent = new Intent(Login.this, RegisterUserType.class);
            intent.putExtra("registerType", "emailRegister");
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

            closeRegisterUsingDialog();
        });

        cancelBtn.setOnClickListener(v -> {
            closeRegisterUsingDialog();
        });

        builder.setView(dialogView);

        registerUsingDialog = builder.create();
        registerUsingDialog.show();

    }

    private void closeRegisterUsingDialog() {
        if (registerUsingDialog != null && registerUsingDialog.isShowing()) {
            registerUsingDialog.dismiss();
        }
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

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        String getGoogleEmail = acct.getEmail();
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(getGoogleEmail)
                .addOnCompleteListener(task -> {
                    SignInMethodQueryResult result = task.getResult();
                    if (!result.getSignInMethods().isEmpty()) {
                        auth.signInWithCredential(credential).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                intent = new Intent(Login.this, LoggingIn.class);
                                startActivity(intent);
                                finish();

                                closePleaseWaitDialog();
                            } else {
                                closePleaseWaitDialog();
                                showLoginFailedDialog();
                            }

                        });
                    } else {
                        closePleaseWaitDialog();

                        intent = new Intent(this, RegisterUserType.class);
                        startActivity(intent);
                        finish();
                    }
                });
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

    private void showLoginFailedDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.login_failed_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            closeLoginFailedDialog();
        });

        builder.setView(dialogView);

        loginFailedDialog = builder.create();
        loginFailedDialog.show();

    }

    private void closeLoginFailedDialog() {
        if (loginFailedDialog != null && loginFailedDialog.isShowing()) {
            loginFailedDialog.dismiss();
        }
    }

}