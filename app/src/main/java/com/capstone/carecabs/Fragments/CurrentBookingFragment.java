package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.CurrentBookingAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Map.MapPassengerActivity;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentCurrentBookingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CurrentBookingFragment extends Fragment {
	private final String TAG = "CurrentBookingFragment";
	private Context context;
	private AlertDialog cancelBookingDialog;
	private CurrentBookingAdapter currentBookingAdapter;
	private FragmentCurrentBookingBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCurrentBookingBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		binding.noCurrentBookingsLayout.setVisibility(View.GONE);

		binding.bookARideBtn.setOnClickListener(v -> {
			Intent intent = new Intent(context, MapPassengerActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		});

		loadCurrentBookingFromDatabase();

		return view;
	}

	private void loadCurrentBookingFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<CurrentBookingModel> currentBookingModelList = new ArrayList<>();
			currentBookingAdapter = new CurrentBookingAdapter(
					context,
					currentBookingModelList,
					currentBookingModel ->
							showCancelBookingDialog(
									currentBookingModel.getBookingID()
									));
			binding.currentBookingsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.currentBookingsRecyclerView.setAdapter(currentBookingAdapter);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@SuppressLint("NotifyDataSetChanged")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						binding.loadingLayout.setVisibility(View.GONE);
						currentBookingModelList.clear();
						boolean hasCurrentBookings = false;
						for (DataSnapshot currentBookingSnapshot : snapshot.getChildren()) {
							CurrentBookingModel currentBookingModel = currentBookingSnapshot.getValue(CurrentBookingModel.class);
							if (currentBookingModel != null &&
									(currentBookingModel.getBookingStatus().equals("Waiting") &&
											currentBookingModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid()) ||
											currentBookingModel.getBookingStatus().equals("Driver on the way")) &&
									currentBookingModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())) {

								currentBookingModelList.add(currentBookingModel);
								hasCurrentBookings = true;
							}
						}
						currentBookingAdapter.notifyDataSetChanged();

						if (hasCurrentBookings) {
							binding.noCurrentBookingsLayout.setVisibility(View.GONE);
						} else {
							binding.noCurrentBookingsLayout.setVisibility(View.VISIBLE);
						}
					} else {
						binding.noCurrentBookingsLayout.setVisibility(View.VISIBLE);
						binding.loadingLayout.setVisibility(View.GONE);
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Log.e(TAG, error.getMessage());
				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private void showCancelBookingDialog(String bookingID) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_booking, null);

		Button closeBtn = dialogView.findViewById(R.id.closeBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		closeBtn.setOnClickListener(v -> {
			closeCancelBookingDialog();
		});

		cancelBtn.setOnClickListener(v -> {
			DatabaseReference databaseReference = FirebaseDatabase
					.getInstance().getReference(FirebaseMain.bookingCollection)
					.child(bookingID);

			Map<String, Object> updateBooking = new HashMap<>();
			updateBooking.put("bookingStatus", "Cancelled");

			databaseReference.updateChildren(updateBooking)
					.addOnSuccessListener(unused -> {

						closeCancelBookingDialog();

						currentBookingAdapter.notifyDataSetChanged();

						Toast.makeText(context,
								"Booking Cancelled",
								Toast.LENGTH_SHORT).show();

					})
					.addOnFailureListener(e -> {
						closeCancelBookingDialog();

						Toast.makeText(context,
								"Booking failed to cancel",
								Toast.LENGTH_LONG).show();
						Log.e(TAG, Objects.requireNonNull(e.getMessage()));
					});
		});

		builder.setView(dialogView);

		cancelBookingDialog = builder.create();
		cancelBookingDialog.show();
	}

	private void closeCancelBookingDialog() {
		if (cancelBookingDialog != null && cancelBookingDialog.isShowing()) {
			cancelBookingDialog.dismiss();
		}
	}
}