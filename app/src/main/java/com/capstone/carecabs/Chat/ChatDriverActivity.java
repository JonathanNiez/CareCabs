package com.capstone.carecabs.Chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.ChatDriverAdapter;
import com.capstone.carecabs.Firebase.APIService;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatDriverModel;
import com.capstone.carecabs.Utility.NotificationHelper;
import com.capstone.carecabs.databinding.ActivityChatDriverBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;

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

public class ChatDriverActivity extends AppCompatActivity {
	private final String TAG = "ChatDriverActivity";
	private DatabaseReference databaseReference;
	private List<ChatDriverModel> chatDriverModelList = new ArrayList<>();
	private ActivityChatDriverBinding binding;

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatDriverBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Intent intent;
		if (getIntent().hasExtra("chatUserID")) {
			intent = getIntent();
			String getDriverID = intent.getStringExtra("chatUserID");
			String getFullName = intent.getStringExtra("fullName");
			String getProfilePicture = intent.getStringExtra("profilePicture");
			String getFCMToken = intent.getStringExtra("fcmToken");

			binding.driverFullNameTextView.setText(getFullName);

			readMessage(FirebaseMain.getUser().getUid(), getDriverID, getProfilePicture);

			binding.sendMessageBtn.setOnClickListener(v -> {
				String message = binding.messageEditText.getText().toString();
				if (message.isEmpty()) {
					binding.messageEditText.setText("");
					return;
				} else {
					binding.messageEditText.setText("");
					sendMessage(
							getFCMToken,
							getCurrentTimeAndDate(),
							FirebaseMain.getUser().getUid(),
							getDriverID,
							message
					);

				}
			});
		} else if (getIntent().hasExtra("driverID")) {
			intent = getIntent();
			String getDriverID = intent.getStringExtra("driverID");
			String getFCMToken = intent.getStringExtra("fcmToken");

			loadDriverInfo(getDriverID);

			binding.sendMessageBtn.setOnClickListener(v -> {
				if (TextUtils.isEmpty(binding.messageEditText.getText().toString())) {
					binding.messageEditText.setText("");
					return;
				} else {
					String message = binding.messageEditText.getText().toString();
					binding.messageEditText.setText("");

					sendMessage
							(
									getFCMToken,
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
		super.onBackPressed();
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

	@SuppressLint("SetTextI18n")
	private void loadDriverInfo(String driverID) {
		DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection).document(driverID);

		documentReference.get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String getDriverFirstname = documentSnapshot.getString("firstname");
						String getDriverLastname = documentSnapshot.getString("lastname");
						String getDriverProfilePicture = documentSnapshot.getString("profilePicture");

						binding.driverFullNameTextView.setText(getDriverFirstname + " " + getDriverLastname);

						readMessage(FirebaseMain.getUser().getUid(), driverID, getDriverProfilePicture);
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
	}

	private void sendMessage(
			String fcmToken,
			String chatDate,
			String sender,
			String receiver,
			String message
	) {
		databaseReference = FirebaseDatabase.getInstance().getReference();
		ChatDriverModel chatDriverModel = new ChatDriverModel(
				chatDate,
				sender,
				receiver,
				message,
				"available"
		);
		databaseReference.child(FirebaseMain.chatCollection).push().setValue(chatDriverModel);
		notificationData(fcmToken, message);
	}

	private void readMessage(String senderID, String receiverID, String profilePicture) {
		databaseReference = FirebaseDatabase.getInstance()
				.getReference(FirebaseMain.chatCollection);

		ChatDriverAdapter chatDriverAdapter = new
				ChatDriverAdapter(
				ChatDriverActivity.this,
				chatDriverModelList,
				profilePicture);
		binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.chatRecyclerView.setAdapter(chatDriverAdapter);
		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					chatDriverModelList.clear();

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
					chatDriverAdapter.notifyDataSetChanged();

					if (chatDriverAdapter.getItemCount() >= 4){
						binding.chatRecyclerView.smoothScrollToPosition(chatDriverAdapter.getItemCount() - 1);
					}
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

			Log.d(TAG, "notificationData: " + fcmToken);

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

								Log.e(TAG, "sendNotification: onResponse " + error.toString());
								return;
							}

							Log.d(TAG, "sendNotification: onResponse " + response.body());
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					Log.e(TAG, "sendNotification: onResponse " + response.body());
				}
			}

			@Override
			public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
				Log.e(TAG, "onFailure: ", t);
			}
		});
	}
}