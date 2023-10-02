package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

	public CurrentBookingAdapter(Context context,
	                             List<CurrentBookingModel> currentBookingModelList) {
		this.context = context;
		this.currentBookingModelList = currentBookingModelList;
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
			public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && response.body().features() != null) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.pickupLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());
				}
			}

			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable t) {
				Log.e(TAG, t.getMessage());
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
			public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && response.body().features() != null) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.destinationLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());
				}
			}

			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable t) {
				Log.e(TAG, t.getMessage());
			}
		});

		holder.binding.bookingStatusTextView.setText(currentBookingModel.getBookingStatus());
		holder.binding.chatDriverBtn.setOnClickListener(v -> {

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
