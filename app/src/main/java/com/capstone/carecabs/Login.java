package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Login extends AppCompatActivity {

    private TextView registerTextView, loginUsingTextView;
    private Button loginBtn;
    private ImageButton googleImgBtn;
    private EditText email, password;
    private LinearLayout progressBarLayout;
    private Intent intent;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private GoogleSignInOptions googleSignInOptions;
    private GoogleSignInAccount googleSignInAccount;
    private GoogleSignInClient googleSignInClient;
    private String TAG = "Login";
    private static final int RC_SIGN_IN = 69;
    private AlertDialog noInternetDialog, emailDialog;
    private NetworkConnectivityChecker networkConnectivityChecker;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        networkConnectivityChecker = new NetworkConnectivityChecker(this);
        checkInternetConnection();

        auth = FirebaseAuth.getInstance();

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        registerTextView = findViewById(R.id.registerTextView);
        loginBtn = findViewById(R.id.loginBtn);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginUsingTextView = findViewById(R.id.loginUsingTextView);
        googleImgBtn = findViewById(R.id.googleImgBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);

        registerTextView.setOnClickListener(v -> {
            showRegisterUsingDialog();
        });

        googleImgBtn.setOnClickListener(v -> {
            intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
        });

        loginBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);

            final String stringEmail = email.getText().toString().trim();
            final String stringPassword = password.getText().toString().trim();

            if (stringEmail.isEmpty()) {
                email.setError("Please Enter your Email");

                progressBarLayout.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);

            } else if (stringPassword.isEmpty()) {
                password.setError("Please Enter your Password");
                progressBarLayout.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);

            } else {
                checkInternetConnection();
                auth.signInWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressBarLayout.setVisibility(View.GONE);
                        loginBtn.setVisibility(View.VISIBLE);

                        intent = new Intent(Login.this, LoggingIn.class);
                        startActivity(intent);
                        finish();

                        Log.i(TAG, "Login Success");
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage());

                    progressBarLayout.setVisibility(View.GONE);
                    loginBtn.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkConnectivityChecker != null) {
            networkConnectivityChecker.unregisterNetworkCallback();
        }
    }

    private void showRegisterUsingDialog() {

        final Dialog customDialog = new Dialog(this);
        customDialog.setContentView(R.layout.register_using_dialog);

        ImageButton googleImgBtn = customDialog.findViewById(R.id.googleImgBtn);
        ImageButton emailImgBtn = customDialog.findViewById(R.id.emailImgBtn);
        Button cancelBtn = customDialog.findViewById(R.id.cancelBtn);

        googleImgBtn.setOnClickListener(v -> {
            intent = new Intent(Login.this, RegisterUserType.class);
            intent.putExtra("registerType", "googleRegister");
            startActivity(intent);
            finish();

            customDialog.dismiss();
        });

        emailImgBtn.setOnClickListener(v -> {
            intent = new Intent(Login.this, RegisterUserType.class);
            intent.putExtra("registerType", "emailRegister");
            startActivity(intent);
            finish();

            customDialog.dismiss();
        });

        cancelBtn.setOnClickListener(v -> customDialog.dismiss());

        customDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Handle Google Sign-In failure
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(currentUser.getEmail())
                .addOnCompleteListener(task -> {
                    List<String> signInMethods = task.getResult().getSignInMethods();
                    if (signInMethods != null && !signInMethods.isEmpty()) {
                        showEmailAlreadyRegisteredDialog();
                    } else {
                        auth.signInWithCredential(credential).addOnCompleteListener(task1 -> {

                        }).addOnFailureListener(e -> {

                        });
                    }

                });
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

    private void showNoInternetDialog() {

        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.no_internet_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
                checkInternetConnection();
            }
        });

        builder.setView(dialogView);

        noInternetDialog = builder.create();
        noInternetDialog.show();
    }

    private void checkInternetConnection() {
        //TODO mugawas ang dialog biskang naay net
        if (networkConnectivityChecker.isConnected()) {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
            Log.e(TAG, String.valueOf(networkConnectivityChecker.isConnected()));
        } else {
            showNoInternetDialog();
            Log.e(TAG, String.valueOf(networkConnectivityChecker.isConnected()));
        }
    }
}