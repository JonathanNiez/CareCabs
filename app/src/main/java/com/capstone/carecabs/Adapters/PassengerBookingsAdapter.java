package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.ChatPassengerActivity;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemPassengersBinding;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerBookingsAdapter extends RecyclerView.Adapter<PassengerBookingsAdapter.PassengerViewHolder> {
	private final String TAG = "PassengerBookingsAdapter";
	private Context context;
	private final List<PassengerBookingModel> passengerBookingModelList;
	private ItemPassengerClickListener itemPassengerClickListener;

	public interface ItemPassengerClickListener {
		void onPassengerItemClick(PassengerBookingModel passengerBookingModel);
	}

	public PassengerBookingsAdapter(Context context,
	                                List<PassengerBookingModel> passengerBookingModelList,
	                                ItemPassengerClickListener itemPassengerClickListener) {
		this.context = context;
		this.passengerBookingModelList = passengerBookingModelList;
		this.itemPassengerClickListener = itemPassengerClickListener;
	}

	@NonNull
	@Override
	public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemPassengersBinding binding = ItemPassengersBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new PassengerViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
		PassengerBookingModel passengerBookingModel = passengerBookingModelList.get(position);

		String fullName = passengerBookingModel.getPassengerFirstname() + " " + passengerBookingModel.getPassengerLastname();

		holder.binding.passengerName.setText(fullName);
		if (!passengerBookingModel.getPassengerProfilePicture().equals("default")) {
			Glide.with(context)
					.load(passengerBookingModel.getPassengerProfilePicture())
					.placeholder(R.drawable.loading_gif)
					.into(holder.binding.passengerImage);
		}

		switch (passengerBookingModel.getPassengerUserType()) {
			case "Senior Citizen":
				holder.binding.passengerTypeImage.setImageResource(R.drawable.senior_32);
				break;

			case "Persons with Disability (PWD)":
				holder.binding.passengerTypeImage.setImageResource(R.drawable.pwd_32);
				break;
		}
		holder.binding.passengerType.setText(passengerBookingModel.getPassengerUserType());
		holder.binding.bookingDate.setText("Booking Date: " + passengerBookingModel.getBookingDate());

		//geocode
		MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						passengerBookingModel.getPickupLongitude(),
						passengerBookingModel.getPickupLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();

		pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint("LongLogTag")
			@Override
			public void onResponse(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                       @androidx.annotation.NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && !response.body().features().isEmpty()) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.pickupLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());
					holder.binding.pickupLocationTextView.setText("Location not found");
				}
			}

			@SuppressLint("LongLogTag")
			@Override
			public void onFailure(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                      @androidx.annotation.NonNull Throwable t) {
				Log.e(TAG, Objects.requireNonNull(t.getMessage()));

				holder.binding.pickupLocationTextView.setText("Location not found");
			}
		});

		MapboxGeocoding destinationLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						passengerBookingModel.getDestinationLongitude(),
						passengerBookingModel.getDestinationLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();
		destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint("LongLogTag")
			@Override
			public void onResponse(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                       @androidx.annotation.NonNull Response<GeocodingResponse> response) {
				if (response.body() != null && !response.body().features().isEmpty()) {
					CarmenFeature feature = response.body().features().get(0);
					String locationName = feature.placeName();

					holder.binding.destinationLocationTextView.setText(locationName);
				} else {
					Log.e(TAG, response.message());

					holder.binding.destinationLocationTextView.setText("Location not found");
				}
			}

			@SuppressLint("LongLogTag")
			@Override
			public void onFailure(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                      @androidx.annotation.NonNull Throwable t) {
				Log.e(TAG, Objects.requireNonNull(t.getMessage()));

				holder.binding.destinationLocationTextView.setText("Location not found");
			}
		});

		if (passengerBookingModel.getBookingStatus().equals("Waiting")) {
			holder.binding.bookingStatus.setTextColor(Color.BLUE);
			holder.binding.chatPassengerBtn.setVisibility(View.GONE);
		}
		holder.binding.bookingStatus.setText(passengerBookingModel.getBookingStatus());

		if (passengerBookingModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.viewBtn.setVisibility(View.GONE);

			holder.binding.chatPassengerBtn.setOnClickListener(view -> {
				if (itemPassengerClickListener != null) {
					itemPassengerClickListener.onPassengerItemClick(passengerBookingModelList.get(position));

					Intent intent = new Intent(context, ChatPassengerActivity.class);
					intent.putExtra("passengerID", passengerBookingModel.getPassengerUserID());
					intent.putExtra("bookingID", passengerBookingModel.getBookingID());

					// Add the FLAG_ACTIVITY_NEW_TASK flag
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					// Check if context is an instance of Activity before starting the activity
					if (context instanceof Activity) {
						((Activity) context).startActivity(intent);
					} else {
						// If context is not an Activity, use the application context
						context.getApplicationContext().startActivity(intent);
					}
				}
			});
		}

		holder.binding.viewBtn.setOnClickListener(view -> {
			if (itemPassengerClickListener != null) {
				itemPassengerClickListener.onPassengerItemClick(passengerBookingModelList.get(position));
			}
		});
	}

	@Override
	public int getItemCount() {
		return passengerBookingModelList.size();
	}

	public static class PassengerViewHolder extends RecyclerView.ViewHolder {

		private final ItemPassengersBinding binding;

		public PassengerViewHolder(ItemPassengersBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
