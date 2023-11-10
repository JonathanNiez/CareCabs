package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.capstone.carecabs.Adapters.CarouselPagerAdapter;
import com.capstone.carecabs.BookingsActivity;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Chat.ChatDriverActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.HelpActivity;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Map.MapDriverActivity;
import com.capstone.carecabs.Map.MapPassengerActivity;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.PassengerBookingsOverviewActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Register.RegisterDriverActivity;
import com.capstone.carecabs.Register.RegisterPWDActivity;
import com.capstone.carecabs.Register.RegisterSeniorActivity;
import com.capstone.carecabs.RequestLocationPermissionActivity;
import com.capstone.carecabs.TripsActivity;
import com.capstone.carecabs.Utility.LocationPermissionChecker;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.DialogEnableLocationServiceBinding;
import com.capstone.carecabs.databinding.DialogHowToBookBinding;
import com.capstone.carecabs.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "HomeFragment";
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private float textSizeSP;
	private float textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private static final int REQUEST_ENABLE_LOCATION = 1;
	private int currentPage = 0;
	private final long AUTOSLIDE_DELAY = 3000;
	private Handler handler;
	private Runnable runnable;
	private List<Fragment> slideFragments = new ArrayList<>();
	private AlertDialog.Builder builder;
	private AlertDialog noInternetDialog, registerNotCompleteDialog,
			enableLocationServiceDialog, howToBookDialog;
	private Context context;
	private Intent intent;
	private VoiceAssistant voiceAssistant;
	private NetworkChangeReceiver networkChangeReceiver;
	private DocumentReference documentReference;
	private DatabaseReference databaseReference;
	private FragmentManager fragmentManager;
	private FragmentTransaction fragmentTransaction;
	private RequestManager requestManager;
	private FragmentHomeBinding binding;

	@Override
	public void onPause() {
		super.onPause();

		stopAutoSlide();
		closeNoInternetDialog();
		closeRegisterNotCompleteDialog();
		closeEnableLocationServiceDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopAutoSlide();
		closeNoInternetDialog();
		closeRegisterNotCompleteDialog();
		closeEnableLocationServiceDialog();

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (requestManager != null) {
			requestManager.clear(binding.driverProfilePictureImageView);
			requestManager.clear(binding.currentPassengerProfilePictureImageView);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		initializeNetworkChecker();
//		startAutoSlide();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentHomeBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.driverStatsLayout.setVisibility(View.GONE);
		binding.passengerStatsLayout.setVisibility(View.GONE);
		binding.driverOnTheWayLayout.setVisibility(View.GONE);
		binding.currentPassengerLayout.setVisibility(View.GONE);
		binding.transportedToDestinationLayout.setVisibility(View.GONE);
		binding.toDestinationLayout.setVisibility(View.GONE);
		binding.myProfileBadge.setVisibility(View.GONE);
		binding.pickupPassengerBadge.setVisibility(View.GONE);

		context = getContext();
		FirebaseApp.initializeApp(context);
		requestManager = Glide.with(this);

		getUserSettings();

//		slideFragments.add(new CarouselFragment1());
//		slideFragments.add(new CarouselFragment2());
//		slideFragments.add(new CarouselFragment3());
//		slideFragments.add(new CarouselFragment4());

		binding.myProfileBtn.setOnClickListener(v -> {
			goToAccountFragment();
		});

		binding.driverStatusSwitch.setOnCheckedChangeListener((compoundButton, b) ->
				updateDriverStatus(b)
		);

		binding.tripHistoryBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), TripsActivity.class);
			startActivity(intent);
		});

		binding.helpBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), HelpActivity.class);
			startActivity(intent);
		});

//		CarouselPagerAdapter adapter = new CarouselPagerAdapter(getChildFragmentManager(), slideFragments);
//		binding.viewPager.setAdapter(adapter);
//
//		startAutoSlide();
		getCurrentTime();

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		checkUserIfRegisterComplete();
	}

	private void getUserSettings() {

		setFontSize(fontSize);

		if (voiceAssistantState.equals("enabled")) {
			VoiceAssistant voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Home");
		}
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {

		String fontSize = isChecked ? "large" : "normal";

		setFontSize(fontSize);

	}

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.homeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.firstnameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.currentTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.greetTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.totalTripsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.hiTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.rateDriverTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.thankYouRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passengerNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passengerTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passengerDisabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.arrivalTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehicleColorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.vehiclePlateNumberTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.pingedLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.bookingsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.bookARideTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.myProfileTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.tripHistoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverDashBoardTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.availabilityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.driverStatusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.passengerTransportedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.yourTripOverviewTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.onBoardDestinationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.viewOnMapTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.chatDriverTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	@SuppressLint({"DefaultLocale", "SetTextI18n"})
	private void getCurrentTime() {
		LocalDateTime currentTime = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			currentTime = LocalDateTime.now();
		}

		int hour = 0;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			hour = currentTime.getHour();
		}

		String greeting;
		if (hour >= 0 && hour < 12) {
			greeting = "Good Morning!";
			binding.currentTimeImageView.setImageResource(R.drawable.morning_128);

		} else if (hour >= 12 && hour < 18) {
			greeting = "Good Afternoon!";
			binding.currentTimeImageView.setImageResource(R.drawable.afternoon_128);

		} else {
			greeting = "Good Evening!";
			binding.currentTimeImageView.setImageResource(R.drawable.evening_128);

		}

		String amPm = (hour < 12) ? "AM" : "PM";
		if (hour > 12) {
			hour -= 12;
		} else if (hour == 0) {
			hour = 12;
		}

		String formattedTime = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			formattedTime = String.format("%02d:%02d %s", hour, currentTime.getMinute(), amPm);
		}

		binding.greetTextView.setText(greeting);
		binding.currentTimeTextView.setText("The time is " + formattedTime);
	}

//	private void startAutoSlide() {
//
//		if (handler == null) {
//			handler = new Handler();
//		}
//
//		if (runnable == null) {
//			runnable = new Runnable() {
//				@Override
//				public void run() {
//					currentPage = (currentPage + 1) % slideFragments.size();
//					binding.viewPager.setCurrentItem(currentPage, true);
//					handler.postDelayed(this, AUTOSLIDE_DELAY);
//				}
//			};
//		}
//
//		handler.removeCallbacks(runnable);
//		handler.postDelayed(runnable, AUTOSLIDE_DELAY);
//	}

	private void stopAutoSlide() {
		if (handler != null && runnable != null) {
			handler.removeCallbacks(runnable);
		}
	}

	private void updateDriverStatus(boolean isAvailable) {
		if (FirebaseMain.getUser() != null) {
			FirebaseMain.getFireStoreInstance().collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid())
					.update("isAvailable", isAvailable);

			getUserTypeAndLoadUserProfileInfo();
		}
	}

	private void goToEditAccountFragment(Context context) {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new EditAccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void goToAccountFragment() {
		fragmentManager = requireActivity().getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	private void checkUserIfRegisterComplete() {
		if (FirebaseMain.getUser() != null) {
			String getUserID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(getUserID);
			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							boolean getUserRegisterStatus = documentSnapshot.getBoolean("isRegisterComplete");

							if (getUserRegisterStatus) {
								getUserTypeAndLoadUserProfileInfo();

							} else {
								String getRegisterUserType = documentSnapshot.getString("userType");
								showRegisterNotCompleteDialog(getRegisterUserType);
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "checkUserIfRegisterComplete: " + e.getMessage()));

		} else {
			Intent intent = new Intent(context, LoginActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	@SuppressLint("SetTextI18n")
	private void getUserTypeAndLoadUserProfileInfo() {
		if (FirebaseMain.getUser() != null) {

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							binding.progressBarLayout1.setVisibility(View.GONE);
							binding.progressBarLayout2.setVisibility(View.GONE);

							String getUserType = documentSnapshot.getString("userType");
							String getFirstname = documentSnapshot.getString("firstname");
							boolean isVerified = documentSnapshot.getBoolean("isVerified");
							boolean isFirstTimeUser = documentSnapshot.getBoolean("isFirstTimeUser");

							binding.firstnameTextView.setText(getFirstname);

							if (isFirstTimeUser) {
								if (isAdded()) {
									showHowToBookDialog();
								}
							} else {
								binding.hiTextView.setText("Welcome back!");
							}

							if (!isVerified) {
								binding.myProfileBadge.setVisibility(View.VISIBLE);
							}

							if (getUserType != null) {
								switch (getUserType) {
									case "Driver":
										Double getDriverRatings = documentSnapshot.getDouble("driverRatings");
										Long getPassengerTransported = documentSnapshot.getLong("passengersTransported");
										boolean isAvailable = documentSnapshot.getBoolean("isAvailable");
										String getNavigationStatus = documentSnapshot.getString("navigationStatus");

										if (getNavigationStatus.equals("Navigating to destination")) {
											binding.currentPassengerLayout.setVisibility(View.VISIBLE);
											binding.navigationStatusTextView.setText("You are currently navigating to Passenger's Destination");
											showNavigationStatusLayout();

										} else if (getNavigationStatus.equals("Navigating to pickup location")) {
											binding.currentPassengerLayout.setVisibility(View.VISIBLE);
											showNavigationStatusLayout();
										}

										binding.bookingsTextView.setText("Passenger Bookings");
										binding.bookingsBtn.setOnClickListener(v -> {
											intent = new Intent(getActivity(), PassengerBookingsOverviewActivity.class);
											startActivity(intent);
										});

										binding.bookARideTextView.setText("Pickup Passengers");
										binding.driverStatsLayout.setVisibility(View.VISIBLE);
										binding.driverRatingTextView.setText("Your Ratings: " + getDriverRatings);
										binding.passengerTransportedTextView.setText("Passengers\nTransported: " + getPassengerTransported);
										binding.driverStatusTextView.setVisibility(View.VISIBLE);

										if (isAvailable) {
											binding.driverStatusTextView.setTextColor(Color.BLUE);
											binding.driverStatusTextView.setText("Driver Availability: Available");
											binding.driverStatusSwitch.setChecked(true);

										} else {
											binding.driverStatusTextView.setTextColor(Color.RED);
											binding.driverStatusTextView.setText("Driver Availability: Busy");
										}

										break;

									case "Person with Disabilities (PWD)":
									case "Senior Citizen":

										checkBookingStatus();

										binding.bookingsTextView.setText("My Bookings");
										binding.bookingsBtn.setOnClickListener(v -> {
											intent = new Intent(getActivity(), BookingsActivity.class);
											startActivity(intent);
										});

										Long getTotalTrips = documentSnapshot.getLong("totalTrips");
										binding.passengerStatsLayout.setVisibility(View.VISIBLE);
										binding.totalTripsTextView.setText("Total Trips: " + getTotalTrips);

										break;
								}

								binding.bookARideBtn.setOnClickListener(v -> {
									if (LocationPermissionChecker.isLocationPermissionGranted(context)) {
										checkLocationService(getUserType);
									} else {
										intent = new Intent(getActivity(), RequestLocationPermissionActivity.class);
										startActivity(intent);
										Objects.requireNonNull(getActivity()).finish();
									}
								});
							}
						}
					})
					.addOnFailureListener(e -> {
						binding.progressBarLayout1.setVisibility(View.GONE);
						binding.progressBarLayout2.setVisibility(View.GONE);

						Log.e(TAG, "getUserTypeAndLoadProfileInfo: " + e.getMessage());
					});

		} else {
			Intent intent = new Intent(context, LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}

	}

	private void getUserTypeForMap(String userType) {
		if (userType.equals("Driver")) {
			intent = new Intent(getActivity(), MapDriverActivity.class);
		} else {
			intent = new Intent(getActivity(), MapPassengerActivity.class);
		}
		startActivity(intent);
		Objects.requireNonNull(getActivity()).finish();
	}

	private void showNavigationStatusLayout() {
		databaseReference = FirebaseDatabase
				.getInstance().getReference(FirebaseMain.bookingCollection);

		List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();
		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					passengerBookingModelList.clear();

					for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingModel =
								bookingSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingModel != null) {
							if (passengerBookingModel.getDriverUserID()
									.equals(FirebaseMain.getUser().getUid())) {

								if (isAdded()) {
									Glide.with(context)
											.load(passengerBookingModel.getPassengerProfilePicture())
											.placeholder(R.drawable.loading_gif)
											.into(binding.currentPassengerProfilePictureImageView);
								}

								binding.pickupPassengerBadge.setVisibility(View.VISIBLE);

								binding.passengerNameTextView.setText("Passenger Name:\n" + passengerBookingModel.getPassengerName());
								binding.passengerTypeTextView.setText(passengerBookingModel.getPassengerType());
								binding.pickupLocationTextView.setText(passengerBookingModel.getPickupLocation());
								binding.destinationTextView.setText(passengerBookingModel.getDestination());

								binding.viewPassengerOnMapBtn.setOnClickListener(v -> checkLocationService("Driver"));

								if (passengerBookingModel.getPassengerType().equals("Senior Citizen")) {
									binding.passengerDisabilityTextView.setVisibility(View.GONE);
									binding.currentPassengerTypeImageView.setImageResource(R.drawable.senior_64_2);
								} else if (passengerBookingModel.getPassengerType().equals("Person with Disabilities (PWD)")) {
									binding.passengerDisabilityTextView
											.setText("Disability: " + passengerBookingModel.getPassengerDisability());
									binding.currentPassengerTypeImageView.setImageResource(R.drawable.pwd_64);
								}
							}
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, "showNavigationStatusLayout - onCancelled: " + error.getMessage());
			}
		});
	}

	private void checkBookingStatus() {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		List<PassengerBookingModel> passengerBookingModelList = new ArrayList<>();
		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					passengerBookingModelList.clear();
					String userID = FirebaseMain.getUser().getUid();

					for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
						PassengerBookingModel passengerBookingModel =
								bookingSnapshot.getValue(PassengerBookingModel.class);
						if (passengerBookingModel != null) {

							if (isAdded()) {
								Glide.with(context)
										.load(passengerBookingModel.getDriverProfilePicture())
										.placeholder(R.drawable.loading_gif)
										.into(binding.driverProfilePictureImageView);
							}

							if (passengerBookingModel.getPassengerUserID().equals(userID) &&
									passengerBookingModel.getBookingStatus().equals("Driver on the way")) {

								String fcmToken = passengerBookingModel.getFcmToken();
								String driverID = passengerBookingModel.getDriverUserID();

								binding.toDestinationLayout.setVisibility(View.GONE);
								binding.driverOnTheWayLayout.setVisibility(View.VISIBLE);
								binding.bookARideBtn.setVisibility(View.GONE);

								binding.chatDriverBtn.setOnClickListener(v -> {
									intent = new Intent(getActivity(), ChatDriverActivity.class);
									intent.putExtra("driverID", driverID);
									intent.putExtra("fcmToken", fcmToken);
									startActivity(intent);
								});

								binding.arrivalTimeTextView.setText("Arrival Time:\n" + "Estimated " +
										passengerBookingModel.getDriverArrivalTime() + " minute(s)");
								binding.driverNameTextView.setText("Driver name:\n" +
										passengerBookingModel.getDriverName());
								binding.vehicleColorTextView.setText("Vehicle color: " +
										passengerBookingModel.getVehicleColor());
								binding.vehiclePlateNumberTextView.setText("Vehicle plate number: " +
										passengerBookingModel.getVehiclePlateNumber());
								passengerBookingModel.getDriverPingedLocation();
								binding.pingedLocationTextView.setText("Ping location: " +
										passengerBookingModel.getDriverPingedLocation());

								binding.viewOnMapBtn.setOnClickListener(v ->
										checkLocationService(passengerBookingModel.getPassengerType()));

							} else if (passengerBookingModel.getPassengerUserID().equals(userID)
									&& passengerBookingModel.getBookingStatus().equals("Onboard")) {

								binding.driverOnTheWayLayout.setVisibility(View.GONE);
								binding.toDestinationLayout.setVisibility(View.VISIBLE);
								binding.onBoardDestinationTextView.setText(passengerBookingModel.getDestination());
								binding.bookARideBtn.setVisibility(View.GONE);

							} else if (passengerBookingModel.getPassengerUserID().equals(userID)
									&& passengerBookingModel.getRatingStatus().equals("Driver not rated")
									&& passengerBookingModel.getBookingStatus().equals("Transported to destination")) {

								binding.driverOnTheWayLayout.setVisibility(View.GONE);
								binding.toDestinationLayout.setVisibility(View.GONE);
								binding.bookARideBtn.setVisibility(View.VISIBLE);
								binding.transportedToDestinationLayout.setVisibility(View.VISIBLE);
								binding.driverRatedLayout.setVisibility(View.GONE);

								binding.rateDriverBtn.setOnClickListener(v -> {
									if (binding.driverRatingBar.getRating() == 0) {
										return;
									} else {
										rateDriver(passengerBookingModel.getDriverUserID(),
												passengerBookingModel.getBookingID());
									}
								});

							}
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, "checkBookingStatus: onCancelled " + error.getMessage());
			}
		});
	}

	private void rateDriver(String driverID, String bookingID) {

		//update current booking
		DatabaseReference bookingReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.bookingCollection);

		Map<String, Object> updateBooking = new HashMap<>();
		updateBooking.put("ratingStatus", "Driver rated");

		bookingReference.child(bookingID).updateChildren(updateBooking)
				.addOnSuccessListener(unused -> Log.i(TAG, "rateDriver: onSuccess booking updated successfully"))
				.addOnFailureListener(e -> Log.e(TAG, "rateDriver: onFailure " + e.getMessage()));

		//update driver ratings
		DocumentReference driverReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(driverID);

		driverReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						double currentRatings = documentSnapshot.getDouble("driverRatings");
						float newRating = binding.driverRatingBar.getRating();
						double totalRatings = currentRatings + newRating;

						Map<String, Object> updatedData = new HashMap<>();
						updatedData.put("driverRatings", totalRatings);

						driverReference.update(updatedData)
								.addOnSuccessListener(unused -> {
									binding.driverRatedLayout.setVisibility(View.VISIBLE);
									binding.rateDriverLayout.setVisibility(View.GONE);

									Handler handler = new Handler();
									Runnable dismissRunnable = new Runnable() {
										@Override
										public void run() {

											binding.transportedToDestinationLayout.setVisibility(View.GONE);
										}
									};

									handler.postDelayed(dismissRunnable, 2500);
								})
								.addOnFailureListener(e -> {
									showToast("Failed to rate Driver");
									Log.e(TAG, "rateDriver: " + e.getMessage());
								});
					} else {
						showToast("Driver document does not exist");
					}
				}).addOnFailureListener(e -> {
					showToast("Failed to fetch driver ratings");
					Log.e(TAG, "getDriverRatings: " + e.getMessage());
				});
	}

	private void showToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	private void showRegisterNotCompleteDialog(String userType) {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_info_not_complete, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			switch (userType) {
				case "Driver":
					intent = new Intent(getActivity(), RegisterDriverActivity.class);

					break;

				case "Senior Citizen":
					intent = new Intent(getActivity(), RegisterSeniorActivity.class);

					break;

				case "Persons with Disabilities (PWD)":
					intent = new Intent(getActivity(), RegisterPWDActivity.class);

					break;

			}
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		});

		builder.setView(dialogView);

		registerNotCompleteDialog = builder.create();
		registerNotCompleteDialog.show();
	}

	private void closeRegisterNotCompleteDialog() {
		if (registerNotCompleteDialog != null && registerNotCompleteDialog.isShowing()) {
			registerNotCompleteDialog.dismiss();
		}
	}

	private void showHowToBookDialog() {
		builder = new AlertDialog.Builder(context);

		DialogHowToBookBinding binding =
				DialogHowToBookBinding.inflate(getLayoutInflater());
		View dialogView = binding.getRoot();

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Do you need help?");
		}

		binding.nextBtn.setOnClickListener(v -> {
			binding.instructionImageView.setImageResource(R.drawable.book_a_ride_ss);
			binding.titleTextView.setText("Go to Book a Ride");
			binding.bodyTextView.setText("Yeah body");
			binding.previousBtn.setVisibility(View.VISIBLE);
			binding.nextBtn.setVisibility(View.GONE);
		});

		binding.previousBtn.setOnClickListener(v -> {
			binding.instructionImageView.setImageResource(R.drawable.mar);
			binding.titleTextView.setText("HOW TO BOOK A RIDE");
			binding.bodyTextView.setText("Yeah body");
			binding.previousBtn.setVisibility(View.GONE);
			binding.nextBtn.setVisibility(View.VISIBLE);
		});

		binding.closeBtn.setOnClickListener(v -> closeHowToBookDialog());

		binding.dontShowAgainBtn.setOnClickListener(v -> {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			Map<String, Object> updateUserSettings = new HashMap<>();
			updateUserSettings.put("isFirstTimeUser", false);

			documentReference.update(updateUserSettings)
					.addOnSuccessListener(unused -> Log.i(TAG, "showHowToBookDialog - onSuccess: updated isFirstTimeUser to false"))
					.addOnFailureListener(e -> Log.e(TAG, "showHowToBookDialog - onFailure: " + e.getMessage()));
		});

		builder.setView(dialogView);

		howToBookDialog = builder.create();
		howToBookDialog.show();
	}

	private void closeHowToBookDialog() {
		if (howToBookDialog != null && howToBookDialog.isShowing()) {
			howToBookDialog.dismiss();
		}
	}

	private void showNoInternetDialog() {

		builder = new AlertDialog.Builder(getContext());
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

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(context);
			updateConnectionStatus(isConnected);
		}
	}

	private void showEnableLocationServiceDialog() {
		builder = new AlertDialog.Builder(context);

		DialogEnableLocationServiceBinding binding =
				DialogEnableLocationServiceBinding.inflate(getLayoutInflater());
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

	private void checkLocationService(String userType) {
		LocationManager locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
		boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!isGpsEnabled && !isNetworkEnabled) {
			showEnableLocationServiceDialog();
		} else {
			getUserTypeForMap(userType);
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
		getContext().registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(getContext());
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