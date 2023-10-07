package com.capstone.carecabs.Chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.capstone.carecabs.Adapters.ChatPassengerAdapter;
import com.capstone.carecabs.Firebase.APIService;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatPassengerModel;
import com.capstone.carecabs.databinding.ActivityChatPassengerBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatPassengerActivity extends AppCompatActivity {
	private final String TAG = "ChatPassengerActivity";
	private ActivityChatPassengerBinding binding;
	private List<ChatPassengerModel> chatPassengerModelList = new ArrayList<>();

	private DatabaseReference databaseReference;

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatPassengerBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		if (getIntent().hasExtra("chatUserID")) {
			Intent intent = getIntent();
			String getPassengerID = intent.getStringExtra("chatUserID");
			String getFirstname = intent.getStringExtra("firstname");
			String getLastname = intent.getStringExtra("lastname");
			String getProfilePicture = intent.getStringExtra("profilePicture");
			String getFCMToken = intent.getStringExtra("fcmToken");

			binding.passengerNameTextView.setText(getFirstname + " " + getLastname);

			readMessage(FirebaseMain.getUser().getUid(), getPassengerID, getProfilePicture);

			binding.sendMessageBtn.setOnClickListener(v -> {
				if (TextUtils.isEmpty(binding.messageEditText.getText().toString())) {
					binding.messageEditText.setText("");
					return;
				} else {
					String message = binding.messageEditText.getText().toString();
					binding.messageEditText.setText("");
					sendMessage(
							getFCMToken,
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
			String fcmToken,
			String chatDate,
			String sender,
			String receiver,
			String message
	) {
		databaseReference = FirebaseDatabase.getInstance().getReference();
		ChatPassengerModel chatPassengerModel = new ChatPassengerModel(
				chatDate,
				sender,
				receiver,
				message,
				"available"
		);
		databaseReference.child(FirebaseMain.chatCollection).push().setValue(chatPassengerModel);
		notificationData(fcmToken, message);
	}

	private void readMessage(String senderID, String receiverID, String profilePicture) {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.chatCollection);

		ChatPassengerAdapter chatPassengerAdapter =
				new ChatPassengerAdapter(
						ChatPassengerActivity.this,
						chatPassengerModelList
						, profilePicture);
		binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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
					binding.chatRecyclerView.smoothScrollToPosition(chatPassengerAdapter.getItemCount() - 1);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}

	void notificationData(String fcmToken, String message) {
		try {
			JSONArray tokens = new JSONArray();
			tokens.put(fcmToken);

			Log.e(TAG, "notificationData: " + fcmToken);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("chat", message);

			JSONObject body = new JSONObject();
			body.put("data", jsonObject);
			body.put("registration_ids", tokens);

			sendNotification(body.toString());

		} catch (Exception ex) {
			Log.e(TAG, "notificationData: ", ex);
		}
	}

	private void sendNotification(String messageBody) {
		FirebaseMain.getClient().create(APIService.class).sendMessage(
				FirebaseMain.getRemoteMsgHeaders(),
				messageBody
		).enqueue(new Callback<String>() {
			@Override
			public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
				if (response.isSuccessful()) {
					try {
						if (response.body() != null) {
							JSONObject responseJSON = new JSONObject(response.body());
							JSONArray results = responseJSON.getJSONArray("results");
							if (responseJSON.getInt("failure") == 1) {
								JSONObject error = (JSONObject) results.get(0);
								//   showToast(error.getString("error"));
								return;
							}

							Log.e(TAG, "onResponse: " + response.body());
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					Log.e(TAG, "onResponse: " + response.body());
				}
			}

			@Override
			public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
				Log.e(TAG, "onFailure: ", t);
			}
		});
	}

}