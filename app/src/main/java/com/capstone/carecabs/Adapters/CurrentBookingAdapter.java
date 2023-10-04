package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.ChatDriverActivity;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemCurrentBookingBinding;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CurrentBookingAdapter extends RecyclerView.Adapter<CurrentBookingAdapter.CurrentBookingViewHolder> {
	private final String TAG = "CurrentBookingAdapter";
	private Context context;
	private List<CurrentBookingModel> currentBookingModelList;
	private ItemCurrentBookingClickListener itemCurrentBookingClickListener;

	public interface ItemCurrentBookingClickListener {
		void onCurrentBookingClick(CurrentBookingModel currentBookingModel);
	}

	public CurrentBookingAdapter(Context context,
	                             List<CurrentBookingModel> currentBookingModelList,
	                             ItemCurrentBookingClickListener itemCurrentBookingClickListener) {
		this.context = context;
		this.currentBookingModelList = currentBookingModelList;
		this.itemCurrentBookingClickListener = itemCurrentBookingClickListener;
	}

	@NonNull
	@Override
	public CurrentBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemCurrentBookingBinding binding = ItemCurrentBookingBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new CurrentBookingViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull CurrentBookingViewHolder holder, int position) {
		CurrentBookingModel currentBookingModel = currentBookingModelList.get(position);
		holder.binding.chatDriverBtn.setVisibility(View.GONE);
		holder.binding.driverOnTheWayTextView.setVisibility(View.GONE);

		//geocode
		MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						currentBookingModel.getPickupLongitude(),
						currentBookingModel.getPickupLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();

		pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(@NonNull Call<GeocodingResponse> call,
			                       @NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && !response.body().features().isEmpty()) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.pickupLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());
					holder.binding.pickupLocationTextView.setText("Location not found");
				}
			}
			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable t) {
				Log.e(TAG, t.getMessage());

				holder.binding.pickupLocationTextView.setText("Location not found");
			}
		});

		MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						currentBookingModel.getDestinationLongitude(),
						currentBookingModel.getDestinationLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();
		destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(@NonNull Call<GeocodingResponse> call,
			                       @NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && !response.body().features().isEmpty()) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.destinationLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());

					holder.binding.destinationLocationTextView.setText("Location not found");
				}
			}

			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable t) {
				Log.e(TAG, t.getMessage());

				holder.binding.destinationLocationTextView.setText("Location not found");
			}
		});

		holder.binding.loadingDestinationLocation.setVisibility(View.GONE);
		holder.binding.loadingPickupLocation.setVisibility(View.GONE);

		holder.binding.bookingStatusTextView.setText(currentBookingModel.getBookingStatus());

		if (currentBookingModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.cancelBookingBtn.setVisibility(View.GONE);
			holder.binding.driverOnTheWayTextView.setVisibility(View.VISIBLE);
			holder.binding.chatDriverBtn.setVisibility(View.VISIBLE);
			holder.binding.chatDriverBtn.setOnClickListener(v -> {
				Intent intent = new Intent(context, ChatDriverActivity.class);
				intent.putExtra("driverID", currentBookingModel.getDriverUserID());
				intent.putExtra("bookingID", currentBookingModel.getBookingID());
				context.startActivity(intent);
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
