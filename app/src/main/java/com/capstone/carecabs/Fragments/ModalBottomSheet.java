package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentBottomSheetBinding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModalBottomSheet extends BottomSheetDialogFragment {
	private static final String ARG_DATA = "data";
	public static final String TAG = "ModalBottomSheet";

	public interface BottomSheetListener {
		void onDataReceived(Point data);
	}

	private BottomSheetListener mBottomSheetListener;
	private Context context;
	private FragmentBottomSheetBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		binding.medicalConditionTextView.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);

		binding.pickupBtn.setOnClickListener(v -> {

		});

		binding.closeBtn.setOnClickListener(v -> {
			dismiss();
		});

		loadPassengerBooking();

		return view;

	}

	public void setBottomSheetListener(BottomSheetListener bottomSheetListener) {
		mBottomSheetListener = bottomSheetListener;
	}

	private void sendDataToMap(Point data) {
		if (mBottomSheetListener != null) {
			mBottomSheetListener.onDataReceived(data);
		}
	}

	public void loadPassengerBooking() {
		if (getArguments() != null) {
			String bookingID = getArguments().getString(ARG_DATA);

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection).child(bookingID);
			databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
				@SuppressLint("SetTextI18n")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot != null && snapshot.exists()) {
						binding.progressBarLayout.setVisibility(View.GONE);

						String getPassengerProfilePicture = snapshot.child("passengerProfilePicture").getValue(String.class);
						String getPassengerUserType = snapshot.child("passengerUserType").getValue(String.class);
						String getPassengerFirstname = snapshot.child("passengerFirstname").getValue(String.class);
						String getPassengerLastname = snapshot.child("passengerLastname").getValue(String.class);

						Double getPassengerPickupLatitude = snapshot.child("pickupLatitude").getValue(Double.class);
						Double getPassengerPickupLongitude = snapshot.child("pickupLongitude").getValue(Double.class);
						Double getPassengerDestinationLatitude = snapshot.child("destinationLatitude").getValue(Double.class);
						Double getPassengerDestinationLongitude = snapshot.child("destinationLongitude").getValue(Double.class);


						//geocode
						MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
								.accessToken(getString(R.string.mapbox_access_token))
								.query(Point.fromLngLat(getPassengerPickupLongitude, getPassengerPickupLatitude))
								.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
								.build();

						pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
							@Override
							public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
								if (response.body() != null && response.body().features() != null) {
									CarmenFeature feature = response.body().features().get(0);
									String locationName = feature.placeName();

									binding.pickupLocationTextView.setText(locationName);
								} else {
									Log.e(TAG, response.message());
								}
							}

							@Override
							public void onFailure(Call<GeocodingResponse> call, Throwable t) {
								Log.e(TAG, t.getMessage());
							}
						});

						MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
								.accessToken(getString(R.string.mapbox_access_token))
								.query(Point.fromLngLat(getPassengerDestinationLongitude, getPassengerDestinationLatitude))
								.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
								.build();
						destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
							@Override
							public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
								if (response.body() != null && response.body().features() != null) {
									CarmenFeature feature = response.body().features().get(0);
									String locationName = feature.placeName();

									binding.destinationLocationTextView.setText(locationName);
								} else {
									Log.e(TAG, response.message());
								}
							}

							@Override
							public void onFailure(Call<GeocodingResponse> call, Throwable t) {
								Log.e(TAG, t.getMessage());
							}
						});


						switch (getPassengerUserType) {
							case "Senior Citizen":
								binding.medicalConditionTextView.setVisibility(View.VISIBLE);
								binding.userTypeImageView.setImageResource(R.drawable.senior_32);
								String getPassengerMedicalCondition = snapshot.child("passengerMedicalCondition").getValue(String.class);

								binding.medicalConditionTextView.setText("Medical Condition:\n" + getPassengerMedicalCondition);

								break;

							case "Persons with Disability (PWD)":
								binding.disabilityTextView.setVisibility(View.VISIBLE);
								binding.userTypeImageView.setImageResource(R.drawable.pwd_32);
								String getPassengerDisability = snapshot.child("passengerDisability").getValue(String.class);

								binding.disabilityTextView.setText("Disability:\n" + getPassengerDisability);

								break;
						}

						if (!getPassengerProfilePicture.equals("default")) {
							Glide.with(context)
									.load(getPassengerProfilePicture)
									.placeholder(R.drawable.loading_gif)
									.into(binding.passengerProfilePic);
						}


						binding.fullNameTextView.setText(getPassengerFirstname + " " +
								getPassengerLastname);
						binding.userTypeTextView.setText(getPassengerUserType);

						binding.pickupBtn.setOnClickListener(v -> {
							updatePassengerBooking(
									bookingID,
									getPassengerPickupLatitude,
									getPassengerPickupLongitude,
									getPassengerDestinationLatitude,
									getPassengerDestinationLongitude
							);
						});
					}
				}

				@SuppressLint("LongLogTag")
				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Log.e(TAG, error.getMessage());
				}
			});
		}
	}

	private void updatePassengerBooking(String bookingID,
	                                    Double pickupLatitude,
	                                    Double pickupLongitude,
	                                    Double destinationLatitude,
	                                    Double destinationLongitude) {

		//convert to point
		LatLng latLng = new LatLng(destinationLatitude, destinationLongitude);
		Point point = Point.fromLngLat(latLng.longitude, latLng.latitude);

		//update booking from passenger
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("bookingStatus", "Driver on the way");
		updateBooking.put("driverUserID", FirebaseMain.getUser().getUid());

		bookingReference.child(bookingID)
				.updateChildren(updateBooking)
				.addOnSuccessListener(unused -> {

					updateDriverStatus(false);
					Toast.makeText(context,
							"Booking Accepted", Toast.LENGTH_LONG).show();

					sendDataToMap(point);

					//TODO
					storeTripToDatabase(
							generateRandomTripID(),
							bookingID,
							"passID",
							pickupLatitude,
							pickupLongitude,
							destinationLatitude,
							destinationLongitude
					);
					dismiss();

				})
				.addOnFailureListener(e -> {

					Log.e(TAG, e.getMessage());

					Toast.makeText(context,
							"Booking Failed to Accept", Toast.LENGTH_LONG).show();

				});
	}

	private void storeTripToDatabase(
			String generateTripID,
			String bookingID,
			String passengerID,
			Double pickupLatitude,
			Double pickupLongitude,
			Double destinationLatitude,
			Double destinationLongitude
	) {

		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.tripCollection).document(generateTripID);

		TripModel tripModel = new TripModel(
				generateTripID,
				bookingID,
				"Ongoing",
				FirebaseMain.getUser().getUid(),
				passengerID,
				getCurrentTimeAndDate(),
				pickupLatitude,
				pickupLongitude,
				destinationLatitude,
				destinationLongitude
		);

		documentReference.set(tripModel).addOnSuccessListener(new OnSuccessListener<Void>() {
			@Override
			public void onSuccess(Void unused) {

			}
		}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
	}

	private void updateDriverStatus(boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);
		}
	}

	private String generateRandomTripID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	private String getCurrentTimeAndDate() {
		Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based, so add 1
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		return month + "-" + day + "-" + year + " " + hour + ":" + minute + ":" + second;
	}

	public static ModalBottomSheet newInstance(String data) {
		ModalBottomSheet fragment = new ModalBottomSheet();
		Bundle args = new Bundle();
		args.putString(ARG_DATA, data);
		fragment.setArguments(args);
		return fragment;
	}
}