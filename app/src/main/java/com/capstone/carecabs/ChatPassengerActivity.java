package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.capstone.carecabs.Adapters.ChatPassengerAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatPassengerModel;
import com.capstone.carecabs.databinding.ActivityChatPassengerBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatPassengerActivity extends AppCompatActivity {
	private final String TAG = "ChatPassengerActivity";
	private ActivityChatPassengerBinding binding;
	private DatabaseReference databaseReference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatPassengerBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		if (getIntent().hasExtra("passengerID")) {
			Intent intent = getIntent();
			String getPassengerID = intent.getStringExtra("passengerID");
			String getBookingID = intent.getStringExtra("bookingID");

			loadPassengerInfo(getPassengerID);

			binding.sendMessageBtn.setOnClickListener(v -> {
				String message = binding.messageEditText.getText().toString();
				if (message.isEmpty()) {
					binding.messageEditText.setText("");
					return;
				} else {
					binding.messageEditText.setText("");
					sendMessage(
							generateRandomChatID(),
							getBookingID,
							getCurrentTimeAndDate(),
							FirebaseMain.getUser().getUid(),
							getPassengerID,
							message
					);
				}
			});
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	private String generateRandomChatID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	@SuppressLint("SetTextI18n")
	private void loadPassengerInfo(String passengerID) {

		readMessage(FirebaseMain.getUser().getUid(), passengerID);

		//load the message receiver profile
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(passengerID);

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot != null && documentSnapshot.exists()) {
						String getFirstname = documentSnapshot.getString("firstname");
						String getLastname = documentSnapshot.getString("lastname");

						binding.passengerNameTextView.setText(getFirstname + " " + getLastname);
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, Objects.requireNonNull(e.getMessage())));
	}

	@SuppressLint("DefaultLocale")
	private String getCurrentTimeAndDate() {
		Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based, so add 1
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		return String.format("%02d-%02d-%04d %02d:%02d:%02d", month, day, year, hour, minute, second);
	}

	private void sendMessage(
			String chatID,
			String bookingID,
			String chatDate,
			String sender,
			String receiver,
			String message
	) {
		databaseReference = FirebaseDatabase.getInstance().getReference();
		ChatPassengerModel chatPassengerModel = new ChatPassengerModel(
				chatID,
				bookingID,
				chatDate,
				sender,
				receiver,
				message,
				"available"
		);
		databaseReference.child(FirebaseMain.chatCollection).push().setValue(chatPassengerModel);
	}

	private void readMessage(String senderID, String receiverID) {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.chatCollection);

		List<ChatPassengerModel> chatPassengerModelList = new ArrayList<>();
		ChatPassengerAdapter chatPassengerAdapter = new ChatPassengerAdapter(ChatPassengerActivity.this, chatPassengerModelList);
		binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		binding.chatRecyclerView.setAdapter(chatPassengerAdapter);

		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					chatPassengerModelList.clear();

					for (DataSnapshot chatPassengerSnapshot : snapshot.getChildren()) {
						ChatPassengerModel chatPassengerModel = chatPassengerSnapshot.getValue(ChatPassengerModel.class);

						if (chatPassengerModel != null) {
							if (chatPassengerModel.getReceiver().equals(receiverID) && chatPassengerModel.getSender().equals(senderID)
									|| chatPassengerModel.getReceiver().equals(senderID) && chatPassengerModel.getSender().equals(receiverID)
							) {
								chatPassengerModelList.add(chatPassengerModel);
							}
						}
					}
					chatPassengerAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}
}