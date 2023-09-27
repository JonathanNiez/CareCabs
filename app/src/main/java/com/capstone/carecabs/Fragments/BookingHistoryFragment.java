package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Adapters.BookingHistoryAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.capstone.carecabs.databinding.FragmentBookingHistoryBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryFragment extends Fragment {
	private final String TAG = "BookingHistoryFragment";
	private AlertDialog bookingInfoDialog;
	private FragmentBookingHistoryBinding binding;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentBookingHistoryBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		loadBookingsFromDatabase();

		return view;
	}

	private void loadBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(StaticDataPasser.bookingCollection);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();

						for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
							PassengerBookingModel passengerBookingModel =
									locationSnapshot.getValue(PassengerBookingModel.class);
							if (passengerBookingModel != null) {
								if (passengerBookingModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())) {
									passengerBookingModelList.add(passengerBookingModel);
								}
							}
						}

						BookingHistoryAdapter bookingHistoryAdapter = new BookingHistoryAdapter(
								context,
								passengerBookingModelList,
								passengerBookingModel ->
										showBookingInfoDialog(passengerBookingModel.getBookingID()));
						binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
						binding.bookingHistoryRecyclerView.setAdapter(bookingHistoryAdapter);

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

	private void showBookingInfoDialog(String bookingID) {

		DialogBookingInfoBinding binding = DialogBookingInfoBinding
				.inflate(getLayoutInflater());

		View dialogView = binding.getRoot();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setView(dialogView);


		binding.closeBtn.setOnClickListener(view -> {
			closeBookingInfoDialog();
		});

		binding.pickupBtn.setOnClickListener(view -> {

		});


		bookingInfoDialog = builder.create();
		bookingInfoDialog.show();
	}

	private void closeBookingInfoDialog() {
		if (bookingInfoDialog != null & bookingInfoDialog.isShowing()) {
			bookingInfoDialog.dismiss();
		}
	}
}