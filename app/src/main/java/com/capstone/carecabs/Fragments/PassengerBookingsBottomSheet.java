package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.capstone.carecabs.Adapters.PickupPassengerAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.PickupPassengerBottomSheetData;
import com.capstone.carecabs.Model.PickupPassengerModel;
import com.capstone.carecabs.databinding.FragmentPassengerBookingsBottomSheetBinding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PassengerBookingsBottomSheet extends BottomSheetDialogFragment {

	public static final String TAG = "PassengerBookingsBottomSheet";
	private static final String ARG_DATA = "data";

	public interface PassengerBookingsBottomSheetListener {
		void onDataReceivedFromPassengerBookingsBottomSheet(PickupPassengerBottomSheetData pickupPassengerBottomSheetData);
	}

	private PassengerBookingsBottomSheetListener mPassengerBookingsBottomSheetListener;

	private FragmentPassengerBookingsBottomSheetBinding binding;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentPassengerBookingsBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		loadPassengerBookingsFromDatabase();
		return view;
	}

	public static PassengerBookingsBottomSheet newInstance(String data) {
		PassengerBookingsBottomSheet fragment = new PassengerBookingsBottomSheet();
		Bundle args = new Bundle();
		args.putString(ARG_DATA, data);
		fragment.setArguments(args);
		return fragment;
	}

	public void setPassengerBookingsBottomSheetListener(PassengerBookingsBottomSheet.PassengerBookingsBottomSheetListener passengerBookingsBottomSheetListener) {
		mPassengerBookingsBottomSheetListener = passengerBookingsBottomSheetListener;
	}

	private void sendDataToMap(String bookingID,
	                           String passengerID,
	                           Point pickupCoordinates,
	                           Point destinationCoordinates) {
		PickupPassengerBottomSheetData pickupPassengerBottomSheetData = new PickupPassengerBottomSheetData(
				bookingID,
				passengerID,
				pickupCoordinates,
				destinationCoordinates);
		if (mPassengerBookingsBottomSheetListener != null) {
			mPassengerBookingsBottomSheetListener.onDataReceivedFromPassengerBookingsBottomSheet(pickupPassengerBottomSheetData);
		}
	}

	private void loadPassengerBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<PickupPassengerModel> pickupPassengerModelList = new ArrayList<>();
			PickupPassengerAdapter pickupPassengerAdapter = new PickupPassengerAdapter(
					context,
					pickupPassengerModelList,
					pickupPassengerModel -> updatePassengerBooking(
							pickupPassengerModel.getPassengerUserID(),
							pickupPassengerModel.getBookingID(),
							pickupPassengerModel.getPickupLatitude(),
							pickupPassengerModel.getPickupLongitude(),
							pickupPassengerModel.getDestinationLatitude(),
							pickupPassengerModel.getDestinationLongitude()
					));
			binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.bookingHistoryRecyclerView.setAdapter(pickupPassengerAdapter);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@SuppressLint("NotifyDataSetChanged")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						binding.loadingLayout.setVisibility(View.GONE);

						boolean hasPassengersBookings = false;
						pickupPassengerModelList.clear();

						for (DataSnapshot passengerBookingSnapshot : snapshot.getChildren()) {
							PickupPassengerModel pickupPassengerModel = passengerBookingSnapshot.getValue(PickupPassengerModel.class);
							if (pickupPassengerModel != null) {

								if (pickupPassengerModel.getBookingStatus().equals("Waiting")) {

									pickupPassengerModelList.add(pickupPassengerModel);
									hasPassengersBookings = true;

								} else if (pickupPassengerModel.getBookingStatus().equals("Driver on the way") &&
										pickupPassengerModel.getDriverUserID().equals(FirebaseMain.getUser().getUid())) {

									pickupPassengerModelList.add(pickupPassengerModel);
									hasPassengersBookings = true;
								}
							}
						}
						if (hasPassengersBookings) {
							binding.noPassengerBookingsTextView.setVisibility(View.GONE);
						} else {
							binding.noPassengerBookingsTextView.setVisibility(View.VISIBLE);
						}
						pickupPassengerAdapter.notifyDataSetChanged();
					} else {
						binding.noPassengerBookingsTextView.setVisibility(View.VISIBLE);
					}
				}

				@SuppressLint("LongLogTag")
				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Log.e(TAG, error.getMessage());
				}
			});

		} else {
			Intent intent = new Intent(context, LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	@SuppressLint("LongLogTag")
	private void updatePassengerBooking(String passengerID,
	                                    String bookingID,
	                                    Double pickupLatitude,
	                                    Double pickupLongitude,
	                                    Double destinationLatitude,
	                                    Double destinationLongitude) {

		//convert to point
		LatLng pickupLatLng = new LatLng(pickupLatitude, pickupLongitude);
		Point pickupCoordinates = Point.fromLngLat(pickupLatLng.longitude, pickupLatLng.latitude);

		LatLng destinationLatLng = new LatLng(destinationLatitude, destinationLongitude);
		Point destinationCoordinates = Point.fromLngLat(destinationLatLng.longitude, destinationLatLng.latitude);


		//update booking from passenger
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("bookingStatus", "Driver on the way");
		updateBooking.put("driverUserID", FirebaseMain.getUser().getUid());

		bookingReference.child(bookingID)
				.updateChildren(updateBooking)
				.addOnSuccessListener(unused -> {

					updateDriverStatus();
					sendDataToMap(bookingID,
							passengerID,
							pickupCoordinates,
							destinationCoordinates);

					dismiss();

				})
				.addOnFailureListener(e -> {

					Log.e(TAG, Objects.requireNonNull(e.getMessage()));

					Toast.makeText(context,
							"Booking Failed to Accept", Toast.LENGTH_LONG).show();

				});
	}

	private void updateDriverStatus() {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", false);
		}
	}

}