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

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.ChatPassengerModel;
import com.capstone.carecabs.R;

import java.util.List;

public class ChatPassengerAdapter extends RecyclerView.Adapter<ChatPassengerAdapter.ChatPassengerViewHolder> {
	private Context context;
	private List<ChatPassengerModel> chatPassengerModelList;

	public ChatPassengerAdapter(Context context, List<ChatPassengerModel> chatPassengerModelList) {
		this.context = context;
		this.chatPassengerModelList = chatPassengerModelList;
	}

	public static final int MSG_TYPE_LEFT = 0;
	public static final int MSG_TYPE_RIGHT = 1;

	@NonNull
	@Override
	public ChatPassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		if (viewType == MSG_TYPE_RIGHT) {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_right, parent, false);
		} else {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_chat_left, parent, false);
		}
		return new ChatPassengerViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ChatPassengerViewHolder holder, int position) {
		ChatPassengerModel chatDriverModel = chatPassengerModelList.get(position);

		holder.messageTextView.setText(chatDriverModel.getMessage());

	}

	@Override
	public int getItemCount() {
		return chatPassengerModelList.size();
	}

	@Override
	public int getItemViewType(int position) {

		if (chatPassengerModelList.get(position).getSender()
				.equals(FirebaseMain.getUser().getUid())) {
			return MSG_TYPE_RIGHT;
		} else {
			return MSG_TYPE_LEFT;
		}
	}

	public class ChatPassengerViewHolder extends RecyclerView.ViewHolder {
		private TextView messageTextView;
		private EditText messageEditText;
		private Button sendMessageBtn;
		private ImageView passengerProfilePicture;

		public ChatPassengerViewHolder(@NonNull View itemView) {
			super(itemView);

			messageTextView = itemView.findViewById(R.id.messageTextView);
			messageEditText = itemView.findViewById(R.id.messageEditText);
			sendMessageBtn = itemView.findViewById(R.id.sendMessageBtn);
			passengerProfilePicture = itemView.findViewById(R.id.receiverProfilePicture);

		}
	}
}
