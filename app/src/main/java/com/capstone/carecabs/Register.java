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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private String userID;
    private String TAG = "Register";
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        imgBackBtn = findViewById(R.id.imgBackBtn);
        nextBtn = findViewById(R.id.nextBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);

        intent = getIntent();
        String getRegisterData = intent.getStringExtra("registerData");

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

                                    databaseReference = FirebaseDatabase.getInstance().getReference("drivers").child(userID);

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

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);

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

                                                intent = new Intent(Register.this, RegisterPWD.class);
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
                    case "senior":
                        auth.createUserWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    currentUser = auth.getCurrentUser();
                                    userID = currentUser.getUid();

                                    databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);

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

                                                intent = new Intent(Register.this, RegisterSenior.class);
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
                }
            }
        });
    }
}