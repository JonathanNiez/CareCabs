package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.capstone.carecabs.Adapters.ChatAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
	private final String TAG = "ChatActivity";
	private DatabaseReference databaseReference;
	private ActivityChatBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Intent intent = getIntent();
		if (getIntent().hasExtra("driverID")) {
			String getDriverID = intent.getStringExtra("driverID");
			String getTripID = intent.getStringExtra("tripID");

			loadCurrentChatUser(getDriverID);

			binding.sendMessageBtn.setOnClickListener(v -> {
				String message = binding.messageEditText.getText().toString();
				if (message.isEmpty()) {
					binding.messageEditText.setText("");
					return;
				} else {
					binding.messageEditText.setText("");
					sendMessage(
							generateRandomChatID(),
							getTripID,
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
	private void loadCurrentChatUser(String driverID) {

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
			String tripID,
			String chatDate,
			String sender,
			String receiver,
			String message
	) {
		databaseReference = FirebaseDatabase.getInstance().getReference();
		ChatModel chatModel = new ChatModel(
				chatID,
				tripID,
				chatDate,
				sender,
				receiver,
				message
		);
		databaseReference.child(FirebaseMain.chatCollection).push().setValue(chatModel);
	}

	private void readMessage(String senderID, String receiverID) {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.chatCollection);
		databaseReference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				List<ChatModel> chatModelList = new ArrayList<>();
				for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
					ChatModel chatModel = dataSnapshot.getValue(ChatModel.class);

					if (chatModel != null) {
						if (chatModel.getReceiver().equals(receiverID) && chatModel.getSender().equals(senderID)
								|| chatModel.getReceiver().equals(senderID) && chatModel.getSender().equals(receiverID)
						) {
							chatModelList.add(chatModel);
						}
					}

				}
				ChatAdapter chatAdapter = new ChatAdapter(ChatActivity.this, chatModelList);
				binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
				binding.chatRecyclerView.setAdapter(chatAdapter);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}

}