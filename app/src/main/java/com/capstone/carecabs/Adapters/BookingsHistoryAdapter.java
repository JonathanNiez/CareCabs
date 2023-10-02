package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.BookingsHistoryModel;
import com.capstone.carecabs.databinding.ItemBookingHistoryBinding;

import java.util.List;

public class BookingsHistoryAdapter extends RecyclerView.Adapter<BookingsHistoryAdapter.BookingHistoryViewHolder> {

	private Context context;
	private List<BookingsHistoryModel> bookingsHistoryModelList;

	public BookingsHistoryAdapter(
			Context context,
			List<BookingsHistoryModel> bookingsHistoryModelList)
	{
		this.context = context;
		this.bookingsHistoryModelList = bookingsHistoryModelList;
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
	public void onBindViewHolder(@NonNull BookingsHistoryAdapter.BookingHistoryViewHolder holder, int position) {
		BookingsHistoryModel bookingsHistoryModel = bookingsHistoryModelList.get(position);

		holder.binding.bookingDateTextView.setText(bookingsHistoryModel.getBookingDate());

		holder.binding.currentLocationTextView
				.setText(
						bookingsHistoryModel.getCurrentLatitude() + "\n" +
								bookingsHistoryModel.getCurrentLongitude()
				);

		holder.binding.destinationLocationTextView
				.setText(
						bookingsHistoryModel.getDestinationLatitude() + "\n" +
								bookingsHistoryModel.getDestinationLongitude()
				);

		holder.binding.bookingStatusTextView
				.setText("Booking Status: " + bookingsHistoryModel.getBookingStatus());

	}

	@Override
	public int getItemCount() {
		return bookingsHistoryModelList.size();
	}


	public class BookingHistoryViewHolder extends RecyclerView.ViewHolder {

		private final ItemBookingHistoryBinding binding;

		public BookingHistoryViewHolder(ItemBookingHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
