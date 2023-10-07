package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Chat.ChatDriverActivity;
import com.capstone.carecabs.Chat.ChatPassengerActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatModel;
import com.capstone.carecabs.Model.ChatOverviewModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemChatOverviewBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatOverviewAdapter extends RecyclerView.Adapter<ChatOverviewAdapter.ChatOverviewViewHolder> {
	private final String TAG = "ChatOverviewAdapter";
	private Context context;
	private ArrayList<ChatOverviewModel> chatOverviewModelArrayList;
	private ChatOverviewOnClickListener chatOverviewOnClickListener;
	private String recentMessage;

	public interface ChatOverviewOnClickListener {
		void onChatOverviewItemClick(ChatOverviewModel chatOverviewModel);
	}

	public ChatOverviewAdapter(Context context,
	                           ArrayList<ChatOverviewModel> chatOverviewModelArrayList,
	                           ChatOverviewOnClickListener chatOverviewOnClickListener) {
		this.context = context;
		this.chatOverviewModelArrayList = chatOverviewModelArrayList;
		this.chatOverviewOnClickListener = chatOverviewOnClickListener;
	}

	@NonNull
	@Override
	public ChatOverviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemChatOverviewBinding binding = ItemChatOverviewBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ChatOverviewViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull ChatOverviewViewHolder holder, int position) {
		ChatOverviewModel chatOverviewModel = chatOverviewModelArrayList.get(position);

		holder.binding.chatNameTextView.setText(chatOverviewModel.getFirstname()
				+ " " + chatOverviewModel.getLastname());
		holder.binding.chatUserTypeTextView.setText(chatOverviewModel.getUserType());

		recentMessage(chatOverviewModel.getUserID(), holder.binding.recentMessageTextView);

		if (!chatOverviewModel.getProfilePicture().equals("default")) {
			Glide.with(context)
					.load(chatOverviewModel.getProfilePicture())
					.placeholder(R.drawable.loading_gif)
					.into(holder.binding.chatProfilePicture);
		}

		holder.binding.chatBtn.setOnClickListener(v -> {
			if (chatOverviewOnClickListener != null) {
				chatOverviewOnClickListener.onChatOverviewItemClick(chatOverviewModelArrayList.get(position));

				Intent intent;
				if (chatOverviewModel.getUserType().equals("Driver")) {
					intent = new Intent(context, ChatDriverActivity.class);

				} else {
					intent = new Intent(context, ChatPassengerActivity.class);
				}
				intent.putExtra("chatUserID", chatOverviewModel.getUserID());
				intent.putExtra("firstname", chatOverviewModel.getFirstname());
				intent.putExtra("lastname", chatOverviewModel.getLastname());
				intent.putExtra("profilePicture", chatOverviewModel.getProfilePicture());
				intent.putExtra("fcmToken", chatOverviewModel.getFcmToken());
				context.startActivity(intent);

			}
		});
	}

	private void recentMessage(String chatUserID, TextView recentMessageTextView) {
		recentMessage = "default";

		String currentUserID = FirebaseMain.getUser().getUid();
		DatabaseReference databaseReference = FirebaseDatabase
				.getInstance().getReference(FirebaseMain.chatCollection);

		databaseReference.addValueEventListener(new ValueEventListener() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
						ChatModel chatModel = dataSnapshot.getValue(ChatModel.class);
						if (chatModel != null) {
							if (chatModel.getReceiver().equals(currentUserID) && chatModel.getSender().equals(chatUserID) ||
									chatModel.getReceiver().equals(chatUserID) && chatModel.getSender().equals(currentUserID)) {

								recentMessage= chatModel.getMessage();
							}
						}
					}

					switch (recentMessage){
						case "default":
							recentMessageTextView.setText("No message");

							break;

						default:
							recentMessageTextView.setText(recentMessage);


							break;
					}
					recentMessage = "default";
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e(TAG, "onCancelled: " + error.getMessage());
			}
		});
	}

	@Override
	public int getItemCount() {
		return chatOverviewModelArrayList.size();
	}

	public class ChatOverviewViewHolder extends RecyclerView.ViewHolder {

		private ItemChatOverviewBinding binding;

		public ChatOverviewViewHolder(@NonNull ItemChatOverviewBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
