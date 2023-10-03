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
import com.capstone.carecabs.ChatActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCurrentBookingBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.noCurrentBookingTextView.setVisibility(View.GONE);

		context = getContext();
		loadCurrentBookingFromDatabase();

		return view;
	}

	private void loadCurrentBookingFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						List<CurrentBookingModel> currentBookingModelList = new ArrayList<>();

						for (DataSnapshot currentBookingSnapshot : snapshot.getChildren()) {

							CurrentBookingModel currentBookingModel =
									currentBookingSnapshot.getValue(CurrentBookingModel.class);
							if (currentBookingModel != null) {
								if (currentBookingModel.getBookingStatus().equals("Waiting") ||
										currentBookingModel.getBookingStatus().equals("Driver on the way")) {
									currentBookingModelList.add(currentBookingModel);

									currentBookingAdapter = new CurrentBookingAdapter(
											context,
											currentBookingModelList,
											currentBookingModel1 ->
													showCancelBookingDialog(currentBookingModel.getBookingID()));

								} else if (currentBookingModel.getBookingStatus().equals("Cancelled")) {
									binding.noCurrentBookingTextView.setVisibility(View.VISIBLE);
								}
							} else {
								binding.noCurrentBookingTextView.setVisibility(View.VISIBLE);
							}
						}
						binding.currentBookingsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
						binding.currentBookingsRecyclerView.setAdapter(currentBookingAdapter);
					} else {
//						binding.noCurrentBookingTextView.setVisibility(View.VISIBLE);
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
			getActivity().finish();
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
						Log.e(TAG, e.getMessage());
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

	private void goToChatActivity(String driverID, String tripID) {
		Intent intent = new Intent(getActivity(), ChatActivity.class);
		intent.putExtra("driverID", driverID);
		intent.putExtra("tripID", tripID);
		startActivity(intent);
	}
}