package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.TripHistoryModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ItemTripHistoryBinding;

import java.util.List;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {
	private String fonSize = StaticDataPasser.storeFontSize;
	private Context context;
	private List<TripHistoryModel> tripHistoryModelList;
	private OnTripItemClickListener onTripItemClickListener;

	public interface OnTripItemClickListener {
		void onTripItemClick(TripHistoryModel tripHistoryModel);
	}

	public TripHistoryAdapter(Context context,
	                          List<TripHistoryModel> tripHistoryModelList,
	                          OnTripItemClickListener onTripItemClickListener) {
		this.context = context;
		this.tripHistoryModelList = tripHistoryModelList;
		this.onTripItemClickListener = onTripItemClickListener;
	}

	@NonNull
	@Override
	public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemTripHistoryBinding binding = ItemTripHistoryBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new TripViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
		TripHistoryModel tripHistoryModel = tripHistoryModelList.get(position);

		if (fonSize.equals("large")){
			float TEXT_SIZE = 22;

			holder.binding.tripDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			holder.binding.passengerNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.driverNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.passengerTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.pickupLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.destinationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);

		}

		holder.binding.passengerNameTextView.setVisibility(View.GONE);
		holder.binding.passengerTypeTextView.setVisibility(View.GONE);
		holder.binding.driverNameTextView.setVisibility(View.GONE);

		holder.binding.tripDateTextView.setText("Trip date: " + tripHistoryModel.getTripDate());
		holder.binding.pickupLocationTextView.setText("Pickup location:\n" + tripHistoryModel.getPickupLocation());
		holder.binding.destinationTextView.setText("Destination:\n" + tripHistoryModel.getDestination());

		if (!tripHistoryModel.getPassengerType().equals("Senior Citizen") ||
				!tripHistoryModel.getPassengerType().equals("Person with Disabilities (PWD)")) {
			holder.binding.passengerNameTextView.setVisibility(View.VISIBLE);
			holder.binding.passengerTypeTextView.setVisibility(View.VISIBLE);
			holder.binding.passengerTypeTextView.setText(tripHistoryModel.getPassengerType());
			holder.binding.passengerNameTextView.setText(tripHistoryModel.getPassengerName());
		} else {
			holder.binding.driverNameTextView.setVisibility(View.VISIBLE);
			holder.binding.driverNameTextView.setText(tripHistoryModel.getDriverName());
		}

	}

	@Override
	public int getItemCount() {
		return tripHistoryModelList.size();
	}


	public static class TripViewHolder extends RecyclerView.ViewHolder {

		private final ItemTripHistoryBinding binding;

		public TripViewHolder(ItemTripHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
