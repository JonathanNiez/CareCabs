package com.capstone.carecabs.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentAccountBinding;

public class AboutFragment extends Fragment {
    private FragmentAccountBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.imgBackBtn.setOnClickListener(v -> backToAccountFragment());

        return view;
    }

    public void onBackPressed() {
        backToAccountFragment();
    }

    private void backToAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}