package com.capstone.carecabs.BottomSheetModal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.DriverRatingsModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentTripRatingsBottomSheetBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class TripRatingsBottomSheet extends BottomSheetDialogFragment {
	private static final String TAG = "TripRatingsBottomSheet";
	private FragmentTripRatingsBottomSheetBinding binding;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentTripRatingsBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.ratingsSubmittedLayout.setVisibility(View.GONE);

		context = getContext();

		Bundle bundle = getArguments();
		if (bundle != null) {
			String driverID = bundle.getString("driverID");
			String bookingID = bundle.getString("bookingID");

			binding.rateDriverBtn.setOnClickListener(v -> {
				if (binding.tripRatingBar.getRating() == 0) {
					return;
				} else {
					rateDriver(driverID, bookingID);
				}
			});

			binding.tripRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
				@Override
				public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
					if (rating >= 2){
						binding.ratingsLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
					}else {

					}
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
		updateBooking.put("isDriverRated", true);

		bookingReference.child(bookingID).updateChildren(updateBooking)
				.addOnSuccessListener(unused -> Log.i(TAG, "rateDriver: onSuccess booking updated successfully"))
				.addOnFailureListener(e -> Log.e(TAG, "rateDriver: onFailure " + e.getMessage()));

		//update driver ratings
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(driverID);

		Map<String, Object> rateDriver = new HashMap<>();
		rateDriver.put("driverRatings", + binding.tripRatingBar.getRating());

		documentReference.update(rateDriver)
				.addOnSuccessListener(unused -> {
					binding.ratingsLayout.setVisibility(View.GONE);
					binding.ratingsSubmittedLayout.setVisibility(View.VISIBLE);

					new Handler().postDelayed(this::dismiss, 2000);
				})
				.addOnFailureListener(e -> {
					Toast.makeText(context, "Failed to rate Driver", Toast.LENGTH_SHORT).show();

					Log.e(TAG, "rateDriver: " + e.getMessage());
				});
	}
}