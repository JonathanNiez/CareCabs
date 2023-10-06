package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Adapters.CarouselPagerAdapter;
import com.capstone.carecabs.BookingsActivity;
import com.capstone.carecabs.Firebase.APIService;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.MapDriverActivity;
import com.capstone.carecabs.MapPassengerActivity;
import com.capstone.carecabs.PassengerBookingsOverview;
import com.capstone.carecabs.R;
import com.capstone.carecabs.RegisterDriverActivity;
import com.capstone.carecabs.RegisterPWDActivity;
import com.capstone.carecabs.RegisterSeniorActivity;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
	private final String TAG = "HomeFragment";
	private int currentPage = 0;
	private final long AUTOSLIDE_DELAY = 3000; // Delay in milliseconds (3 seconds)
	private Handler handler;
	private Runnable runnable;
	private List<Fragment> slideFragments = new ArrayList<>();
	private AlertDialog noInternetDialog, registerNotCompleteDialog;
	private AlertDialog.Builder builder;
	private Context context;
	private Intent intent;
	private NetworkChangeReceiver networkChangeReceiver;
	private FragmentHomeBinding binding;
	private DocumentReference documentReference;
	private FragmentManager fragmentManager;
	private FragmentTransaction fragmentTransaction;
	public interface OnFragmentInteractionListener {
		void onFragmentChange(int menuItemId);
	}

	@Override
	public void onPause() {
		super.onPause();

		stopAutoSlide();
		closeNoInternetDialog();
		closeRegisterNotCompleteDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopAutoSlide();
		closeNoInternetDialog();
		closeRegisterNotCompleteDialog();
	}

	@Override
	public void onStart() {
		super.onStart();

		startAutoSlide();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentHomeBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.driverStatsLayout.setVisibility(View.GONE);
		binding.passengerStatsLayout.setVisibility(View.GONE);

		context = getContext();
		initializeNetworkChecker();

//		FirebaseMain.signOutUser();

		FirebaseApp.initializeApp(context);

		slideFragments.add(new CarouselFragment1());
		slideFragments.add(new CarouselFragment2());
		slideFragments.add(new CarouselFragment3());
		slideFragments.add(new CarouselFragment4());

		binding.myProfileBtn.setOnClickListener(v -> {
			goToAccountFragment();
		});

		binding.driverStatusSwitch.setOnCheckedChangeListener((compoundButton, b) ->
				updateDriverStatus(b)
		);

		binding.tripHistoryBtn.setOnClickListener(view1 -> {

		});

		CarouselPagerAdapter adapter = new CarouselPagerAdapter(getChildFragmentManager(), slideFragments);
		binding.viewPager.setAdapter(adapter);

		startAutoSlide();
		getCurrentTime();

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		checkUserIfRegisterComplete();

	}

	private void getCurrentTime() {
		// Get the current time
		LocalDateTime currentTime = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			currentTime = LocalDateTime.now();
		}

		// Get the hour of the day
		int hour = 0;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			hour = currentTime.getHour();
		}

		// Set the greeting message based on the time
		String greeting;
		if (hour >= 0 && hour < 12) {
			greeting = "Good Morning!";
		} else if (hour >= 12 && hour < 18) {
			greeting = "Good Afternoon!";
		} else {
			greeting = "Good Evening!";
		}

		// Display the greeting message and formatted time in a TextView
		String amPm = (hour < 12) ? "AM" : "PM";
		if (hour > 12) {
			hour -= 12;
		} else if (hour == 0) {
			hour = 12;
		}

		// Format the time
		String formattedTime = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			formattedTime = String.format("%02d:%02d %s", hour, currentTime.getMinute(), amPm);
		}

		// Concatenate the greeting message and formatted time
		binding.greetTextView.setText(greeting);
		binding.currentTimeTextView.setText("The time is " + formattedTime);
	}

	private void startAutoSlide() {

		if (handler == null) {
			handler = new Handler();
		}

		if (runnable == null) {
			runnable = new Runnable() {
				@Override
				public void run() {
					currentPage = (currentPage + 1) % slideFragments.size();
					binding.viewPager.setCurrentItem(currentPage, true);
					handler.postDelayed(this, AUTOSLIDE_DELAY);
				}
			};
		}

		handler.removeCallbacks(runnable);
		handler.postDelayed(runnable, AUTOSLIDE_DELAY);
	}

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

			getUserTypeAndLoadProfileInfo();
		}
	}


	public class NotificationReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if ("user_not_verified_action".equals(intent.getAction())) {
//				goToEditAccountFragment(context);
			}
		}

	}

	private void getCurrentFontSizeFromUserSetting() {
		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(FirebaseMain.getUser().getUid());

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot != null && documentSnapshot.exists()) {
						Long getFontSizeLong = documentSnapshot.getLong("fontSize");
						int getFontSize = getFontSizeLong.intValue();

						StaticDataPasser.storeFontSize = getFontSize;

						switch (StaticDataPasser.storeFontSize) {
							case 15:
								binding.driverDashBoardTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.passengerTransportedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.yourTripOverviewTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.totalTripsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

								break;

							case 17:
								binding.driverDashBoardTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.passengerTransportedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.yourTripOverviewTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.totalTripsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								break;

							case 19:
								binding.driverDashBoardTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.passengerTransportedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.yourTripOverviewTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.totalTripsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								break;

							case 21:
								binding.driverDashBoardTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.driverRatingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.passengerTransportedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.totalDistanceTravelledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.yourTripOverviewTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.totalTripsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

								break;
						}
					} else {

						Log.e(TAG, "Not Exist");

					}
				})
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
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
			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null && documentSnapshot.exists()) {
					boolean getUserRegisterStatus = documentSnapshot.getBoolean("isRegisterComplete");
					String getRegisterUserType = documentSnapshot.getString("userType");

					if (getUserRegisterStatus) {

						getUserTypeAndLoadProfileInfo();

					} else {

						showRegisterNotCompleteDialog(getRegisterUserType);

					}
				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));

		} else {
			Intent intent = new Intent(context, LoginActivity.class);
			startActivity(intent);
			getActivity().finish();
		}
	}


	@SuppressLint("SetTextI18n")
	private void getUserTypeAndLoadProfileInfo() {
		if (FirebaseMain.getUser() != null) {

			getCurrentFontSizeFromUserSetting();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null && documentSnapshot.exists()) {
							binding.progressBarLayout1.setVisibility(View.GONE);
							binding.progressBarLayout2.setVisibility(View.GONE);

							String getUserType = documentSnapshot.getString("userType");
							String getFirstName = documentSnapshot.getString("firstname");

							binding.firstnameTextView.setText(getFirstName);

							switch (getUserType) {
								case "Driver":
									Long getDriverRatingsLong = documentSnapshot.getLong("driverRating");
									int getDriverRatings = getDriverRatingsLong.intValue();
									Long getPassengerTransported = documentSnapshot.getLong("passengersTransported");
									boolean getDriverStatus = documentSnapshot.getBoolean("isAvailable");

									binding.bookingsTextView.setText("Passenger Bookings");
									binding.bookingsBtn.setOnClickListener(v -> {
										intent = new Intent(getActivity(), PassengerBookingsOverview.class);
										startActivity(intent);
									});

									binding.bookARideTextView.setText("Pickup Passengers");
									binding.driverStatsLayout.setVisibility(View.VISIBLE);
									binding.driverRatingTextView.setText("Your Ratings: " + getDriverRatings);
									binding.passengerTransportedTextView.setText("Passengers\nTransported: " + getPassengerTransported);
									binding.driverStatusTextView.setVisibility(View.VISIBLE);

									binding.bookARideBtn.setOnClickListener(v -> {
										intent = new Intent(getActivity(), MapDriverActivity.class);
										startActivity(intent);
										Objects.requireNonNull(getActivity()).finish();
									});

									if (getDriverStatus) {
										binding.driverStatusTextView.setTextColor(Color.BLUE);
										binding.driverStatusTextView.setText("Driver Availability: Available");
										binding.driverStatusSwitch.setChecked(true);

									} else {
										binding.driverStatusTextView.setTextColor(Color.RED);
										binding.driverStatusTextView.setText("Driver Availability: Busy");
									}

									break;

								case "Persons with Disability (PWD)":
								case "Senior Citizen":

									binding.bookingsTextView.setText("My Bookings");
									binding.bookingsBtn.setOnClickListener(v -> {
										intent = new Intent(getActivity(), BookingsActivity.class);
										startActivity(intent);
									});

									binding.bookARideBtn.setOnClickListener(v -> {
										intent = new Intent(getActivity(), MapPassengerActivity.class);
										startActivity(intent);
										Objects.requireNonNull(getActivity()).finish();
									});

									Long getTotalTrips = documentSnapshot.getLong("totalTrips");
									binding.passengerStatsLayout.setVisibility(View.VISIBLE);
									binding.totalTripsTextView.setText("Total Trips: " + getTotalTrips);

									break;
							}
						}
					})
					.addOnFailureListener(e -> {
						binding.progressBarLayout1.setVisibility(View.GONE);
						binding.progressBarLayout2.setVisibility(View.GONE);

						Log.e(TAG, Objects.requireNonNull(e.getMessage()));
					});

		} else {
			Intent intent = new Intent(context, LoginActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}

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