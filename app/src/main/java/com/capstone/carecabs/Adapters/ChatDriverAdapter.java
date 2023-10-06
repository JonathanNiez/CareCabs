package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatDriverModel;
import com.capstone.carecabs.R;

import java.util.List;

public class ChatDriverAdapter extends RecyclerView.Adapter<ChatDriverAdapter.ChatDriverViewHolder> {

	private Context context;
	private List<ChatDriverModel> chatDriverModelList;
	private String profilePicture;
	public ChatDriverAdapter(Context context,
	                         List<ChatDriverModel> chatDriverModelList,
	                         String profilePicture) {
		this.context = context;
		this.chatDriverModelList = chatDriverModelList;
		this.profilePicture = profilePicture;
	}

	public static final int MSG_TYPE_LEFT = 0;
	public static final int MSG_TYPE_RIGHT = 1;

	@NonNull
	@Override
	public ChatDriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		if (viewType == MSG_TYPE_RIGHT) {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_right, parent, false);
		} else {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_left, parent, false);
		}
		return new ChatDriverViewHolder(view);
	}


	@Override
	public void onBindViewHolder(@NonNull ChatDriverViewHolder holder, int position) {
		ChatDriverModel chatDriverModel = chatDriverModelList.get(position);

		holder.messageTextView.setText(chatDriverModel.getMessage());

		if (!profilePicture.equals("default")){
			Glide.with(context)
					.load(profilePicture)
					.placeholder(R.drawable.loading_gif)
					.into(holder.chatProfilePicture);
		}

	}

	@Override
	public int getItemCount() {
		return chatDriverModelList.size();
	}

	@Override
	public int getItemViewType(int position) {

		if (chatDriverModelList.get(position).getSender()
				.equals(FirebaseMain.getUser().getUid())) {
			return MSG_TYPE_RIGHT;
		} else {
			return MSG_TYPE_LEFT;
		}
	}

	public class ChatDriverViewHolder extends RecyclerView.ViewHolder {
		private TextView messageTextView;
		private ImageView chatProfilePicture;

		public ChatDriverViewHolder(@NonNull View itemView) {
			super(itemView);

			messageTextView = itemView.findViewById(R.id.messageTextView);
			chatProfilePicture = itemView.findViewById(R.id.chatProfilePicture);
		}
	}

}
