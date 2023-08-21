package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Objects;

public class RegisterUserType extends AppCompatActivity {

    private ImageButton imgBackBtn, passengerImgBtn, driverImgBtn;
    private TextView googleTextView;
    private Button loginHereBtn;
    private Intent intent;
    private String registerData;
    private String registerType;
    private AlertDialog userTypeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user_type);

        intent = getIntent();
        String getRegisterType = intent.getStringExtra("registerType");

        imgBackBtn = findViewById(R.id.imgBackBtn);
        passengerImgBtn = findViewById(R.id.passengerImgBtn);
        driverImgBtn = findViewById(R.id.driverImgBtn);
        loginHereBtn = findViewById(R.id.loginHereBtn);
        googleTextView = findViewById(R.id.googleTextView);


        if (getRegisterType != null) {
            if (getRegisterType.equals("googleRegister")) {
                googleTextView.setVisibility(View.VISIBLE);

                registerType = getRegisterType;
            } else {
                registerType = "emailRegister";
            }
        } else {
            return;
        }


        loginHereBtn.setOnClickListener(v -> {
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

        imgBackBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

    }

    private void closeUserTypeDialog() {
        if (userTypeDialog != null && userTypeDialog.isShowing()) {
            userTypeDialog.dismiss();
        }
    }

    private void showUserTypeDialog() {
        AlertDialog.Builder  builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.user_type_dialog, null);

        ImageButton seniorImgBtn = dialogView.findViewById(R.id.seniorImgBtn);
        ImageButton pwdImgBtn = dialogView.findViewById(R.id.pwdImgBtn);
        ImageButton imgBackBtn = dialogView.findViewById(R.id.imgBackBtn);

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

            userTypeDialog.dismiss();

        });

        imgBackBtn.setOnClickListener(v -> {
            closeUserTypeDialog();
        });

        builder.setView(dialogView);

        userTypeDialog = builder.create();
        userTypeDialog.show();
    }
}