package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.capstone.carecabs.databinding.ActivityGetStartedBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GetStarted extends AppCompatActivity {

    private FirebaseUser currentUser;
    private FirebaseAuth auth;
    private Intent intent;
    private AlertDialog exitAppDialog;
    private AlertDialog.Builder builder;
    private ActivityGetStartedBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        FirebaseApp.initializeApp(this);

        if (currentUser != null) {
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding.getStartedBtn.setOnClickListener(v -> {
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (exitAppDialog != null && exitAppDialog.isShowing()) {
            exitAppDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (exitAppDialog != null && exitAppDialog.isShowing()) {
            exitAppDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        showExitConfirmationDialog();
    }


    private void showExitConfirmationDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_app, null);

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
}