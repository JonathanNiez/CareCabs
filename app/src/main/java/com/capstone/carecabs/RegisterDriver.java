package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterDriver extends AppCompatActivity {

    private Button doneBtn;
    private EditText firstname, lastname, age, sex;
    private LinearLayout progressBarLayout;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "RegisterDriver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);

        doneBtn = findViewById(R.id.doneBtn);
        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        age = findViewById(R.id.age);
        sex = findViewById(R.id.sex);
        progressBarLayout = findViewById(R.id.progressBarLayout);

        auth = FirebaseAuth.getInstance();

        doneBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            doneBtn.setVisibility(View.GONE);

            String stringFirstname = firstname.getText().toString().trim();
            String stringLastname = lastname.getText().toString().trim();
            String stringSex = sex.getText().toString().trim();
            String stringAge = age.getText().toString().trim();

            if (stringFirstname.isEmpty() || stringLastname.isEmpty() || stringAge.isEmpty() || stringSex.isEmpty()){
                Toast.makeText(this, "Please enter your Info", Toast.LENGTH_LONG).show();
                progressBarLayout.setVisibility(View.GONE);
                doneBtn.setVisibility(View.VISIBLE);

            }else{
                currentUser = auth.getCurrentUser();
                userID = currentUser.getUid();

                databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID);

                Map<String, Object> registerUser = new HashMap<>();
                registerUser.put("firstname", stringFirstname);
                registerUser.put("lastname", stringLastname);
                registerUser.put("age", stringAge);
                registerUser.put("sex", stringSex);

                databaseReference.updateChildren(registerUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            progressBarLayout.setVisibility(View.GONE);
                            doneBtn.setVisibility(View.VISIBLE);

                            Intent intent = new Intent(RegisterDriver.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBarLayout.setVisibility(View.GONE);
                        doneBtn.setVisibility(View.VISIBLE);

                        Log.e(TAG, e.getMessage());
                    }
                });
            }



        });

    }
}