package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.ChatDriverActivity;
import com.capstone.carecabs.Model.ChatOverviewModel;
import com.capstone.carecabs.databinding.ItemChatOverviewBinding;

import java.util.List;

public class ChatOverviewAdapter extends RecyclerView.Adapter<ChatOverviewAdapter.ChatOverviewViewHolder> {
	private Context context;
	private List<ChatOverviewModel> chatOverviewModelList;
	private ChatOverviewOnClickListener chatOverviewOnClickListener;
	public interface ChatOverviewOnClickListener {
		void onChatOverviewItemClick(ChatOverviewModel chatOverviewModel);
	}

	public ChatOverviewAdapter(Context context,
	                           List<ChatOverviewModel> chatOverviewModelList,
	                           ChatOverviewOnClickListener chatOverviewOnClickListener) {
		this.context = context;
		this.chatOverviewModelList = chatOverviewModelList;
		this.chatOverviewOnClickListener = chatOverviewOnClickListener;
	}

	@NonNull
	@Override
	public ChatOverviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemChatOverviewBinding binding = ItemChatOverviewBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ChatOverviewViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull ChatOverviewViewHolder holder, int position) {
		ChatOverviewModel chatOverviewModel = chatOverviewModelList.get(position);

		holder.binding.receiverNameTextView.setText(chatOverviewModel.getReceiver());
		holder.binding.receiverUserTypeTextView.setText(chatOverviewModel.getMessage());

		holder.binding.chatBtn.setOnClickListener(v -> {
			if (chatOverviewOnClickListener != null){
				chatOverviewOnClickListener.onChatOverviewItemClick(chatOverviewModelList.get(position));

				Intent intent = new Intent(context, ChatDriverActivity.class);
				context.startActivity(intent);
			}
		});
	}

	@Override
	public int getItemCount() {
		return chatOverviewModelList.size();
	}

	public class ChatOverviewViewHolder extends RecyclerView.ViewHolder {

		private ItemChatOverviewBinding binding;
		public ChatOverviewViewHolder(@NonNull ItemChatOverviewBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
