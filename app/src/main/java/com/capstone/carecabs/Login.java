package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    private String userID;
    private static final int RC_SIGN_IN = 69;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

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

        registerTextView.setOnClickListener(v -> {
            intent = new Intent(this, RegisterUserType.class);
            startActivity(intent);
            finish();
        });

        googleImgBtn.setOnClickListener(v -> {
            googleImgBtn.setVisibility(View.GONE);
            progressBarLayout.setVisibility(View.VISIBLE);
            loginUsingTextView.setVisibility(View.GONE);
            loginBtn.setEnabled(false);

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
                auth.signInWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressBarLayout.setVisibility(View.GONE);
                            loginBtn.setVisibility(View.VISIBLE);

                            intent = new Intent(Login.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                            Log.i(TAG, "Login Success");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());

                        progressBarLayout.setVisibility(View.GONE);
                        loginBtn.setVisibility(View.VISIBLE);

                    }
                });
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

                googleImgBtn.setVisibility(View.VISIBLE);
                progressBarLayout.setVisibility(View.GONE);
                loginUsingTextView.setVisibility(View.VISIBLE);
                loginBtn.setEnabled(true);

            }
        }

    }
    private void fireBaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        if (googleSignInAccount != null) {

            auth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithCredential:success");
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
                                                googleImgBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);
                                                loginUsingTextView.setVisibility(View.VISIBLE);
                                                loginBtn.setEnabled(true);

                                                intent = new Intent(Login.this, MainActivity.class);
//                                                intent.putExtra("registerData", getRegisterData);
                                                startActivity(intent);
                                                finish();

                                            } else {
                                                Log.e(TAG, String.valueOf(task.getException()));

                                                googleImgBtn.setVisibility(View.VISIBLE);
                                                progressBarLayout.setVisibility(View.GONE);
                                                loginUsingTextView.setVisibility(View.VISIBLE);
                                                loginBtn.setEnabled(true);
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressBarLayout.setVisibility(View.GONE);
                                            googleImgBtn.setVisibility(View.VISIBLE);
                                            loginUsingTextView.setVisibility(View.VISIBLE);
                                            loginBtn.setEnabled(true);

                                            Log.e(TAG, e.getMessage());
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(Login.this, "Login failed", Toast.LENGTH_SHORT).show();

                                googleImgBtn.setVisibility(View.VISIBLE);
                                progressBarLayout.setVisibility(View.GONE);
                                loginUsingTextView.setVisibility(View.VISIBLE);
                                loginBtn.setEnabled(true);

                            }
                        }
                    });
        } else {
            Log.d(TAG, "signInWithCredential:failed");

            googleImgBtn.setVisibility(View.VISIBLE);
            progressBarLayout.setVisibility(View.GONE);
            loginUsingTextView.setVisibility(View.VISIBLE);
            loginBtn.setEnabled(true);
        }
    }

}