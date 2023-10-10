package com.capstone.carecabs.BottomSheetModal;

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
import com.capstone.carecabs.Firebase.APIService;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.PickupPassengerBottomSheetData;
import com.capstone.carecabs.Model.PickupPassengerModel;
import com.capstone.carecabs.Utility.DistanceCalculator;
import com.capstone.carecabs.databinding.FragmentPassengerBookingsBottomSheetBinding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.mapbox.geojson.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
					pickupPassengerModel -> getVehicleInfo(
							pickupPassengerModel.getFcmToken(),
							pickupPassengerModel.getPassengerUserID(),
							pickupPassengerModel.getBookingID(),
							pickupPassengerModel.getPickupLatitude(),
							pickupPassengerModel.getPickupLongitude(),
							pickupPassengerModel.getDestinationLatitude(),
							pickupPassengerModel.getDestinationLongitude()
					));
			binding.passengerBookingRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.passengerBookingRecyclerView.setAdapter(pickupPassengerAdapter);

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
							binding.loadingLayout.setVisibility(View.GONE);
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
	private void getVehicleInfo(String fcmToken,
	                            String passengerID,
	                            String bookingID,
	                            Double pickupLatitude,
	                            Double pickupLongitude,
	                            Double destinationLatitude,
	                            Double destinationLongitude) {

		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String getVehicleColor = documentSnapshot.getString("vehicleColor");
						String getVehiclePlateNumber = documentSnapshot.getString("vehiclePlateNumber");
						String getFirstname = documentSnapshot.getString("firstname");
						String getLastname = documentSnapshot.getString("lastname");
						String fullName = getFirstname + " " + getLastname;

						updatePassengerBooking(
								fcmToken,
								passengerID,
								bookingID,
								pickupLatitude,
								pickupLongitude,
								destinationLatitude,
								destinationLongitude,
								fullName,
								getVehicleColor,
								getVehiclePlateNumber
						);
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
	}

	@SuppressLint("LongLogTag")
	private void updatePassengerBooking(String fcmToken,
	                                    String passengerID,
	                                    String bookingID,
	                                    Double pickupLatitude,
	                                    Double pickupLongitude,
	                                    Double destinationLatitude,
	                                    Double destinationLongitude,
	                                    String fullName,
	                                    String vehicleColor,
	                                    String vehiclePlateNumber) {

		//convert to point
		LatLng pickupLatLng = new LatLng(pickupLatitude, pickupLongitude);
		Point pickupCoordinates = Point.fromLngLat(pickupLatLng.longitude, pickupLatLng.latitude);

		LatLng destinationLatLng = new LatLng(destinationLatitude, destinationLongitude);
		Point destinationCoordinates = Point.fromLngLat(destinationLatLng.longitude, destinationLatLng.latitude);

		double distance = DistanceCalculator.calculateDistance(
				pickupLatLng.latitude, pickupLatLng.longitude,
				destinationLatLng.latitude, destinationLatLng.longitude
		);

		long estimatedArrivalTime = DistanceCalculator.calculateArrivalTime(distance);
		long estimatedArrivalMinutes = estimatedArrivalTime / 60000;

		//update booking from passenger
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("bookingStatus", "Driver on the way");
		updateBooking.put("driverUserID", FirebaseMain.getUser().getUid());
		updateBooking.put("driverName", fullName);
		updateBooking.put("vehicleColor", vehicleColor);
		updateBooking.put("vehiclePlateNumber", vehiclePlateNumber);
		updateBooking.put("driverArrivalTime", estimatedArrivalMinutes);

		bookingReference.child(bookingID)
				.updateChildren(updateBooking)
				.addOnSuccessListener(unused -> {

					String notificationMessage = "Vehicle Color: " + vehicleColor
							+ "\n" + "Vehicle plate number: " + vehiclePlateNumber;


					updateDriverStatus(destinationLatitude, destinationLongitude);
					notificationData(fcmToken, notificationMessage);
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

	private void updateDriverStatus(Double destinationLatitude, Double destinationLongitude) {
		if (FirebaseMain.getUser() != null) {

			Map<String, Object> updateDriverStatus = new HashMap<>();
			updateDriverStatus.put("isAvailable", false);
			updateDriverStatus.put("destinationLatitude", destinationLatitude);
			updateDriverStatus.put("destinationLongitude", destinationLongitude);
			updateDriverStatus.put("isNavigatingToDestination", true);

			FirebaseMain.getFireStoreInstance().collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update(updateDriverStatus);
		}
	}
	@SuppressLint("LongLogTag")
	void notificationData(String fcmToken, String message) {
		try {
			JSONArray tokens = new JSONArray();
			tokens.put(fcmToken);

			Log.e(TAG, "notificationData: " + fcmToken);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("driverOTW", message);

			JSONObject body = new JSONObject();
			body.put("data", jsonObject);
			body.put("registration_ids", tokens);

			sendNotification(body.toString());

		} catch (Exception ex) {
			Log.e(TAG, "notificationData: ", ex);
		}
	}

	private void sendNotification(String messageBody) {
		FirebaseMain.getClient().create(APIService.class).sendMessage(
				FirebaseMain.getRemoteMsgHeaders(),
				messageBody
		).enqueue(new Callback<String>() {
			@SuppressLint("LongLogTag")
			@Override
			public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
				if (response.isSuccessful()) {
					try {
						if (response.body() != null) {
							JSONObject responseJSON = new JSONObject(response.body());
							JSONArray results = responseJSON.getJSONArray("results");
							if (responseJSON.getInt("failure") == 1) {
								JSONObject error = (JSONObject) results.get(0);

								return;
							}

							Log.e(TAG, "onResponse: " + response.body());
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					Log.e(TAG, "onResponse: " + response.body());
				}
			}

			@SuppressLint("LongLogTag")
			@Override
			public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
				Log.e(TAG, "onFailure: ", t);
			}
		});
	}
}