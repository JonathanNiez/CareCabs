package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.carecabs.Adapters.CurrentPassengerPendingBookingsAdapter;
import com.capstone.carecabs.ChatActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.CurrentPassengerPendingBookingsModel;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentPendingBookingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PendingBookingFragment extends Fragment {
	private final String TAG = "PendingBookingFragment";
	private Context context;
	private FragmentPendingBookingBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentPendingBookingBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		loadBookingsFromDatabase();

		return view;
	}

	private void loadBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						List<CurrentPassengerPendingBookingsModel> currentPassengerPendingBookingsModelList = new ArrayList<>();

						for (DataSnapshot pendingBookingSnapshot : snapshot.getChildren()) {
							CurrentPassengerPendingBookingsModel currentPassengerPendingBookingsModel =
									pendingBookingSnapshot.getValue(CurrentPassengerPendingBookingsModel.class);
							if (currentPassengerPendingBookingsModel != null) {
								if (currentPassengerPendingBookingsModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())
										&& currentPassengerPendingBookingsModel.getBookingStatus().equals("Standby")) {
									currentPassengerPendingBookingsModelList.add(currentPassengerPendingBookingsModel);
								}
							}
						}

						CurrentPassengerPendingBookingsAdapter currentPassengerPendingBookingsAdapter =
								new CurrentPassengerPendingBookingsAdapter(
										context,
										currentPassengerPendingBookingsModelList,
										currentPassengerPendingBookingsModel -> goToChatActivity(
												currentPassengerPendingBookingsModel.getDriverUserID(),
												currentPassengerPendingBookingsModel.getTripID()
										));
						binding.pendingBookingsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
						binding.pendingBookingsRecyclerView.setAdapter(currentPassengerPendingBookingsAdapter);

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

	private void goToChatActivity(String driverID, String tripID) {
		Intent intent = new Intent(getActivity(), ChatActivity.class);
		intent.putExtra("driverID", driverID);
		intent.putExtra("tripID", tripID);
		startActivity(intent);
	}
}