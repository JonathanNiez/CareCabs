package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.BookingsHistoryModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemBookingHistoryBinding;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsHistoryAdapter extends RecyclerView.Adapter<BookingsHistoryAdapter.BookingHistoryViewHolder> {
	private final String TAG = "BookingsHistoryAdapter";

	private Context context;
	private List<BookingsHistoryModel> bookingsHistoryModelList;

	public BookingsHistoryAdapter(
			Context context,
			List<BookingsHistoryModel> bookingsHistoryModelList) {
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

		holder.binding.bookingDateTextView.setText("Booking Date: " + bookingsHistoryModel.getBookingDate());
		holder.binding.pickupLocationTextView.setText(bookingsHistoryModel.getPickupLocation());
		holder.binding.destinationTextView.setText(bookingsHistoryModel.getDestination());

		if (bookingsHistoryModel.getBookingStatus().equals("Waiting")
				|| bookingsHistoryModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.bookingStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.light_blue));
		} else if (bookingsHistoryModel.getBookingStatus().equals("Cancelled")) {
			holder.binding.bookingStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.light_red));
		}

		holder.binding.bookingStatusTextView.setText(bookingsHistoryModel.getBookingStatus());

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
