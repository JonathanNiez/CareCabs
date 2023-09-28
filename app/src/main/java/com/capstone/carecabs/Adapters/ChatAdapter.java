package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.util.Log;
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
import com.capstone.carecabs.Model.ChatModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemChatLeftBinding;
import com.capstone.carecabs.databinding.ItemChatRightBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

	private Context context;
	private List<ChatModel> chatModelList;
	public static final int MSG_TYPE_LEFT = 0;
	public static final int MSG_TYPE_RIGHT = 1;

	public ChatAdapter(Context context, List<ChatModel> chatModelList) {
		this.context = context;
		this.chatModelList = chatModelList;
	}

	@NonNull
	@Override
	public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		if (viewType == MSG_TYPE_RIGHT) {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_right, parent, false);
		} else {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_left, parent, false);
		}
		return new ChatViewHolder(view);
	}


	@Override
	public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
		ChatModel chatModel = chatModelList.get(position);

		holder.messageTextView.setText(chatModel.getMessage());


	}

	@Override
	public int getItemCount() {
		return chatModelList.size();
	}

	@Override
	public int getItemViewType(int position) {

		if (chatModelList.get(position).getSender()
				.equals(FirebaseMain.getUser().getUid())) {
			return MSG_TYPE_RIGHT;
		} else {
			return MSG_TYPE_LEFT;
		}
	}

	public class ChatViewHolder extends RecyclerView.ViewHolder {
		private TextView messageTextView;
		private EditText messageEditText;
		private Button sendMessageBtn;
		private ImageView receiverProfilePicture;

		public ChatViewHolder(@NonNull View itemView) {
			super(itemView);

			messageTextView = itemView.findViewById(R.id.messageTextView);
			messageEditText = itemView.findViewById(R.id.messageEditText);
			sendMessageBtn = itemView.findViewById(R.id.sendMessageBtn);
			receiverProfilePicture = itemView.findViewById(R.id.receiverProfilePicture);
		}
	}

}
