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

import com.capstone.carecabs.Adapters.CurrentTripAdapter;
import com.capstone.carecabs.Adapters.TripAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.CurrentTripModel;
import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.databinding.FragmentCurrentTripBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CurrentTripFragment extends Fragment {
	private final String TAG = "CurrentTripFragment";
	private Context context;
	private FragmentCurrentTripBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCurrentTripBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.loadingLayout.setVisibility(View.GONE);
		binding.noCurrentTripsTextView.setVisibility(View.GONE);

		context = getContext();
		loadCurrentTripFromFireStore();

		return view;
	}

	@SuppressLint("NotifyDataSetChanged")
	private void loadCurrentTripFromFireStore() {
		if (FirebaseMain.getUser() != null) {

			CollectionReference collectionReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.tripCollection);

			List<CurrentTripModel> currentTripModelList = new ArrayList<>();
			CurrentTripAdapter currentTripAdapter = new CurrentTripAdapter(context, currentTripModelList);
			binding.currentTripRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.currentTripRecyclerView.setAdapter(currentTripAdapter);

			collectionReference.addSnapshotListener((value, error) -> {
				if (error != null){
					Log.e(TAG, "loadCurrentTripFromFireStore: " + error.getMessage());

					return;
				}

				if (value != null) {
					binding.loadingLayout.setVisibility(View.GONE);
					currentTripModelList.clear();
					boolean hasCurrentTrip = false;

					for (QueryDocumentSnapshot tripSnapshot : value) {
						CurrentTripModel currentTripModel = tripSnapshot.toObject(CurrentTripModel.class);

						if (currentTripModel.getDriverUserID().equals(FirebaseMain.getUser().getUid())
								|| currentTripModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())) {
							currentTripModelList.add(currentTripModel);
							hasCurrentTrip = true;
						}
					}

					if (hasCurrentTrip){
						binding.noCurrentTripsTextView.setVisibility(View.GONE);
					}else {
						binding.noCurrentTripsTextView.setVisibility(View.VISIBLE);
					}
					currentTripAdapter.notifyDataSetChanged();
				}else {
					Log.e(TAG, "loadCurrentTripFromFireStore: addSnapshotListener is null");
				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

}