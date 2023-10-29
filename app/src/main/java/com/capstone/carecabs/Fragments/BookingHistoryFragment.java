package com.capstone.carecabs.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.DialogBookingInfoBinding;
import com.capstone.carecabs.databinding.FragmentBookingHistoryBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookingHistoryFragment extends Fragment {
	private final String TAG = "BookingHistoryFragment";
	private AlertDialog bookingInfoDialog;
	private Context context;
	private VoiceAssistant voiceAssistant;
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
		SharedPreferences preferences = Objects.requireNonNull(context).getSharedPreferences("userSettings", Context.MODE_PRIVATE);
		String voiceAssistantToggle = preferences.getString("voiceAssistant", "disabled");

		if (voiceAssistantToggle.equals("enabled")){
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Booking History");
		}

		loadBookingsFromDatabase();

		return view;
	}

	private void loadBookingsFromDatabase() {
		if (FirebaseMain.getUser() != null) {

			DatabaseReference databaseReference = FirebaseDatabase.getInstance()
					.getReference(FirebaseMain.bookingCollection);

			List<BookingsHistoryModel> bookingsHistoryModelList = new ArrayList<>();
			BookingsHistoryAdapter bookingsHistoryAdapter =
					new BookingsHistoryAdapter(
							context,
							bookingsHistoryModelList);
			binding.bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(context));
			binding.bookingHistoryRecyclerView.setAdapter(bookingsHistoryAdapter);
			databaseReference.addValueEventListener(new ValueEventListener() {
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
								&& !bookingsHistoryModel.getBookingStatus().equals("Driver on the way")) {
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