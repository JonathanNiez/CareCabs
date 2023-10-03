package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.CurrentTripModel;
import com.capstone.carecabs.databinding.ItemCurrentTripBinding;

import java.util.List;

public class CurrentTripAdapter extends RecyclerView.Adapter<CurrentTripAdapter.CurrentTripViewHolder> {
	private Context context;
	private List<CurrentTripModel> currentTripModelList;

	public CurrentTripAdapter(Context context, List<CurrentTripModel> currentTripModelList) {
		this.context = context;
		this.currentTripModelList = currentTripModelList;
	}

	@NonNull
	@Override
	public CurrentTripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemCurrentTripBinding binding = ItemCurrentTripBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new CurrentTripViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull CurrentTripViewHolder holder, int position) {
		CurrentTripModel currentTripModel = currentTripModelList.get(position);

		holder.binding.tripDateTextView.setText("Trip date: " + currentTripModel.getTripDate());

	}

	@Override
	public int getItemCount() {
		return currentTripModelList.size();
	}

	public class CurrentTripViewHolder extends RecyclerView.ViewHolder {
		private ItemCurrentTripBinding binding;

		public CurrentTripViewHolder(@NonNull ItemCurrentTripBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
