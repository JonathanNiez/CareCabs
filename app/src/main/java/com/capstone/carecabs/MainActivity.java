package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.capstone.carecabs.Fragments.AccountFragment;
import com.capstone.carecabs.Fragments.EditAccountFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Intent intent;
    private DoubleTapBackHandler doubleTapBackHandler;
    private AlertDialog exitAppDialog;
    private AlertDialog.Builder builder;
    private String TAG = "MainActivity";
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int STORAGE_PERMISSION_REQUEST = 102;

    private EditAccountFragment editAccountFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showFragment(new HomeFragment());

        doubleTapBackHandler = new DoubleTapBackHandler();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);
        bottomNavigationView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.home) {
                showFragment(new HomeFragment());

            } else if (item.getItemId() == R.id.account) {
                showFragment(new AccountFragment());

            } else if (item.getItemId() == R.id.map) {
                intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleTapBackHandler.handleDoubleTap()) {
            super.onBackPressed();
        } else {
            showExitConfirmationDialog();
        }
    }

    public class DoubleTapBackHandler {
        private boolean doubleTapped = false;
        private final Handler handler = new Handler();

        public boolean handleDoubleTap() {
            if (doubleTapped) {
                return true;
            }

            doubleTapped = true;
            handler.postDelayed(() -> doubleTapped = false, 2000); // 2 seconds
            return false;
        }
    }

    private void showExitConfirmationDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.exit_app_dialog, null);

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

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.commit();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

        if (editAccountFragment != null) {
            editAccountFragment.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        editAccountFragment = (EditAccountFragment) getSupportFragmentManager().findFragmentByTag("editAccountFragment");

        if (editAccountFragment != null) {
            editAccountFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

}