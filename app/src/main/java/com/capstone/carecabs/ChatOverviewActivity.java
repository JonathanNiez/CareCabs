package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.capstone.carecabs.Adapters.ChatOverviewAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatOverviewModel;
import com.capstone.carecabs.databinding.ActivityChatOverviewBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ChatOverviewActivity extends AppCompatActivity {
	private final String TAG = "ChatOverviewActivity";
	private ActivityChatOverviewBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatOverviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.noAvailableChatsTextView.setVisibility(View.GONE);

		loadAvailableChats();
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	//TODO:recent chat
	private void loadAvailableChats() {
		DatabaseReference databaseReference = FirebaseDatabase
				.getInstance().getReference(FirebaseMain.chatCollection);

		List<ChatOverviewModel> chatOverviewModelList = new ArrayList<>();
		ChatOverviewAdapter chatOverviewAdapter = new ChatOverviewAdapter(this,
				chatOverviewModelList, chatOverviewModel -> {
			//on click
		});
		binding.chatOverviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.chatOverviewRecyclerView.setAdapter(chatOverviewAdapter);
		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					chatOverviewModelList.clear();

					binding.loadingLayout.setVisibility(View.GONE);
					boolean hasAvailableChats = false;

					for (DataSnapshot chatOverviewSnapshot : snapshot.getChildren()) {
						ChatOverviewModel chatOverviewModel = chatOverviewSnapshot.getValue(ChatOverviewModel.class);
						if (chatOverviewModel != null) {

							String chatStatus = chatOverviewModel.getChatStatus();
							String senderID = chatOverviewModel.getSender();
							String receiverID = chatOverviewModel.getReceiver();
							String currentUserID = FirebaseMain.getUser().getUid();
							String getBookingID = chatOverviewModel.getBookingID();

							if ((chatStatus.equals("available") && senderID.equals(currentUserID))
									|| (chatStatus.equals("available") && receiverID.equals(currentUserID))) {

								hasAvailableChats = true;
								chatOverviewModelList.add(chatOverviewModel);

							} else {
								hasAvailableChats = false;

							}
						}
					}
					if (hasAvailableChats) {
						binding.noAvailableChatsTextView.setVisibility(View.GONE);

					} else {
						binding.noAvailableChatsTextView.setVisibility(View.VISIBLE);
					}

					chatOverviewAdapter.notifyDataSetChanged();
				} else {
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
}