package com.capstone.carecabs.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.capstone.carecabs.R;

public class AppSettingsFragment extends Fragment {


    private ImageButton imgBackBtn;
    private Button changeFontSizeBtn;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        changeFontSizeBtn = view.findViewById(R.id.changeFontSizeBtn);

        imgBackBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        changeFontSizeBtn.setOnClickListener(v -> {
            goToChangeFontSizeFragment();
        });

        return view;
    }
    private void goToChangeFontSizeFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new ChangeFontSizeFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void backToAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}