package com.capstone.carecabs.Fragments;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.carecabs.databinding.FragmentCancelBookingModalBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CancelBookingModal extends BottomSheetDialogFragment {
	public static String TAG = "CancelBookingModal";
	private FragmentCancelBookingModalBinding binding;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCancelBookingModalBinding.inflate(inflater, container, false);
		View view = binding.getRoot();



		return view;
	}

	public static CancelBookingModal newInstance() {
		CancelBookingModal fragment = new CancelBookingModal();
//		Bundle args = new Bundle();
//		args.putString(ARG_DATA, data);
//		fragment.setArguments(args);
		return fragment;
	}
}