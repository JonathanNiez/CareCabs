package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.BookingsHistoryModel;
import com.capstone.carecabs.Model.FavoritesModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemBookingHistoryBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsHistoryAdapter
		extends RecyclerView.Adapter<BookingsHistoryAdapter.BookingHistoryViewHolder> {
	private final String TAG = "BookingsHistoryAdapter";
	private Context context;
	private List<BookingsHistoryModel> bookingsHistoryModelList;
	private ItemBookingsHistoryClickListener itemBookingsHistoryClickListener;

	public interface ItemBookingsHistoryClickListener {
		void onItemBookingsHistoryClick(BookingsHistoryModel bookingsHistoryModel);
	}

	public BookingsHistoryAdapter(Context context,
	                              List<BookingsHistoryModel> bookingsHistoryModelList,
	                              ItemBookingsHistoryClickListener itemBookingsHistoryClickListener) {
		this.context = context;
		this.bookingsHistoryModelList = bookingsHistoryModelList;
		this.itemBookingsHistoryClickListener = itemBookingsHistoryClickListener;
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
		holder.binding.destinationTextView.setText(bookingsHistoryModel.getDestination());

		if (bookingsHistoryModel.getBookingStatus().equals("Waiting")
				|| bookingsHistoryModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.bookingStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.light_blue));
		} else if (bookingsHistoryModel.getBookingStatus().equals("Cancelled")) {
			holder.binding.bookingStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.light_red));
		}

		holder.binding.bookingStatusTextView.setText(bookingsHistoryModel.getBookingStatus());

		holder.binding.heartImageView.setOnClickListener(v -> {
			if (itemBookingsHistoryClickListener != null) {
				holder.binding.heartImageView.setImageResource(R.drawable.heart_48);
				itemBookingsHistoryClickListener
						.onItemBookingsHistoryClick(bookingsHistoryModelList.get(position));
			}
		});

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
