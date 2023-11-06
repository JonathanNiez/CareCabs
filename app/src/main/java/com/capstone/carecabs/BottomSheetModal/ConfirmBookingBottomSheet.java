package com.capstone.carecabs.BottomSheetModal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.capstone.carecabs.BookingsActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentConfirmBookingBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmBookingBottomSheet extends BottomSheetDialogFragment {
	private static final String TAG = "ConfirmBookingBottomSheet";
	private static final String ARG_POINT = "point";
	private String pickupLocation, destination;
	private final String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private VoiceAssistant voiceAssistant;
	private int pickupLocationGeocodeResult = 0, destinationGeocodeResult = 0;
	private Context context;
	private Point destinationPoint;
	private FirebaseDatabase firebaseDatabase;
	private DatabaseReference bookingReference;
	private FragmentConfirmBookingBottomSheetBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			String pointJson = getArguments().getString(ARG_POINT);
			if (pointJson != null) {
				destinationPoint = Point.fromJson(pointJson);
			}
		}
	}

	@SuppressLint("SetTextI18n")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentConfirmBookingBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();

		if (
				destinationPoint != null
						&& StaticDataPasser.storePickupLatitude != null
						&& StaticDataPasser.storePickupLongitude != null
		) {

			//destination geocode
			MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
					.accessToken(context.getString(R.string.mapbox_access_token))
					.query(Point.fromLngLat(
							destinationPoint.longitude(),
							destinationPoint.latitude()))
					.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
					.build();
			destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
				@SuppressLint({"SetTextI18n", "LongLogTag"})
				@Override
				public void onResponse(@NonNull Call<GeocodingResponse> call,
				                       @NonNull Response<GeocodingResponse> response) {
					if (response.isSuccessful()) {
						if (response.body() != null && !response.body().features().isEmpty()) {
							CarmenFeature feature = response.body().features().get(0);
							String locationName = feature.placeName();

							destination = locationName;
							binding.destinationTextView.setText(locationName);
							destinationGeocodeResult = 1;
						} else {

							binding.destinationTextView.setText("Location not found");
						}
					} else {
						Log.e(TAG, "Geocode error " + response.message());
						binding.destinationTextView.setText("Location not found");

					}
				}

				@SuppressLint({"SetTextI18n", "LongLogTag"})
				@Override
				public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
					Log.e(TAG, Objects.requireNonNull(t.getMessage()));

					binding.destinationTextView.setText("Location not found");
				}
			});

			//pickup location geocode
			MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
					.accessToken(context.getString(R.string.mapbox_access_token))
					.query(Point.fromLngLat(
							StaticDataPasser.storePickupLongitude,
							StaticDataPasser.storePickupLatitude))
					.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
					.build();

			pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
				@SuppressLint({"SetTextI18n", "LongLogTag"})
				@Override
				public void onResponse(@NonNull Call<GeocodingResponse> call,
				                       @NonNull Response<GeocodingResponse> response) {
					if (response.isSuccessful()) {
						if (response.body() != null && !response.body().features().isEmpty()) {
							CarmenFeature feature = response.body().features().get(0);
							String locationName = feature.placeName();

							pickupLocation = locationName;
							binding.pickupLocationTextView.setText(locationName);
							pickupLocationGeocodeResult = 1;
						} else {

							binding.pickupLocationTextView.setText("Location not found");
						}
					} else {
						Log.e(TAG, "Geocode error " + response.message());
						binding.pickupLocationTextView.setText("Location not found");
					}

				}

				@SuppressLint({"SetTextI18n", "LongLogTag"})
				@Override
				public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
					Log.e(TAG, "onFailure: " + t.getMessage());
					binding.pickupLocationTextView.setText("Location not found");
				}
			});

			String message = "Pickup location: " + pickupLocation + "."
					+ "Destination: " + destination;

			if (voiceAssistantState.equals("enabled")) {
				voiceAssistant = VoiceAssistant.getInstance(context);

				if (pickupLocation != null && destination != null) {
					voiceAssistant.speak(message);
				}
			}

			binding.confirmButton.setOnClickListener(v -> retrieveAndStoreFCMToken(destinationPoint));

		} else {
			binding.pickupLocationTextView.setText("Location not found");
			binding.destinationTextView.setText("Location not found");
		}

		binding.cancelButton.setOnClickListener(v -> {
			dismiss();
		});


		return view;
	}

	private String generateRandomBookingID() {
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

	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	@SuppressLint("LongLogTag")
	private void retrieveAndStoreFCMToken(Point destinationPoint) {
		FirebaseMessaging.getInstance().getToken()
				.addOnSuccessListener(token -> storeCoordinatesInFireStore(destinationPoint, token))
				.addOnFailureListener(e -> Log.e(TAG, "retrieveAndStoreFCMToken: " + e.getMessage()));
	}

	@SuppressLint("LongLogTag")
	private void storeCoordinatesInFireStore(Point destinationPoint, String fcmToken) {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							String getUserType = documentSnapshot.getString("userType");
							String getProfilePicture = documentSnapshot.getString("profilePicture");
							String getFirstName = documentSnapshot.getString("firstname");
							String getLastName = documentSnapshot.getString("lastname");
							String fullName = getFirstName + " " + getLastName;

							if (pickupLocationGeocodeResult == 1 && destinationGeocodeResult == 1) {
								if (getUserType.equals("Senior Citizen")) {

									storeSeniorCitizenBookingToDatabase(
											fcmToken,
											destinationPoint,
											pickupLocation,
											destination,
											fullName,
											getUserType,
											getProfilePicture,
											generateRandomBookingID()
									);
								} else if (getUserType.equals("Person with Disabilities (PWD)")) {
									String getDisability = documentSnapshot.getString("disability");

									storePWDBookingToDatabase(
											fcmToken,
											destinationPoint,
											pickupLocation,
											destination,
											fullName,
											getUserType,
											getProfilePicture,
											getDisability,
											generateRandomBookingID()
									);
								}
							} else {
								showToast(context, "Try again");
								dismiss();
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "storeCoordinatesInFireStore: " + e.getMessage()));
		} else {
			Intent intent = new Intent(context, LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	@SuppressLint("LongLogTag")
	private void storeSeniorCitizenBookingToDatabase(String fcmToken,
	                                                 Point destinationPoint,
	                                                 String pickupLocation,
	                                                 String destination,
	                                                 String fullName,
	                                                 String userType,
	                                                 String profilePicture,
	                                                 String generateBookingID) {

		firebaseDatabase = FirebaseDatabase.getInstance();
		bookingReference = firebaseDatabase
				.getReference(FirebaseMain.bookingCollection).child(generateBookingID);

		Map<String, Object> booking = new HashMap<>();
		booking.put("fcmToken", fcmToken);
		booking.put("passengerUserID", FirebaseMain.getUser().getUid());
		booking.put("bookingID", generateBookingID);
		booking.put("bookingStatus", "Waiting");
		booking.put("pickupLocation", pickupLocation);
		booking.put("pickupLongitude", StaticDataPasser.storePickupLongitude);
		booking.put("pickupLatitude", StaticDataPasser.storePickupLatitude);
		booking.put("destination", destination);
		booking.put("destinationLongitude", destinationPoint.longitude());
		booking.put("destinationLatitude", destinationPoint.latitude());
		booking.put("bookingDate", getCurrentTimeAndDate());
		booking.put("passengerName", fullName);
		booking.put("passengerProfilePicture", profilePicture);
		booking.put("passengerType", userType);

		bookingReference.setValue(booking)
				.addOnSuccessListener(aVoid -> {
					showToast(context, "Booking success");

					openBookingsActivity();

				})
				.addOnFailureListener(e -> {
					showToast(context, "Booking failed");
					Log.e(TAG, "storeSeniorCitizenBookingToDatabase: " + e.getMessage());
				});
	}

	@SuppressLint("LongLogTag")
	private void storePWDBookingToDatabase(String fcmToken,
	                                       Point destinationPoint,
	                                       String pickupLocation,
	                                       String destination,
	                                       String fullName,
	                                       String userType,
	                                       String profilePicture,
	                                       String disability,
	                                       String generateBookingID) {

		firebaseDatabase = FirebaseDatabase.getInstance();
		bookingReference = firebaseDatabase
				.getReference(FirebaseMain.bookingCollection).child(generateBookingID);

		Map<String, Object> booking = new HashMap<>();
		booking.put("fcmToken", fcmToken);
		booking.put("passengerUserID", FirebaseMain.getUser().getUid());
		booking.put("bookingID", generateBookingID);
		booking.put("bookingStatus", "Waiting");
		booking.put("pickupLocation", pickupLocation);
		booking.put("pickupLongitude", StaticDataPasser.storePickupLongitude);
		booking.put("pickupLatitude", StaticDataPasser.storePickupLatitude);
		booking.put("destination", destination);
		booking.put("destinationLongitude", destinationPoint.longitude());
		booking.put("destinationLatitude", destinationPoint.latitude());
		booking.put("bookingDate", getCurrentTimeAndDate());
		booking.put("passengerName", fullName);
		booking.put("passengerProfilePicture", profilePicture);
		booking.put("passengerType", userType);
		booking.put("passengerDisability", disability);

		bookingReference.setValue(booking)
				.addOnSuccessListener(aVoid -> {
					showToast(context, "Booking success");

					openBookingsActivity();
				})
				.addOnFailureListener(e -> {
					showToast(context, "Booking failed");
					Log.e(TAG, "storePWDBookingToDatabase: " + e.getMessage());
				});
	}

	private void openBookingsActivity() {
		dismiss();
		Intent intent = new Intent(context, BookingsActivity.class);
		startActivity(intent);
	}

	public static ConfirmBookingBottomSheet newInstance(String pointJson) {
		ConfirmBookingBottomSheet fragment = new ConfirmBookingBottomSheet();
		Bundle args = new Bundle();
		args.putString(ARG_POINT, pointJson);
		fragment.setArguments(args);
		return fragment;
	}
}