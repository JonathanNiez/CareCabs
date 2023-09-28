package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.databinding.ItemTripsBinding;

import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
	private final Context context;

	private final List<TripModel> tripModelList;

	public TripAdapter(Context context, List<TripModel> tripModelList) {
		this.context = context;
		this.tripModelList = tripModelList;
	}

	@NonNull
	@Override
	public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemTripsBinding binding = ItemTripsBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new TripViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
		TripModel tripModel = tripModelList.get(position);

		holder.binding.tripDateTextView.setText(tripModel.getTripDate());

	}

	@Override
	public int getItemCount() {
		return tripModelList.size();
	}


	public static class TripViewHolder extends RecyclerView.ViewHolder {

		private final ItemTripsBinding binding;

		public TripViewHolder(ItemTripsBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
