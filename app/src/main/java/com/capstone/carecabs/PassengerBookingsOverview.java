package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Adapters.PassengerBookingsAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityPassengerBookingsOverviewBinding;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PassengerBookingsOverview extends AppCompatActivity {
	private AlertDialog bookingInfoDialog;
	private final String TAG = "PassengerBookingsOverview";
	private Intent intent;
	private ActivityPassengerBookingsOverviewBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityPassengerBookingsOverviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.imgBackBtn.setOnClickListener(v -> {
			intent = new Intent(PassengerBookingsOverview.this, MapDriverActivity.class);
			startActivity(intent);
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

			databaseReference.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();

					for (DataSnapshot passengerBookingSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingModel = passengerBookingSnapshot.getValue(PassengerBookingModel.class);
						passengerBookingModelList.add(passengerBookingModel);
					}

					PassengerBookingsAdapter passengerBookingsAdapter = new PassengerBookingsAdapter(
							getApplicationContext(),
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
									passengerBookingModel.getCurrentLatitude(),
									passengerBookingModel.getCurrentLongitude(),
									passengerBookingModel.getDestinationLatitude(),
									passengerBookingModel.getDestinationLongitude()
							));
					binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
					binding.bookingHistoryRecyclerView.setAdapter(passengerBookingsAdapter);

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

		//update booking from passenger
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("bookingStatus", "Standby");
		updateBooking.put("driverUserID", FirebaseMain.getUser().getUid());
		updateBooking.put("tripID", generateTripID);

		bookingReference.child(bookingID)
				.updateChildren(updateBooking)
				.addOnSuccessListener(unused -> Toast.makeText(PassengerBookingsOverview.this, "Booking Accepted", Toast.LENGTH_LONG).show())
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));

		updateDriverAvailabilityStatus(false);
	}

	private void updateDriverAvailabilityStatus(Boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);
		}
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
			storeTripToDatabase(
					generateRandomTripID(),
					bookingID,
					passengerID,
					currentLatitude,
					currentLongitude,
					destinationLatitude,
					destinationLongitude
			);

			closeBookingInfoDialog();
		});


		bookingInfoDialog = builder.create();
		bookingInfoDialog.show();
	}

	private void closeBookingInfoDialog() {
		if (bookingInfoDialog != null & bookingInfoDialog.isShowing()) {
			bookingInfoDialog.dismiss();
		}
	}
}