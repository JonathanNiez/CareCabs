package com.capstone.carecabs.Utility;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.capstone.carecabs.R;

import org.checkerframework.checker.nullness.qual.NonNull;

public class CustomDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog dialogBookingInfo;
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking_info, null);


		builder.setView(dialogView);

		return dialogBookingInfo = builder.create();
	}
}
