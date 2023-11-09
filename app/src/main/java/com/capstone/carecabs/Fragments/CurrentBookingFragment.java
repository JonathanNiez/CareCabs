package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.CurrentBookingAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Map.MapPassengerActivity;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.DialogEnableLocationServiceBinding;
import com.capstone.carecabs.databinding.FragmentCurrentBookingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CurrentBookingFragment extends Fragment {
	private final String TAG = "CurrentBookingFragment";
	private static final int REQUEST_ENABLE_LOCATION = 1;
	private Context context;
	private final String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private AlertDialog cancelBookingDialog, enableLocationServiceDialog;
	private AlertDialog.Builder builder;
	private Intent intent;
	private CurrentBookingAdapter currentBookingAdapter;
	private FragmentCurrentBookingBinding binding;

	@Override
	public void onPause() {
		super.onPause();

		closeCancelBookingDialog();
		closeEnableLocationServiceDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		closeCancelBookingDialog();
		closeEnableLocationServiceDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentCurrentBookingBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.noCurrentBookingsLayout.setVisibility(View.GONE);

		binding.bookARideBtn.setOnClickListener(v -> checkLocationService());

		loadCurrentBookingFromDatabase();
		getUserSettings();

		return view;
	}

	private void getUserSettings() {

		if (voiceAssistantState.equals("enabled")) {
			VoiceAssistant voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Bookings");
		}
	}

	private void loadCurrentBookingFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference bookingReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<CurrentBookingModel> currentBookingModelList = new ArrayList<>();
			currentBookingAdapter = new CurrentBookingAdapter(
					context,
					currentBookingModelList,
					currentBookingModel ->
							showCancelBookingDialog(
									currentBookingModel.getBookingID()
							),
					getActivity());

			binding.currentBookingsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.currentBookingsRecyclerView.setAdapter(currentBookingAdapter);
			bookingReference.addValueEventListener(new ValueEventListener() {
				@SuppressLint("NotifyDataSetChanged")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						binding.loadingLayout.setVisibility(View.GONE);
						currentBookingModelList.clear();
						boolean hasCurrentBookings = false;
						final String userID = FirebaseMain.getUser().getUid();

						for (DataSnapshot currentBookingSnapshot : snapshot.getChildren()) {
							CurrentBookingModel currentBookingModel =
									currentBookingSnapshot.getValue(CurrentBookingModel.class);
							if (currentBookingModel != null) {

								if (currentBookingModel.getPassengerUserID().equals(userID)
										&& currentBookingModel.getBookingStatus().equals("Waiting")
										|| currentBookingModel.getPassengerUserID().equals(userID)
										&& currentBookingModel.getBookingStatus().equals("Driver on the way")
										|| currentBookingModel.getPassengerUserID().equals(userID)
										&& currentBookingModel.getBookingStatus().equals("Transported to destination")
										&& currentBookingModel.getRatingStatus().equals("Driver not rated")) {

									currentBookingModelList.add(currentBookingModel);
									hasCurrentBookings = true;

								}
							}
						}
						currentBookingAdapter.notifyDataSetChanged();

						if (hasCurrentBookings) {
							binding.noCurrentBookingsLayout.setVisibility(View.GONE);
						} else {
							binding.noCurrentBookingsLayout.setVisibility(View.VISIBLE);
						}
					} else {
						binding.noCurrentBookingsLayout.setVisibility(View.VISIBLE);
						binding.loadingLayout.setVisibility(View.GONE);
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Log.e(TAG, "loadCurrentBookingFromDatabase: onCancelled " + error.getMessage());
				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	private void goToMap() {
		intent = new Intent(context, MapPassengerActivity.class);
		startActivity(intent);
		Objects.requireNonNull(getActivity()).finish();
	}

	private void checkLocationService() {
		LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
		boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!isGpsEnabled && !isNetworkEnabled) {
			showEnableLocationServiceDialog();
		} else {
			goToMap();
		}

	}

	private void showEnableLocationServiceDialog() {
		builder = new AlertDialog.Builder(context);

		DialogEnableLocationServiceBinding binding = DialogEnableLocationServiceBinding.inflate(getLayoutInflater());
		View dialogView = binding.getRoot();

		binding.enableLocationServiceBtn.setOnClickListener(v -> {
			intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, REQUEST_ENABLE_LOCATION);

			closeEnableLocationServiceDialog();
		});

		builder.setView(dialogView);

		enableLocationServiceDialog = builder.create();
		enableLocationServiceDialog.show();
	}

	private void closeEnableLocationServiceDialog() {
		if (enableLocationServiceDialog != null && enableLocationServiceDialog.isShowing()) {
			enableLocationServiceDialog.dismiss();
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private void showCancelBookingDialog(String bookingID) {
		builder = new AlertDialog.Builder(context);
		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_booking, null);

		Button closeBtn = dialogView.findViewById(R.id.closeBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		closeBtn.setOnClickListener(v -> {
			closeCancelBookingDialog();
		});

		cancelBtn.setOnClickListener(v -> {
			DatabaseReference databaseReference = FirebaseDatabase
					.getInstance().getReference(FirebaseMain.bookingCollection)
					.child(bookingID);

			Map<String, Object> updateBooking = new HashMap<>();
			updateBooking.put("bookingStatus", "Cancelled");

			databaseReference.updateChildren(updateBooking)
					.addOnSuccessListener(unused -> {

						closeCancelBookingDialog();

						currentBookingAdapter.notifyDataSetChanged();

						Toast.makeText(context,
								"Booking Cancelled",
								Toast.LENGTH_SHORT).show();

					})
					.addOnFailureListener(e -> {

						closeCancelBookingDialog();

						Toast.makeText(context,
								"Booking failed to cancel",
								Toast.LENGTH_LONG).show();
						Log.e(TAG, Objects.requireNonNull(e.getMessage()));
					});
		});

		builder.setView(dialogView);

		cancelBookingDialog = builder.create();
		cancelBookingDialog.show();
	}

	private void closeCancelBookingDialog() {
		if (cancelBookingDialog != null && cancelBookingDialog.isShowing()) {
			cancelBookingDialog.dismiss();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_ENABLE_LOCATION) {
			// Check if the user enabled location services after going to settings.
			LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
			boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (isGpsEnabled || isNetworkEnabled) {
				// Location services are now enabled, open your desired activity.
				goToMap();
			} else {
				// Location services are still not enabled, you can show a message to the user.
				Toast.makeText(context, "Location services are still disabled.", Toast.LENGTH_SHORT).show();
			}
		}
	}
}