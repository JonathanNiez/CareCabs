package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.databinding.ItemBookingHistoryBinding;

import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.BookingHistoryViewHolder> {
	public interface ItemBookingHistoryClickListener {
		void onBookingHistoryItemClick(PassengerBookingModel passengerBookingModel);
	}

	private final ItemBookingHistoryClickListener itemBookingHistoryClickListener;
	private final List<PassengerBookingModel> passengerBookingModelList;

	private final Context context;

	public BookingHistoryAdapter(Context context,
	                             List<PassengerBookingModel> passengerBookingModelList,
	                             ItemBookingHistoryClickListener itemBookingHistoryClickListener
	) {
		this.passengerBookingModelList = passengerBookingModelList;
		this.context = context;
		this.itemBookingHistoryClickListener = itemBookingHistoryClickListener;
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
	public void onBindViewHolder(@NonNull BookingHistoryAdapter.BookingHistoryViewHolder holder, int position) {
		PassengerBookingModel passengerBookingModel = passengerBookingModelList.get(position);

		holder.binding.currentLocationTextView
				.setText(
						passengerBookingModel.getCurrentLatitude() + "\n" +
								passengerBookingModel.getCurrentLongitude()
				);

		holder.binding.destinationLocationTextView
				.setText(
						passengerBookingModel.getDestinationLatitude() + "\n" +
								passengerBookingModel.getDestinationLongitude()
				);

		holder.binding.bookingStatusTextView
				.setText("Booking Status: " + passengerBookingModel.getBookingStatus());

		holder.itemView.setOnClickListener(view -> {
			itemBookingHistoryClickListener.onBookingHistoryItemClick(passengerBookingModelList.get(position));
		});
	}

	@Override
	public int getItemCount() {
		return passengerBookingModelList.size();
	}


	public class BookingHistoryViewHolder extends RecyclerView.ViewHolder {

		private final ItemBookingHistoryBinding binding;

		public BookingHistoryViewHolder(ItemBookingHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
