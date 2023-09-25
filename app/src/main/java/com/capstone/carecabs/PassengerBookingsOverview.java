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

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Adapters.PassengerAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityPassengerBookingsOverviewBinding;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

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

		loadBookingsFromDatabase();

	}

	@Override
	public void onBackPressed() {
		intent = new Intent(PassengerBookingsOverview.this, MapDriverActivity.class);
		startActivity(intent);
		finish();
	}

	private void loadBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(StaticDataPasser.bookingCollection);

			databaseReference.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();

					for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingModel = locationSnapshot.getValue(PassengerBookingModel.class);
						passengerBookingModelList.add(passengerBookingModel);
					}

					PassengerAdapter passengerAdapter = new PassengerAdapter(
							getApplicationContext(),
							passengerBookingModelList,
							passengerBookingModel -> showBookingInfoDialog(
									passengerBookingModel.getPassengerFirstname(),
									passengerBookingModel.getPassengerLastname(),
									passengerBookingModel.getPassengerUserType(),
									passengerBookingModel.getPassengerProfilePicture(),
									passengerBookingModel.getPassengerDisability(),
									passengerBookingModel.getPassengerMedicalCondition()
							));
					binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
					binding.bookingHistoryRecyclerView.setAdapter(passengerAdapter);

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

	private void showBookingInfoDialog(String firstname,
	                                   String lastname,
	                                   String userType,
	                                   String profilePicture,
	                                   String disability,
	                                   String medicalCondition) {

		DialogBookingInfoBinding binding = DialogBookingInfoBinding
				.inflate(getLayoutInflater());

		View dialogView = binding.getRoot();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setView(dialogView);

		binding.medicalConditionTextView.setVisibility(View.GONE);
		binding.disabilityTextView.setVisibility(View.GONE);

		String fullname = firstname + " " + lastname;

		binding.fullNameTextView.setText(fullname);

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