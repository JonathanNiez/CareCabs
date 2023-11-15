package com.capstone.carecabs.BottomSheetModal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Firebase.APIService;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Map.MapDriverActivity;
import com.capstone.carecabs.Model.PickupPassengerBottomSheetData;
import com.capstone.carecabs.Model.RenavigateData;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.DistanceCalculator;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentPickupPassengerBottomSheetBinding;
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

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PickupPassengerBottomSheet extends BottomSheetDialogFragment {

	public static final String TAG = "ModalBottomSheet";
	private static final String ARG_DATA = "data";
	private String fontSize = StaticDataPasser.storeFontSize;

	public interface PickupPassengerBottomSheetListener {
		void onDataReceivedFromPickupPassengerBottomSheet
				(PickupPassengerBottomSheetData pickupPassengerBottomSheetData);
	}

	public interface RenavigateListener {
		void onRenavigateClick(boolean isClicked);
	}

	private RenavigateListener mRenavigateListener;
	private PickupPassengerBottomSheetListener mPickupPassengerBottomSheetListener;
	private Context context;
	private FragmentPickupPassengerBottomSheetBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentPickupPassengerBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		binding.disabilityTextView.setVisibility(View.GONE);
		binding.renavigateBtn.setVisibility(View.GONE);

		binding.closeBtn.setOnClickListener(v -> dismiss());

		return view;

	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (isAdded()) {
			loadPassengerBooking();

			if (fontSize.equals("large")) {
				float TEXT_SIZE = 22;

				binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
				binding.passengerNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
				binding.passengerTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);

				binding.disabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.pickupLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.textView3.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.destinationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.renavigateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.pickupBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
				binding.closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			}
		}
	}

	//show the bottom sheet
	public static PickupPassengerBottomSheet newInstance(String data) {
		PickupPassengerBottomSheet fragment = new PickupPassengerBottomSheet();
		Bundle args = new Bundle();
		args.putString(ARG_DATA, data);
		fragment.setArguments(args);
		return fragment;
	}

	public void setPickupPassengerBottomSheetListener(
			PickupPassengerBottomSheetListener pickupPassengerBottomSheetListener) {
		mPickupPassengerBottomSheetListener = pickupPassengerBottomSheetListener;
	}

	public void setRenavigateListener(RenavigateListener renavigateListener) {
		this.mRenavigateListener = renavigateListener;
	}

	private void sendDataToMap(String bookingID,
	                           String passengerID,
	                           String passengerName,
	                           String passengerType,
	                           String driverName,
	                           String pickupLocation,
	                           Point pickupCoordinates,
	                           String destinationLocation,
	                           Point destinationCoordinates) {

		PickupPassengerBottomSheetData pickupPassengerBottomSheetData =
				new PickupPassengerBottomSheetData(
						bookingID,
						passengerID,
						passengerName,
						passengerType,
						driverName,
						pickupLocation,
						pickupCoordinates,
						destinationLocation,
						destinationCoordinates);

		if (mPickupPassengerBottomSheetListener != null) {
			mPickupPassengerBottomSheetListener.onDataReceivedFromPickupPassengerBottomSheet(pickupPassengerBottomSheetData);
		}
	}


	public void loadPassengerBooking() {
		if (getArguments() != null) {
			String bookingID = getArguments().getString(ARG_DATA);

			if (bookingID != null) {
				DatabaseReference bookingReference = FirebaseDatabase.getInstance()
						.getReference(FirebaseMain.bookingCollection).child(bookingID);
				bookingReference.addListenerForSingleValueEvent(new ValueEventListener() {
					@SuppressLint("SetTextI18n")
					@Override
					public void onDataChange(@NonNull DataSnapshot snapshot) {
						if (snapshot.exists()) {
							String getFCMToken = snapshot.child("fcmToken").getValue(String.class);
							String getPassengerID = snapshot.child("passengerUserID").getValue(String.class);
							String getBookingStatus = snapshot.child("bookingStatus").getValue(String.class);
							String getPassengerProfilePicture = snapshot.child("passengerProfilePicture").getValue(String.class);
							String getPassengerType = snapshot.child("passengerType").getValue(String.class);
							String getPassengerName = snapshot.child("passengerName").getValue(String.class);

							String getPickupLocation = snapshot.child("pickupLocation").getValue(String.class);
							Double getPickupLatitude = snapshot.child("pickupLatitude").getValue(Double.class);
							Double getPickupLongitude = snapshot.child("pickupLongitude").getValue(Double.class);
							String getDestination = snapshot.child("destination").getValue(String.class);
							Double getDestinationLatitude = snapshot.child("destinationLatitude").getValue(Double.class);
							Double getDestinationLongitude = snapshot.child("destinationLongitude").getValue(Double.class);

							binding.pickupLocationTextView.setText(getPickupLocation);
							binding.destinationTextView.setText(getDestination);

							if (getPassengerType != null && getBookingStatus != null) {
								switch (getPassengerType) {
									case "Senior Citizen":
										binding.passengerProfilePic.setImageResource(R.drawable.senior_32);

										break;

									case "Person with Disabilities (PWD)":
										binding.disabilityTextView.setVisibility(View.VISIBLE);
										binding.passengerTypeImageView.setImageResource(R.drawable.pwd_32);
										String getPassengerDisability = snapshot.child("passengerDisability").getValue(String.class);

										binding.disabilityTextView.setText("Disability:\n" + getPassengerDisability);

										break;
								}

								switch (getBookingStatus) {
									case "Waiting":
										binding.renavigateBtn.setVisibility(View.GONE);
										binding.pickupBtn.setOnClickListener(v -> {

											getDriverAndVehicleInfo(
													getFCMToken,
													getPassengerID,
													getPassengerName,
													getPassengerType,
													bookingID,
													getPickupLocation,
													getPickupLatitude,
													getPickupLongitude,
													getDestination,
													getDestinationLatitude,
													getDestinationLongitude);

										});

										break;

									case "Driver on the way":
										binding.pickupBtn.setVisibility(View.GONE);
										binding.renavigateBtn.setVisibility(View.VISIBLE);
										binding.renavigateBtn.setOnClickListener(v -> {

											//convert to point
											LatLng pickupLatLng =
													new LatLng(getPickupLatitude, getPickupLongitude);
											Point pickupCoordinates =
													Point.fromLngLat(pickupLatLng.longitude,
															pickupLatLng.latitude);

											LatLng destinationLatLng =
													new LatLng(getDestinationLatitude, getDestinationLongitude);
											Point destinationCoordinates =
													Point.fromLngLat(destinationLatLng.longitude,
															destinationLatLng.latitude);

											//renavigate
											if (mRenavigateListener != null) {
												mRenavigateListener.onRenavigateClick(true);

												sendDataToMap(bookingID,
														getPassengerID,
														getPassengerName,
														getPassengerType,
														"none",
														getPickupLocation,
														pickupCoordinates,
														getDestination,
														destinationCoordinates);

												dismiss();
											}
										});
										break;
								}
							}

							if (getPassengerProfilePicture != null && !getPassengerProfilePicture.equals("default")) {
								Glide.with(context)
										.load(getPassengerProfilePicture)
										.placeholder(R.drawable.loading_gif)
										.into(binding.passengerProfilePic);
							}

							binding.passengerNameTextView.setText(getPassengerName);
							binding.passengerTypeTextView.setText(getPassengerType);
						}
					}

					@SuppressLint("LongLogTag")
					@Override
					public void onCancelled(@NonNull DatabaseError error) {
						Log.e(TAG, "loadPassengerBooking: " + error.getMessage());
					}
				});

			}
		}
	}

	private void getDriverAndVehicleInfo(String fcmToken,
	                                     String passengerID,
	                                     String passengerName,
	                                     String passengerType,
	                                     String bookingID,
	                                     String pickupLocation,
	                                     Double pickupLatitude,
	                                     Double pickupLongitude,
	                                     String destinationLocation,
	                                     Double destinationLatitude,
	                                     Double destinationLongitude) {

		DocumentReference driverReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		driverReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String getVehicleColor = documentSnapshot.getString("vehicleColor");
						String getVehiclePlateNumber = documentSnapshot.getString("vehiclePlateNumber");
						String getFirstname = documentSnapshot.getString("firstname");
						String getLastname = documentSnapshot.getString("lastname");
						String getProfilePicture = documentSnapshot.getString("profilePicture");
						String fullName = getFirstname + " " + getLastname;

						updatePassengerBooking(fcmToken,
								passengerID,
								passengerName,
								passengerType,
								bookingID,
								pickupLocation,
								pickupLatitude,
								pickupLongitude,
								destinationLocation,
								destinationLatitude,
								destinationLongitude,
								fullName,
								getProfilePicture,
								getVehicleColor,
								getVehiclePlateNumber);
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
	}

	private void updatePassengerBooking(String fcmToken,
	                                    String passengerID,
	                                    String passengerName,
	                                    String passengerType,
	                                    String bookingID,
	                                    String pickupLocation,
	                                    Double pickupLatitude,
	                                    Double pickupLongitude,
	                                    String destinationLocation,
	                                    Double destinationLatitude,
	                                    Double destinationLongitude,
	                                    String driverName,
	                                    String profilePicture,
	                                    String vehicleColor,
	                                    String vehiclePlateNumber) {

		//convert to point
		LatLng pickupLatLng = new LatLng(pickupLatitude, pickupLongitude);
		Point pickupCoordinates = Point.fromLngLat(pickupLatLng.longitude, pickupLatLng.latitude);

		LatLng destinationLatLng = new LatLng(destinationLatitude, destinationLongitude);
		Point destinationCoordinates = Point.fromLngLat(destinationLatLng.longitude, destinationLatLng.latitude);

		//calculate driver arrival time
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
		updateBooking.put("driverName", driverName);
		updateBooking.put("driverProfilePicture", profilePicture);
		updateBooking.put("vehicleColor", vehicleColor);
		updateBooking.put("vehiclePlateNumber", vehiclePlateNumber);
		updateBooking.put("driverArrivalTime", estimatedArrivalMinutes);

		bookingReference.child(bookingID).updateChildren(updateBooking)
				.addOnSuccessListener(unused -> {

					String notificationMessage = "Vehicle Color: " + vehicleColor
							+ "\n" + "Vehicle plate number: " + vehiclePlateNumber;

					updateDriverStatus(passengerType,
							bookingID,
							pickupLocation,
							pickupLatitude,
							pickupLongitude);

					notificationData(fcmToken, notificationMessage);

					sendDataToMap(bookingID,
							passengerID,
							passengerName,
							passengerType,
							driverName,
							pickupLocation,
							pickupCoordinates,
							destinationLocation,
							destinationCoordinates);

					dismiss();

				})
				.addOnFailureListener(e -> {

					Toast.makeText(context,
							"Booking Failed to Accept", Toast.LENGTH_LONG).show();

					Log.e(TAG, "updatePassengerBooking: " + e.getMessage());
				});
	}

	private void updateDriverStatus(String passengerType,
	                                String bookingID,
	                                String pickupLocation,
	                                Double pickupLatitude,
	                                Double pickupLongitude) {

		if (FirebaseMain.getUser() != null) {

			DocumentReference driverReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			Map<String, Object> updateDriverStatus = new HashMap<>();
			updateDriverStatus.put("isAvailable", false);
			updateDriverStatus.put("pickupLocation", pickupLocation);
			updateDriverStatus.put("pickupLatitude", pickupLatitude);
			updateDriverStatus.put("pickupLongitude", pickupLongitude);
			updateDriverStatus.put("navigationStatus", "Navigating to pickup location");
			updateDriverStatus.put("passengerType", passengerType);
			updateDriverStatus.put("bookingID", bookingID);

			driverReference.update(updateDriverStatus)
					.addOnSuccessListener(unused -> Log.i(TAG, "updateDriverStatus: driverReference updated successfully"))
					.addOnFailureListener(e -> Log.e(TAG, "updateDriverStatus - onFailure: " + e.getMessage()));

		}
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

			@Override
			public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
				Log.e(TAG, "onFailure: ", t);
			}
		});
	}

	private String generateRandomTripID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

//	private void storeTripToDatabase(
//			String generateTripID,
//			String bookingID,
//			String passengerID,
//			Double pickupLatitude,
//			Double pickupLongitude,
//			Double destinationLatitude,
//			Double destinationLongitude
//	) {
//
//		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
//				.collection(FirebaseMain.tripCollection).document(generateTripID);
//
//		TripModel tripModel = new TripModel(
//				generateTripID,
//				false,
//				bookingID,
//				"Ongoing",
//				FirebaseMain.getUser().getUid(),
//				passengerID,
//				getCurrentTimeAndDate(),
//				pickupLatitude,
//				pickupLongitude,
//				destinationLatitude,
//				destinationLongitude
//		);
//
//		documentReference.set(tripModel)
//				.addOnSuccessListener(unused -> {
//					Toast.makeText(context,
//							"Booking accepted", Toast.LENGTH_LONG).show();
//
//				})
//				.addOnFailureListener(e -> {
//					Toast.makeText(context,
//							"Booking failed to accept", Toast.LENGTH_LONG).show();
//
//					Log.e(TAG, "storeTripToDatabase: " + e.getMessage());
//				});
//	}
}