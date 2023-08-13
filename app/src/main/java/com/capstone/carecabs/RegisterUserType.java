package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

        if (getRegisterType.equals("googleRegister")){
            googleTextView.setVisibility(View.VISIBLE);

            registerType = getRegisterType;
        }else{
            registerType = "emailRegister";
        }

        loginHereBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });

        driverImgBtn.setOnClickListener(v -> {
            intent = new Intent(this, Register.class);
            registerData = "driver";
            intent.putExtra("registerData", registerData);
            intent.putExtra("registerType", registerType);
            startActivity(intent);
            finish();

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

    private void showUserTypeDialog() {

        final Dialog customDialog = new Dialog(this);
        customDialog.setContentView(R.layout.user_type_dialog);

        ImageButton seniorImgBtn = customDialog.findViewById(R.id.seniorImgBtn);
        ImageButton pwdImgBtn = customDialog.findViewById(R.id.pwdImgBtn);
        ImageButton imgBackBtn = customDialog.findViewById(R.id.imgBackBtn);

        seniorImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(RegisterUserType.this, Register.class);
                registerData = "senior";
                intent.putExtra("registerData", registerData);
                intent.putExtra("registerType", registerType);
                startActivity(intent);
                finish();

                customDialog.dismiss();
            }
        });

        pwdImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(RegisterUserType.this, Register.class);
                registerData = "pwd";
                intent.putExtra("registerData", registerData);
                intent.putExtra("registerType", registerType);
                startActivity(intent);
                finish();

                customDialog.dismiss();

            }
        });

        imgBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
            }
        });

        customDialog.show();
    }
}