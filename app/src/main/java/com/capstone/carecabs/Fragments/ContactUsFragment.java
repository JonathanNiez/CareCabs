package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.capstone.carecabs.FeedbackActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentContactUsBinding;

import java.util.Objects;

public class ContactUsFragment extends Fragment {
    private FragmentContactUsBinding binding;
    private Context context;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentContactUsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        context = getContext();

        binding.imgBackBtn.setOnClickListener(v -> backToAccountFragment());

        binding.submitFeedBackBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, FeedbackActivity.class);
            startActivity(intent);
            Objects.requireNonNull(getActivity()).finish();
        });

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