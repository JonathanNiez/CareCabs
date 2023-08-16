package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class ChangePassword extends AppCompatActivity {

    private Button changePasswordBtn;
    private EditText password, confirmPassword;
    private ImageButton imgBackBtn;
    private LinearLayout progressBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);


        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        changePasswordBtn = findViewById(R.id.changePasswordBtn);
        progressBarLayout = findViewById(R.id.progressBarLayout);
        imgBackBtn = findViewById(R.id.imgBackBtn);

        imgBackBtn.setOnClickListener(v -> {
            finish();
        });

        changePasswordBtn.setOnClickListener(v -> {
            progressBarLayout.setVisibility(View.VISIBLE);
            changePasswordBtn.setVisibility(View.GONE);

            String stringPassword = password.getText().toString();
            String stringConfirmPassword = confirmPassword.getText().toString();

            if (stringPassword.isEmpty() || stringConfirmPassword.isEmpty()){
                password.setError("Please Enter your Password you want to change");
            }else{

            }
        });

    }
}