package com.capstone.carecabs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.TripHistoryAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.TripHistoryModel;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityTripHistoryBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TripHistoryActivity extends AppCompatActivity {
	private final String TAG = "TripHistoryActivity";
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private AlertDialog noInternetDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityTripHistoryBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeNoInternetDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityTripHistoryBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.backFloatingBtn.setOnClickListener(v -> finish());

		loadTripHistoryFromFireStore();
		getUserSettings();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@SuppressLint("NotifyDataSetChanged")
	private void loadTripHistoryFromFireStore() {
		if (FirebaseMain.getUser() != null) {

			CollectionReference tripReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.tripCollection);

			List<TripHistoryModel> tripHistoryModelList = new ArrayList<>();
			TripHistoryAdapter tripHistoryAdapter = new TripHistoryAdapter(this, tripHistoryModelList, tripHistoryModel -> {
				// Handle item click if needed
			});
			binding.tripsHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
			binding.tripsHistoryRecyclerView.setAdapter(tripHistoryAdapter);

			tripReference.addSnapshotListener((value, error) -> {
				if (error != null) {
					binding.loadingLayout.setVisibility(View.GONE);
					Log.e(TAG, "loadTripHistoryFromFireStore: " + error.getMessage());

					return;
				}

				if (value != null) {
					binding.loadingLayout.setVisibility(View.GONE);
					tripHistoryModelList.clear();
					boolean hasTripHistory = false;
					String userID = FirebaseMain.getUser().getUid();

					for (QueryDocumentSnapshot tripSnapshot : value) {
						TripHistoryModel tripHistoryModel = tripSnapshot.toObject(TripHistoryModel.class);

						if (tripHistoryModel.getDriverUserID().equals(userID)
								|| tripHistoryModel.getPassengerUserID().equals(userID)) {
							tripHistoryModelList.add(tripHistoryModel);
							hasTripHistory = true;
						}
					}
					tripHistoryAdapter.notifyDataSetChanged();

					if (hasTripHistory) {
						binding.noTripHistoryTextView.setVisibility(View.GONE);
						binding.loadingLayout.setVisibility(View.GONE);
					} else {
						binding.noTripHistoryTextView.setVisibility(View.VISIBLE);
						binding.loadingLayout.setVisibility(View.GONE);
					}

				} else {
					Log.e(TAG, "loadTripHistoryFromFireStore: addSnapshotListener is null");
				}
			});
		} else {
			Intent intent = new Intent(this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void getUserSettings() {

		if (voiceAssistantState.equals("enabled")) {
			VoiceAssistant voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Trip history");
		}

		if (fontSize.equals("large")){
			binding.tripHistoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
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