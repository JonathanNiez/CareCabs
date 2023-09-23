package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.databinding.ActivityGetStartedBinding;
import com.google.firebase.FirebaseApp;

public class GetStartedActivity extends AppCompatActivity {
    private Intent intent;
    private AlertDialog exitAppDialog;
    private AlertDialog.Builder builder;
    private ActivityGetStartedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseMain.getAuth();
        FirebaseApp.initializeApp(this);

        if (FirebaseMain.getUser() != null) {
            intent = new Intent(this, MainActivity.class);
//            overridePendingTransition(R.anim.popup_enter, R.anim.popup_exit);
            startActivity(intent);
            finish();
        }

        binding.getStartedBtn.setOnClickListener(v -> {
            intent = new Intent(this, LoginOrRegisterActivity.class);
//            overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeExitConfirmationDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closeExitConfirmationDialog();
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
            closeExitConfirmationDialog();
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
}