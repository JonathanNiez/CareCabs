package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
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

import com.capstone.carecabs.Adapters.TripAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentTripHistoryBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TripHistoryFragment extends Fragment {
	private final String TAG = "TripHistoryFragment";
	private Context context;
	private FragmentTripHistoryBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentTripHistoryBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.loadingLayout.setVisibility(View.GONE);
		binding.noTripHistoryTextView.setVisibility(View.GONE);

		context = getContext();
		loadTripHistoryFromFireStore();

		return view;
	}

	@SuppressLint("NotifyDataSetChanged")
	private void loadTripHistoryFromFireStore() {
		if (FirebaseMain.getUser() != null) {

			CollectionReference tripReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.tripCollection);

			List<TripModel> tripModelList = new ArrayList<>();
			TripAdapter tripAdapter = new TripAdapter(context, tripModelList, tripModel -> {
				// Handle item click if needed
			});
			binding.tripsHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.tripsHistoryRecyclerView.setAdapter(tripAdapter);

			tripReference.addSnapshotListener((value, error) -> {
				if (error != null){
					binding.loadingLayout.setVisibility(View.GONE);
					Log.e(TAG, "loadTripHistoryFromFireStore: " + error.getMessage());

					return;
				}

				if (value != null) {
					binding.loadingLayout.setVisibility(View.GONE);
					tripModelList.clear();
					boolean hasTripHistory = false;

					for (QueryDocumentSnapshot tripSnapshot : value) {
						TripModel tripModel = tripSnapshot.toObject(TripModel.class);

						if (tripModel.getDriverUserID().equals(FirebaseMain.getUser().getUid())
								|| tripModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())) {
							tripModelList.add(tripModel);
							hasTripHistory = true;
						}
					}
					tripAdapter.notifyDataSetChanged();

					if (hasTripHistory){
						binding.noTripHistoryTextView.setVisibility(View.GONE);
						binding.loadingLayout.setVisibility(View.GONE);
					}else {
						binding.noTripHistoryTextView.setVisibility(View.VISIBLE);
						binding.loadingLayout.setVisibility(View.GONE);
					}

				}else {
					Log.e(TAG, "loadTripHistoryFromFireStore: addSnapshotListener is null");
				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

}