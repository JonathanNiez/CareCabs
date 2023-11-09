package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.TripModel;
import com.capstone.carecabs.databinding.ItemTripsBinding;

import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
	private Context context;
	private List<TripModel> tripModelList;
	private OnTripItemClickListener onTripItemClickListener;

	public interface OnTripItemClickListener {
		void onTripItemClick(TripModel tripModel);
	}

	public TripAdapter(Context context,
	                   List<TripModel> tripModelList,
	                   OnTripItemClickListener onTripItemClickListener) {
		this.context = context;
		this.tripModelList = tripModelList;
		this.onTripItemClickListener = onTripItemClickListener;
	}

	@NonNull
	@Override
	public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemTripsBinding binding = ItemTripsBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new TripViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
		TripModel tripModel = tripModelList.get(position);

		holder.binding.passengerNameTextView.setVisibility(View.GONE);
		holder.binding.passengerTypeTextView.setVisibility(View.GONE);
		holder.binding.driverNameTextView.setVisibility(View.GONE);

		holder.binding.tripDateTextView.setText("Trip date: " + tripModel.getTripDate());
		holder.binding.pickupLocationTextView.setText("Pickup location:\n" + tripModel.getPickupLocation());
		holder.binding.destinationTextView.setText("Destination:\n" + tripModel.getDestination());

		if (!tripModel.getPassengerType().equals("Senior Citizen") ||
				!tripModel.getPassengerType().equals("Person with Disabilities (PWD)")) {
			holder.binding.passengerNameTextView.setVisibility(View.VISIBLE);
			holder.binding.passengerTypeTextView.setVisibility(View.VISIBLE);
			holder.binding.passengerTypeTextView.setText(tripModel.getPassengerType());
			holder.binding.passengerNameTextView.setText(tripModel.getPassengerName());
		} else {
			holder.binding.driverNameTextView.setVisibility(View.VISIBLE);
			holder.binding.driverNameTextView.setText(tripModel.getDriverName());
		}

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
