package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Login extends AppCompatActivity {

    private TextView registerTextView;
    private Button loginBtn;
    private EditText username, password;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        registerTextView = findViewById(R.id.registerTextView);
        loginBtn = findViewById(R.id.loginBtn);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        registerTextView.setOnClickListener(v -> {
             intent = new Intent(this, RegisterUserType.class);
             startActivity(intent);
             finish();
        });

        loginBtn.setOnClickListener(v -> {
            final String stringUsername = username.getText().toString().trim();
            final String stringPassword = password.getText().toString().trim();

            if (stringUsername.isEmpty()){
                username.setError("Please Enter your Username");
            } else if (stringPassword.isEmpty()) {
                password.setError("Please Enter your Password");
            }
            else{

            }
        });
    }
}