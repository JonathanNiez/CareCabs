package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.ChatModel;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

	private Context context;
	private List<ChatModel> chatModelList;

	public ChatAdapter(Context context, List<ChatModel> chatModelList) {
		this.context = context;
		this.chatModelList = chatModelList;
	}

	@NonNull
	@Override
	public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {

	}

	@Override
	public int getItemCount() {
		return chatModelList.size();
	}

	public class ChatViewHolder extends RecyclerView.ViewHolder {

		public ChatViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
}
