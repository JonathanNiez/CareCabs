package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.CurrentPassengerBookingsHistoryModel;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.databinding.ItemBookingHistoryBinding;

import java.util.List;

public class CurrentPassengerBookingsHistoryAdapter extends RecyclerView.Adapter<CurrentPassengerBookingsHistoryAdapter.BookingHistoryViewHolder> {

	private Context context;
	private List<CurrentPassengerBookingsHistoryModel> currentPassengerBookingsHistoryModelList;

	public CurrentPassengerBookingsHistoryAdapter(
			Context context,
			List<CurrentPassengerBookingsHistoryModel> currentPassengerBookingsHistoryModelList
	) {
		this.context = context;
		this.currentPassengerBookingsHistoryModelList = currentPassengerBookingsHistoryModelList;
	}


	@NonNull
	@Override
	public BookingHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemBookingHistoryBinding binding = ItemBookingHistoryBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new BookingHistoryViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull CurrentPassengerBookingsHistoryAdapter.BookingHistoryViewHolder holder, int position) {
		CurrentPassengerBookingsHistoryModel currentPassengerBookingsHistoryModel = currentPassengerBookingsHistoryModelList.get(position);

		holder.binding.currentLocationTextView
				.setText(
						currentPassengerBookingsHistoryModel.getCurrentLatitude() + "\n" +
								currentPassengerBookingsHistoryModel.getCurrentLongitude()
				);

		holder.binding.destinationLocationTextView
				.setText(
						currentPassengerBookingsHistoryModel.getDestinationLatitude() + "\n" +
								currentPassengerBookingsHistoryModel.getDestinationLongitude()
				);

		holder.binding.bookingStatusTextView
				.setText("Booking Status: " + currentPassengerBookingsHistoryModel.getBookingStatus());

	}

	@Override
	public int getItemCount() {
		return currentPassengerBookingsHistoryModelList.size();
	}


	public class BookingHistoryViewHolder extends RecyclerView.ViewHolder {

		private final ItemBookingHistoryBinding binding;

		public BookingHistoryViewHolder(ItemBookingHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
