package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.carecabs.Adapters.BookingsHistoryAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginOrRegisterActivity;
import com.capstone.carecabs.Model.BookingsHistoryModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.capstone.carecabs.databinding.FragmentBookingHistoryBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BookingHistoryFragment extends Fragment {
	private final String TAG = "BookingHistoryFragment";
	private AlertDialog bookingInfoDialog;
	private Context context;
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantState;
	private FragmentBookingHistoryBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentBookingHistoryBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.noBookingsHistoryTextView.setVisibility(View.GONE);

		context = getContext();

		voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Booking History");
		}

		loadBookingHistoryFromDatabase();

		return view;
	}

	private void loadBookingHistoryFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference bookingReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<BookingsHistoryModel> bookingsHistoryModelList = new ArrayList<>();
			BookingsHistoryAdapter bookingsHistoryAdapter =
					new BookingsHistoryAdapter(
							context,
							bookingsHistoryModelList,
							bookingsHistoryModel ->
									storeFavoriteToFireStore(
											generateRandomFavoritesID(),
											bookingsHistoryModel.getBookingID(),
											bookingsHistoryModel.getDestination(),
											bookingsHistoryModel.getDestinationLongitude(),
											bookingsHistoryModel.getDestinationLatitude()));

			binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.bookingHistoryRecyclerView.setAdapter(bookingsHistoryAdapter);
			bookingReference.addValueEventListener(new ValueEventListener() {
				@SuppressLint("NotifyDataSetChanged")
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {

						bookingsHistoryModelList.clear();
						boolean hasBookingsHistory = false;
						for (DataSnapshot bookingHistorySnapshot : snapshot.getChildren()) {
							BookingsHistoryModel bookingsHistoryModel =
									bookingHistorySnapshot.getValue(BookingsHistoryModel.class);

							if (bookingsHistoryModel != null) {
								if (bookingsHistoryModel.getPassengerUserID().equals(FirebaseMain.getUser().getUid())
										&& !bookingsHistoryModel.getBookingStatus().equals("Waiting")) {
									bookingsHistoryModelList.add(bookingsHistoryModel);
									hasBookingsHistory = true;
								}
							}
						}
						bookingsHistoryAdapter.notifyDataSetChanged();
						if (hasBookingsHistory) {
							binding.noBookingsHistoryTextView.setVisibility(View.GONE);

						} else {
							binding.noBookingsHistoryTextView.setVisibility(View.VISIBLE);
						}
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Log.e(TAG, error.getMessage());
				}
			});

		} else {
			Intent intent = new Intent(getActivity(), LoginOrRegisterActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		}
	}

	private void storeFavoriteToFireStore(String favoriteID,
	                                      String bookingID,
	                                      String destination,
	                                      Double destinationLongitude,
	                                      Double destinationLatitude) {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.favoriteCollection)
					.document(favoriteID);

			Map<String, Object> favorites = new HashMap<>();
			favorites.put("favoriteID", favoriteID);
			favorites.put("userID", FirebaseMain.getUser().getUid());
			favorites.put("bookingID", bookingID);
			favorites.put("destination", destination);
			favorites.put("destinationLongitude", destinationLongitude);
			favorites.put("destinationLatitude", destinationLatitude);

			documentReference.set(favorites)
					.addOnSuccessListener(unused -> {

						Log.i(TAG, "onSuccess: favorite added");
					})
					.addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
		}

	}

	private String generateRandomFavoritesID() {
		String uuid = String.valueOf(UUID.randomUUID());
		return uuid.toString();
	}

	private void showBookingInfoDialog(String bookingID) {

		DialogBookingInfoBinding binding = DialogBookingInfoBinding
				.inflate(getLayoutInflater());

		View dialogView = binding.getRoot();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setView(dialogView);


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