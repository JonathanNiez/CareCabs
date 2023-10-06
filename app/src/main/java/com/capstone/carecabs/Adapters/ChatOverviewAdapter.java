package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.ChatDriverActivity;
import com.capstone.carecabs.ChatPassengerActivity;
import com.capstone.carecabs.Model.ChatOverviewModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemChatOverviewBinding;

import java.util.ArrayList;
import java.util.List;

public class ChatOverviewAdapter extends RecyclerView.Adapter<ChatOverviewAdapter.ChatOverviewViewHolder> {
	private Context context;
	private ArrayList<ChatOverviewModel> chatOverviewModelArrayList;
	private ChatOverviewOnClickListener chatOverviewOnClickListener;

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
