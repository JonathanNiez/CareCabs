package com.capstone.carecabs.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.capstone.carecabs.databinding.FragmentIdScanInfo2Binding;

public class FragmentIdScanInfo2 extends Fragment {

	private FragmentIdScanInfo2Binding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		binding = FragmentIdScanInfo2Binding.inflate(inflater, container, false);
		return binding.getRoot();

	}
}