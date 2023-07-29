package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.capstone.carecabs.Fragments.AccountFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Intent intent;
    private DoubleTapBackHandler doubleTapBackHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showFragment(new HomeFragment());

        doubleTapBackHandler = new DoubleTapBackHandler();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.home) {
                showFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.account) {
                showFragment(new AccountFragment());
            } else if (item.getItemId() == R.id.map) {
                intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }

            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit App?");
        builder.setMessage("Are you sure you want to exit the app?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform any cleanup or other actions before exiting the app
                finish();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.commit();
    }
}