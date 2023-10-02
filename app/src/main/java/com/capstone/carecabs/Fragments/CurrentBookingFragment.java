package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.CurrentBookingAdapter;
import com.capstone.carecabs.ChatActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.databinding.FragmentCurrentBookingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CurrentBookingFragment extends Fragment {
	private final String TAG = "CurrentBookingFragment";
	private Context context;
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
								currentBookingModelList.add(currentBookingModel);

								CurrentBookingAdapter currentBookingAdapter = new CurrentBookingAdapter(
										context,
										currentBookingModelList);
								binding.currentBookingsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
								binding.currentBookingsRecyclerView.setAdapter(currentBookingAdapter);
							}
						}
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