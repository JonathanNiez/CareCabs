package com.capstone.carecabs.Chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.ChatOverviewAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatModel;
import com.capstone.carecabs.Model.ChatOverviewModel;
import com.capstone.carecabs.databinding.ActivityChatOverviewBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ChatOverviewActivity extends AppCompatActivity {
	private final String TAG = "ChatOverviewActivity";
	private List<String> stringUsersList;
	private ActivityChatOverviewBinding binding;
	private boolean hasAvailableChats = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatOverviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		FirebaseApp.initializeApp(this);

		binding.noAvailableChatsTextView.setVisibility(View.GONE);

		binding.chatOverviewRecyclerView.setHasFixedSize(true);
		binding.chatOverviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		loadAvailableChats();
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	//TODO:recent chat
	private void loadAvailableChats() {
		stringUsersList = new ArrayList<>();

		DatabaseReference databaseReference = FirebaseDatabase
				.getInstance().getReference(FirebaseMain.chatCollection);

		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					stringUsersList.clear();

					for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
						ChatModel chatModel = chatSnapshot.getValue(ChatModel.class);
						if (chatModel != null) {

							String currentUserID = FirebaseMain.getUser().getUid();
							String senderID = chatModel.getSender();
							String receiverID = chatModel.getReceiver();
							String chatStatus = chatModel.getChatStatus();

							if (chatStatus.equals("available")) {
								if (senderID.equals(currentUserID)) {
									stringUsersList.add(receiverID);
									hasAvailableChats = true;

								}
								if (receiverID.equals(currentUserID)) {
									stringUsersList.add(senderID);
									hasAvailableChats = true;

								}
							}
						}
					}
					if (hasAvailableChats) {
						binding.noAvailableChatsTextView.setVisibility(View.GONE);
						showRecentChats();
					} else {
						binding.noAvailableChatsTextView.setVisibility(View.VISIBLE);
						binding.loadingLayout.setVisibility(View.GONE);
					}
				}else {
					binding.noAvailableChatsTextView.setVisibility(View.VISIBLE);
					binding.loadingLayout.setVisibility(View.GONE);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, error.getMessage());
			}
		});
	}

	@SuppressLint("NotifyDataSetChanged")
	private void showRecentChats() {
		ArrayList<ChatOverviewModel> chatOverviewModelList = new ArrayList<>();

		String currentUserID = FirebaseMain.getUser().getUid();
		CollectionReference collectionReference = FirebaseMain
				.getFireStoreInstance().collection(FirebaseMain.userCollection);

		ChatOverviewAdapter chatOverviewAdapter = new ChatOverviewAdapter(
				this,
				chatOverviewModelList,
				chatOverviewModel -> {
				});
		binding.chatOverviewRecyclerView.setAdapter(chatOverviewAdapter);

		collectionReference.addSnapshotListener((value, error) -> {
			if (value != null) {
				chatOverviewModelList.clear();
				binding.loadingLayout.setVisibility(View.GONE);

				for (QueryDocumentSnapshot recentChatSnapshot : value) {
					ChatOverviewModel chatOverviewModel = recentChatSnapshot.toObject(ChatOverviewModel.class);

					for (String userID : stringUsersList) {
						if (currentUserID.equals(userID) || chatOverviewModel.getUserID().equals(userID)) {
							chatOverviewModelList.add(chatOverviewModel);
							hasAvailableChats = true;
							break;
						}
					}
				}
				chatOverviewAdapter.notifyDataSetChanged();

				if (hasAvailableChats) {
					binding.noAvailableChatsTextView.setVisibility(View.GONE);
				} else {
					binding.noAvailableChatsTextView.setVisibility(View.VISIBLE);
					binding.loadingLayout.setVisibility(View.GONE);
				}
			} else {
				Log.e(TAG, error.getMessage());
			}
		});
	}
}