package com.capstone.carecabs.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentCurrentTripBinding;

public class CurrentTripFragment extends Fragment {
	private final String TAG = "CurrentTripFragment";
	private FragmentCurrentTripBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCurrentTripBinding.inflate(inflater, container, false);
		View view = binding.getRoot();



		return view;
	}
}