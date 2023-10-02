package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.CurrentPassengerPendingBookingsModel;
import com.capstone.carecabs.databinding.ItemPendingBookingBinding;

import java.util.List;

public class CurrentPassengerPendingBookingsAdapter extends RecyclerView.Adapter<CurrentPassengerPendingBookingsAdapter.BookingHistoryViewHolder> {

	private Context context;
	private List<CurrentPassengerPendingBookingsModel> currentPassengerPendingBookingsModelList;
	private ItemPendingBookingClickListener itemPendingBookingClickListener;

	public interface ItemPendingBookingClickListener {
		void onPendingBookingItemClick(CurrentPassengerPendingBookingsModel currentPassengerPendingBookingsModel);
	}

	public CurrentPassengerPendingBookingsAdapter(
			Context context,
			List<CurrentPassengerPendingBookingsModel> currentPassengerPendingBookingsModelList,
			ItemPendingBookingClickListener itemPendingBookingClickListener
	) {
		this.context = context;
		this.currentPassengerPendingBookingsModelList = currentPassengerPendingBookingsModelList;
		this.itemPendingBookingClickListener = itemPendingBookingClickListener;
	}


	@NonNull
	@Override
	public BookingHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemPendingBookingBinding binding = ItemPendingBookingBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new BookingHistoryViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull CurrentPassengerPendingBookingsAdapter.BookingHistoryViewHolder holder, int position) {
		CurrentPassengerPendingBookingsModel currentPassengerPendingBookingsModel =
				currentPassengerPendingBookingsModelList.get(position);

		holder.binding.currentLocationTextView
				.setText(
						currentPassengerPendingBookingsModel.getCurrentLatitude() + "\n" +
								currentPassengerPendingBookingsModel.getCurrentLongitude()
				);

		holder.binding.destinationLocationTextView
				.setText(
						currentPassengerPendingBookingsModel.getDestinationLatitude() + "\n" +
								currentPassengerPendingBookingsModel.getDestinationLongitude()
				);

		holder.binding.bookingStatusTextView
				.setText("Booking Status: " + currentPassengerPendingBookingsModel.getBookingStatus());

		holder.binding.chatDriverBtn.setOnClickListener(view -> {
			itemPendingBookingClickListener.onPendingBookingItemClick(currentPassengerPendingBookingsModelList.get(position));
		});
	}

	@Override
	public int getItemCount() {
		return currentPassengerPendingBookingsModelList.size();
	}


	public class BookingHistoryViewHolder extends RecyclerView.ViewHolder {

		private final ItemPendingBookingBinding binding;

		public BookingHistoryViewHolder(ItemPendingBookingBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
