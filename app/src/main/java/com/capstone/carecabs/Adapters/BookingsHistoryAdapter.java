package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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

		//geocode
		MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						bookingsHistoryModel.getPickupLongitude(),
						bookingsHistoryModel.getPickupLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();

		pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onResponse(@NonNull Call<GeocodingResponse> call,
			                       @NonNull Response<GeocodingResponse> response) {
				if (response.isSuccessful()) {
					if (response.body() != null && !response.body().features().isEmpty()) {
						CarmenFeature feature = response.body().features().get(0);
						String locationName = feature.placeName();

						holder.binding.pickupLocationTextView.setText(locationName);

					} else {
						Log.e(TAG, "Geocode error: No features found in the response.");

						holder.binding.pickupLocationTextView.setText("Location not found");
					}
				} else {
					Log.e(TAG, "Geocode error" + response.message());
					holder.binding.pickupLocationTextView.setText("Location not found");

				}
			}

			@SuppressLint("SetTextI18n")
			@Override
			public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
				Log.e(TAG, Objects.requireNonNull(t.getMessage()));

				holder.binding.pickupLocationTextView.setText("Location not found");
			}
		});

		MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						bookingsHistoryModel.getDestinationLongitude(),
						bookingsHistoryModel.getDestinationLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();
		destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onResponse(@NonNull Call<GeocodingResponse> call,
			                       @NonNull Response<GeocodingResponse> response) {
				if (response.isSuccessful()) {
					if (response.body() != null && !response.body().features().isEmpty()) {
						CarmenFeature feature = response.body().features().get(0);
						String locationName = feature.placeName();

						holder.binding.destinationLocationTextView.setText(locationName);
					} else {
						Log.e(TAG, "Geocode error: No features found in the response.");

						holder.binding.destinationLocationTextView.setText("Location not found");
					}
				} else {
					Log.e(TAG, "Geocode error: " + response.message());

					holder.binding.destinationLocationTextView.setText("Location not found");
				}
			}

			@SuppressLint("SetTextI18n")
			@Override
			public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
				Log.e(TAG, Objects.requireNonNull(t.getMessage()));

				holder.binding.destinationLocationTextView.setText("Location not found");
			}
		});

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
