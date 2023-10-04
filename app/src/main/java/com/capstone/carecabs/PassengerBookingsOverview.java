package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Adapters.PassengerBookingsAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.ParcelablePoint;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityPassengerBookingsOverviewBinding;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PassengerBookingsOverview extends AppCompatActivity {
	private final String TAG = "PassengerBookingsOverview";
	private AlertDialog bookingInfoDialog, noInternetDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityPassengerBookingsOverviewBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
		closeBookingInfoDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeNoInternetDialog();
		closeBookingInfoDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityPassengerBookingsOverviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.noPassengerBookingsTextView.setVisibility(View.GONE);

		binding.imgBackBtn.setOnClickListener(v -> {
			finish();
		});

		loadPassengerBookingsFromDatabase();

	}

	@Override
	public void onBackPressed() {
		finish();
	}

	private void loadPassengerBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();
			PassengerBookingsAdapter passengerBookingsAdapter = new PassengerBookingsAdapter(
					this,
					passengerBookingModelList,
					passengerBookingModel -> showBookingInfoDialog(
							passengerBookingModel.getPassengerFirstname(),
							passengerBookingModel.getPassengerLastname(),
							passengerBookingModel.getPassengerUserType(),
							passengerBookingModel.getPassengerProfilePicture(),
							passengerBookingModel.getPassengerDisability(),
							passengerBookingModel.getPassengerMedicalCondition(),
							passengerBookingModel.getBookingStatus(),
							passengerBookingModel.getBookingID(),
							passengerBookingModel.getPassengerUserID(),
							passengerBookingModel.getPickupLatitude(),
							passengerBookingModel.getPickupLongitude(),
							passengerBookingModel.getDestinationLatitude(),
							passengerBookingModel.getDestinationLongitude()
					));
			binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
			binding.bookingHistoryRecyclerView.setAdapter(passengerBookingsAdapter);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@SuppressLint("NotifyDataSetChanged")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						binding.loadingLayout.setVisibility(View.GONE);

						boolean hasPassengersBookings = false;
						passengerBookingModelList.clear();

						for (DataSnapshot passengerBookingSnapshot : snapshot.getChildren()) {
							PassengerBookingModel passengerBookingModel = passengerBookingSnapshot.getValue(PassengerBookingModel.class);
							if (passengerBookingModel != null) {

								if (passengerBookingModel.getBookingStatus().equals("Waiting")) {

									passengerBookingModelList.add(passengerBookingModel);
									hasPassengersBookings = true;

								} else if (passengerBookingModel.getBookingStatus().equals("Driver on the way") &&
										passengerBookingModel.getDriverUserID().equals(FirebaseMain.getUser().getUid())) {

									passengerBookingModelList.add(passengerBookingModel);
									hasPassengersBookings = true;
								}
							}
						}
						if (hasPassengersBookings) {
							binding.noPassengerBookingsTextView.setVisibility(View.GONE);
						} else {
							binding.noPassengerBookingsTextView.setVisibility(View.VISIBLE);
						}
						passengerBookingsAdapter.notifyDataSetChanged();
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
			Intent intent = new Intent(PassengerBookingsOverview.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private String generateRandomTripID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	@SuppressLint("DefaultLocale")
	private String getCurrentTimeAndDate() {
		Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based, so add 1
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		return String.format("%02d-%02d-%04d %02d:%02d:%02d", month, day, year, hour, minute, second);
	}

	@SuppressLint("LongLogTag")
	private void storeTripToDatabase(
			String generateTripID,
			String bookingID,
			String passengerID,
			Double currentLatitude,
			Double currentLongitude,
			Double destinationLatitude,
			Double destinationLongitude
	) {

		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.tripCollection)
				.document(generateTripID);

		TripModel tripModel = new TripModel(
				generateTripID,
				false,
				bookingID,
				"Ongoing",
				FirebaseMain.getUser().getUid(),
				passengerID,
				getCurrentTimeAndDate(),
				currentLatitude,
				currentLongitude,
				destinationLatitude,
				destinationLongitude
		);

		documentReference.set(tripModel)
				.addOnSuccessListener(unused -> {

				})
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));


	}

	private void updateDriverAvailabilityStatus(Boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);
		}
	}

	@SuppressLint("LongLogTag")
	private void updatePassengerBooking(String bookingID,
	                                    Double latitude,
	                                    Double longitude) {

		//convert to point
		LatLng latLng = new LatLng(latitude, longitude);

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

					Toast.makeText(PassengerBookingsOverview.this,
							"Booking Accepted", Toast.LENGTH_LONG).show();

					goToMapAndFindRoute(point);

				})
				.addOnFailureListener(e -> {

					Log.e(TAG, e.getMessage());

					Toast.makeText(PassengerBookingsOverview.this,
							"Booking Failed to Accept", Toast.LENGTH_LONG).show();

				});
	}

	private void goToMapAndFindRoute(Point point) {
		Intent intent = new Intent(
				PassengerBookingsOverview.this, MapDriverActivity.class);
		intent.putExtra("dataSend", true);
		intent.putExtra("point", new ParcelablePoint(point));
		startActivity(intent);
		finish();
	}

	@SuppressLint("SetTextI18n")
	private void showBookingInfoDialog(
			String firstname,
			String lastname,
			String userType,
			String profilePicture,
			String disability,
			String medicalCondition,
			String bookingStatus,
			String bookingID,
			String passengerID,
			Double currentLatitude,
			Double currentLongitude,
			Double destinationLatitude,
			Double destinationLongitude
	) {

		DialogBookingInfoBinding binding = DialogBookingInfoBinding
				.inflate(getLayoutInflater());

		View dialogView = binding.getRoot();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setView(dialogView);

		binding.medicalConditionTextView.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);

		String fullName = firstname + " " + lastname;

		binding.fullNameTextView.setText(fullName);

		switch (userType) {
			case "Senior Citizen":
				binding.medicalConditionTextView.setVisibility(View.VISIBLE);
				binding.medicalConditionTextView.setText("Medical Condition(s): " + medicalCondition);
				break;

			case "Persons with Disability (PWD)":
				binding.disabilityTextView.setVisibility(View.VISIBLE);
				binding.disabilityTextView.setText("Disability: " + disability);

				break;
		}

		binding.userTypeTextView.setText(userType);
		binding.bookingStatusTextView.setText("Booking Status: " + bookingStatus);

		if (!profilePicture.equals("default")) {
			Glide.with(this)
					.load(profilePicture)
					.placeholder(R.drawable.loading_gif)
					.into(binding.passengerProfilePic);
		}

		binding.closeBtn.setOnClickListener(view -> {
			closeBookingInfoDialog();
		});

		binding.pickupBtn.setOnClickListener(view -> {
			//TODO: navigation to passenger location

			updatePassengerBooking(bookingID,
					destinationLatitude,
					destinationLongitude);

//			storeTripToDatabase(
//					generateRandomTripID(),
//					bookingID,
//					passengerID,
//					currentLatitude,
//					currentLongitude,
//					destinationLatitude,
//					destinationLongitude
//			);

			closeBookingInfoDialog();
		});


		bookingInfoDialog = builder.create();
		bookingInfoDialog.show();
	}

	private void closeBookingInfoDialog() {
		if (bookingInfoDialog != null && bookingInfoDialog.isShowing()) {
			bookingInfoDialog.dismiss();
		}
	}

	private void showNoInternetDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

		Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

		tryAgainBtn.setOnClickListener(v -> {
			closeNoInternetDialog();
		});

		builder.setView(dialogView);

		noInternetDialog = builder.create();
		noInternetDialog.show();
	}

	private void closeNoInternetDialog() {
		if (noInternetDialog != null && noInternetDialog.isShowing()) {
			noInternetDialog.dismiss();

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
			updateConnectionStatus(isConnected);
		}
	}

	private void initializeNetworkChecker() {
		networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkChangeListener() {
			@Override
			public void onNetworkChanged(boolean isConnected) {
				updateConnectionStatus(isConnected);
			}
		});

		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
		updateConnectionStatus(isConnected);

	}

	private void updateConnectionStatus(boolean isConnected) {
		if (isConnected) {
			if (noInternetDialog != null && noInternetDialog.isShowing()) {
				noInternetDialog.dismiss();
			}
		} else {
			showNoInternetDialog();
		}
	}
}