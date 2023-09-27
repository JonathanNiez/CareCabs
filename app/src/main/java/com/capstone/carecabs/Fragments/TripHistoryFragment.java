package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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

public class TripHistoryFragment extends Fragment {
	private Context context;
	private FragmentTripHistoryBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentTripHistoryBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		loadTripHistoryFromFireStore(context);

		return view;
	}


	private void loadTripHistoryFromFireStore(Context context) {
		if (FirebaseMain.getUser() != null) {

			CollectionReference collectionReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.tripCollection);

			collectionReference.addSnapshotListener((value, error) -> {
				if (value != null) {
					List<TripModel> tripModelList = new ArrayList<>();

					for (QueryDocumentSnapshot tripSnapshot : value) {
						TripModel tripModel = tripSnapshot.toObject(TripModel.class);

						if (tripModel.getUserPassengerID().equals(FirebaseMain.getUser().getUid())) {
							tripModelList.add(tripModel);
						}
					}

					TripAdapter tripAdapter = new TripAdapter(context, tripModelList);
					binding.tripsHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
					binding.tripsHistoryRecyclerView.setAdapter(tripAdapter);

				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			getActivity().finish();
		}
	}

}