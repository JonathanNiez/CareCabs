package com.capstone.carecabs.BottomSheetModal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.databinding.FragmentRateDriverBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class RateDriverBottomSheet extends BottomSheetDialogFragment {
	private static final String TAG = "TripRatingsBottomSheet";
	private FragmentRateDriverBottomSheetBinding binding;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentRateDriverBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		binding.driverRatedLayout.setVisibility(View.GONE);

		Bundle bundle = getArguments();
		if (bundle != null) {
			String driverID = bundle.getString("driverID");
			String bookingID = bundle.getString("bookingID");

			binding.rateDriverBtn.setOnClickListener(v -> {
				if (binding.driverRatingBar.getRating() == 0) {
					return;
				} else {
					rateDriver(driverID, bookingID);
				}
			});

		} else {
			dismiss();
		}

		binding.laterBtn.setOnClickListener(v -> {
			dismiss();
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	private void rateDriver(String driverID, String bookingID) {

		//update current booking
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("ratingStatus", "Driver rated");

		bookingReference.child(bookingID).updateChildren(updateBooking)
				.addOnSuccessListener(unused -> Log.d(TAG, "rateDriver: onSuccess booking updated successfully"))
				.addOnFailureListener(e -> Log.e(TAG, "rateDriver: onFailure " + e.getMessage()));

		//update driver ratings
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(driverID);

		documentReference.get().addOnSuccessListener(documentSnapshot -> {
			if (documentSnapshot.exists()) {
				double currentRatings = documentSnapshot.getDouble("driverRatings");
				float newRating = binding.driverRatingBar.getRating();
				double totalRatings = currentRatings + newRating;

				Map<String, Object> updatedData = new HashMap<>();
				updatedData.put("driverRatings", totalRatings);

				documentReference.update(updatedData)
						.addOnSuccessListener(unused -> {
							// Ratings updated successfully
							binding.ratingsLayout.setVisibility(View.GONE);
							binding.driverRatedLayout.setVisibility(View.VISIBLE);

							new Handler().postDelayed(this::dismiss, 2000);
						})
						.addOnFailureListener(e -> {
							Toast.makeText(context, "Failed to rate Driver", Toast.LENGTH_SHORT).show();
							Log.e(TAG, "rateDriver: " + e.getMessage());
						});
			} else {
				Log.e(TAG, "rateDriver: document not exist");
			}
		}).addOnFailureListener(e -> {
			Log.e(TAG, "rateDriver: " + e.getMessage());
		});
	}
}