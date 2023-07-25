package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;

import com.capstone.carecabs.Fragments.AccountFragment;
import com.capstone.carecabs.Fragments.HomeFragment;
import com.capstone.carecabs.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showFragment(new HomeFragment());

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
    private void showFragment(Fragment fragment) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.commit();
    }
}