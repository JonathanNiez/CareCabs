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
import com.capstone.carecabs.databinding.FragmentAppSettingsBinding;

public class AppSettingsFragment extends Fragment {

	private FragmentAppSettingsBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentAppSettingsBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.imgBackBtn.setOnClickListener(v -> backToAccountFragment());

		binding.changeFontSizeBtn.setOnClickListener(v -> goToChangeFontSizeFragment());

		binding.languageBtn.setOnClickListener(v -> goToChangeLanguageFragment());

		return view;
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void goToChangeFontSizeFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new ChangeFontSizeFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToChangeLanguageFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new ChangeLanguageFragment());
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