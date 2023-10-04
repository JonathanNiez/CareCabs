package com.capstone.carecabs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.ChatDriverAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatDriverModel;
import com.capstone.carecabs.databinding.ActivityChatDriverBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ChatDriverActivity extends AppCompatActivity {
	private final String TAG = "ChatActivity";
	private DatabaseReference databaseReference;
	private ActivityChatDriverBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatDriverBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		if (getIntent().hasExtra("driverID")) {
			Intent intent = getIntent();
			String getDriverID = intent.getStringExtra("driverID");
			String getBookingID = intent.getStringExtra("bookingID");

			loadCurrentChatUser(getDriverID, getBookingID);

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
							getDriverID,
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
	private void loadCurrentChatUser(String driverID, String bookingID) {

		readMessage(FirebaseMain.getUser().getUid(), driverID);

		//load the message receiver profile
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(driverID);

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot != null && documentSnapshot.exists()) {
						String getFirstname = documentSnapshot.getString("firstname");
						String getLastname = documentSnapshot.getString("lastname");

						binding.receiverFullNameTextView.setText(getFirstname + " " + getLastname);
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
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
		ChatDriverModel chatDriverModel = new ChatDriverModel(
				chatID,
				bookingID,
				chatDate,
				sender,
				receiver,
				message,
				"available"
		);
		databaseReference.child(FirebaseMain.chatCollection).push().setValue(chatDriverModel);
	}

	private void readMessage(String senderID, String receiverID) {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.chatCollection);
		databaseReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				List<ChatDriverModel> chatDriverModelList = new ArrayList<>();
				for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
					ChatDriverModel chatDriverModel = dataSnapshot.getValue(ChatDriverModel.class);

					if (chatDriverModel != null) {
						if (chatDriverModel.getReceiver().equals(receiverID) && chatDriverModel.getSender().equals(senderID)
								|| chatDriverModel.getReceiver().equals(senderID) && chatDriverModel.getSender().equals(receiverID)
						) {
							chatDriverModelList.add(chatDriverModel);
						}
					}

				}
				ChatDriverAdapter chatDriverAdapter = new ChatDriverAdapter(ChatDriverActivity.this, chatDriverModelList);
				binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
				binding.chatRecyclerView.setAdapter(chatDriverAdapter);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}

}