package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.BottomSheetModal.TripRatingsBottomSheet;
import com.capstone.carecabs.Chat.ChatDriverActivity;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemCurrentBookingBinding;
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

public class CurrentBookingAdapter extends RecyclerView.Adapter<CurrentBookingAdapter.CurrentBookingViewHolder> {
	private final String TAG = "CurrentBookingAdapter";
	private Context context;
	private List<CurrentBookingModel> currentBookingModelList;
	private ItemCurrentBookingClickListener itemCurrentBookingClickListener;
	private FragmentActivity fragmentActivity;

	public interface ItemCurrentBookingClickListener {
		void onCurrentBookingClick(CurrentBookingModel currentBookingModel);
	}

	public CurrentBookingAdapter(Context context,
	                             List<CurrentBookingModel> currentBookingModelList,
	                             ItemCurrentBookingClickListener itemCurrentBookingClickListener,
	                             FragmentActivity fragmentActivity) {
		this.context = context;
		this.currentBookingModelList = currentBookingModelList;
		this.itemCurrentBookingClickListener = itemCurrentBookingClickListener;
		this.fragmentActivity = fragmentActivity;
	}

	@NonNull
	@Override
	public CurrentBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemCurrentBookingBinding binding = ItemCurrentBookingBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new CurrentBookingViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull CurrentBookingViewHolder holder, int position) {
		CurrentBookingModel currentBookingModel = currentBookingModelList.get(position);
		holder.binding.driverDetailsLayout.setVisibility(View.GONE);
		holder.binding.rateDriverBtn.setVisibility(View.GONE);

		//geocode
		MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						currentBookingModel.getPickupLongitude(),
						currentBookingModel.getPickupLatitude()))
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

		//TODO:geocode
		MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						currentBookingModel.getDestinationLongitude(),
						currentBookingModel.getDestinationLatitude()))
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

						holder.binding.destinationLocationTextView.setText("Location not found");
					}
				} else {
					Log.e(TAG, "Geocode error" + response.message());

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

		holder.binding.loadingDestinationLocation.setVisibility(View.GONE);
		holder.binding.loadingPickupLocation.setVisibility(View.GONE);

		holder.binding.bookingStatusTextView.setText(currentBookingModel.getBookingStatus());

		if (currentBookingModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.bookingStatusTextView.setVisibility(View.GONE);
			holder.binding.cancelBookingBtn.setVisibility(View.GONE);
			holder.binding.driverDetailsLayout.setVisibility(View.VISIBLE);

			holder.binding.driverArrivalTimeTextView.setText("Arrival time:\nEstimated "
					+ currentBookingModel.getDriverArrivalTime() + " minute(s)");
			holder.binding.vehicleColorTextView.setText("Vehicle Color: " + currentBookingModel.getVehicleColor());
			holder.binding.vehiclePlateNumberTextView.setText("Vehicle plate number: " + currentBookingModel.getVehiclePlateNumber());
			holder.binding.chatDriverBtn.setOnClickListener(v -> {
				Intent intent = new Intent(context, ChatDriverActivity.class);
				intent.putExtra("driverID", currentBookingModel.getDriverUserID());
				context.startActivity(intent);
			});
		} else if (currentBookingModel.getBookingStatus().equals("Transported to destination")) {

			holder.binding.rateDriverBtn.setOnClickListener(v -> {
				Bundle bundle = new Bundle();
				bundle.putString("driverID", currentBookingModel.getDriverUserID());

				TripRatingsBottomSheet bottomSheetFragment = new TripRatingsBottomSheet();
				bottomSheetFragment.show(fragmentActivity.getSupportFragmentManager(), bottomSheetFragment.getTag());
			});

		}

		holder.binding.cancelBookingBtn.setOnClickListener(v -> {
			if (itemCurrentBookingClickListener != null) {
				itemCurrentBookingClickListener
						.onCurrentBookingClick(currentBookingModelList.get(position));
			}
		});
	}

	@Override
	public int getItemCount() {
		return currentBookingModelList.size();
	}

	public class CurrentBookingViewHolder extends RecyclerView.ViewHolder {

		private ItemCurrentBookingBinding binding;

		public CurrentBookingViewHolder(@NonNull ItemCurrentBookingBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
