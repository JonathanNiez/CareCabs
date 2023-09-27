package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import com.capstone.carecabs.Adapters.ChatAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatModel;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ActivityChatBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
	private DatabaseReference databaseReference;
	private ActivityChatBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.sendMessageBtn.setOnClickListener(v -> {
			String message = binding.messageEditText.getText().toString();
			if (message.isEmpty()) {
				binding.messageEditText.setText("");
				return;
			} else {
				binding.messageEditText.setText("");

				sendMessage(generateRandomChatID(), getCurrentTimeAndDate(), FirebaseMain.getUser().getUid(), null, message);
			}
		});
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	private String generateRandomChatID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
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

	private void sendMessage(String chatID, String chatDate, String sender, String receiver, String message) {

		databaseReference = FirebaseDatabase.getInstance()
				.getReference();

		ChatModel chatModel = new ChatModel(
				chatID,
				null,
				chatDate,
				sender,
				receiver,
				message
		);

		databaseReference.child(StaticDataPasser.chatCollection).push().setValue(chatModel);
	}

	private void readMessage(String senderID, String receiverID, String imageURL) {

		databaseReference = FirebaseDatabase.getInstance()
				.getReference(StaticDataPasser.chatCollection);
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

			}
		});
	}

}