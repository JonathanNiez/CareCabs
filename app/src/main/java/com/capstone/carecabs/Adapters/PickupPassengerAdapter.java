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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Chat.ChatPassengerActivity;
import com.capstone.carecabs.Model.PickupPassengerModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemPickupPassengerBinding;
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

public class PickupPassengerAdapter extends RecyclerView.Adapter<PickupPassengerAdapter.PickupPassengerViewHolder> {
	private final String TAG = "PickupPassengerAdapter";
	private Context context;
	private List<PickupPassengerModel> pickupPassengerModelList;
	private PickupPassengerClickListener pickupPassengerClickListener;

	public interface PickupPassengerClickListener {
		void onPickupPassengerItemClick(PickupPassengerModel pickupPassengerModel);
	}

	public PickupPassengerAdapter(Context context,
	                              List<PickupPassengerModel> pickupPassengerModelList,
	                              PickupPassengerClickListener pickupPassengerClickListener) {
		this.context = context;
		this.pickupPassengerModelList = pickupPassengerModelList;
		this.pickupPassengerClickListener = pickupPassengerClickListener;
	}

	@NonNull
	@Override
	public PickupPassengerAdapter.PickupPassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemPickupPassengerBinding binding = ItemPickupPassengerBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new PickupPassengerViewHolder(binding);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull PickupPassengerAdapter.PickupPassengerViewHolder holder, int position) {
		PickupPassengerModel pickupPassengerModel = pickupPassengerModelList.get(position);
		holder.binding.renavigateBtn.setVisibility(View.GONE);
		String fullName = pickupPassengerModel.getPassengerFirstname() + " " + pickupPassengerModel.getPassengerLastname();

		holder.binding.passengerName.setText(fullName);
		if (!pickupPassengerModel.getPassengerProfilePicture().equals("default")) {
			Glide.with(context)
					.load(pickupPassengerModel.getPassengerProfilePicture())
					.placeholder(R.drawable.loading_gif)
					.into(holder.binding.passengerImage);
		}

		switch (pickupPassengerModel.getPassengerUserType()) {
			case "Senior Citizen":
				holder.binding.passengerTypeImage.setImageResource(R.drawable.senior_32);
				break;

			case "Persons with Disability (PWD)":
				holder.binding.passengerTypeImage.setImageResource(R.drawable.pwd_32);
				break;
		}
		holder.binding.passengerType.setText(pickupPassengerModel.getPassengerUserType());
		holder.binding.bookingDate.setText("Booking Date: " + pickupPassengerModel.getBookingDate());

		//geocode
		MapboxGeocoding pickupLocationGeocode = MapboxGeocoding.builder()
				.accessToken(context.getString(R.string.mapbox_access_token))
				.query(Point.fromLngLat(
						pickupPassengerModel.getPickupLongitude(),
						pickupPassengerModel.getPickupLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();

		pickupLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint({"LongLogTag", "SetTextI18n"})
			@Override
			public void onResponse(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                       @androidx.annotation.NonNull Response<GeocodingResponse> response) {
				if (response.isSuccessful()) {
					if (response.body() != null && !response.body().features().isEmpty()) {
						CarmenFeature feature = response.body().features().get(0);
						String locationName = feature.placeName();

						holder.binding.pickupLocationTextView.setText(locationName);
					} else {

						holder.binding.pickupLocationTextView.setText("Location not found");
					}
				} else {
					Log.e(TAG, response.message());
					holder.binding.pickupLocationTextView.setText("Location not found");

				}

			}

			@SuppressLint({"LongLogTag", "SetTextI18n"})
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
						pickupPassengerModel.getDestinationLongitude(),
						pickupPassengerModel.getDestinationLatitude()))
				.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
				.build();
		destinationLocationGeocode.enqueueCall(new Callback<GeocodingResponse>() {
			@SuppressLint({"LongLogTag", "SetTextI18n"})
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

			@SuppressLint({"LongLogTag", "SetTextI18n"})
			@Override
			public void onFailure(@androidx.annotation.NonNull Call<GeocodingResponse> call,
			                      @androidx.annotation.NonNull Throwable t) {
				Log.e(TAG, Objects.requireNonNull(t.getMessage()));

				holder.binding.destinationLocationTextView.setText("Location not found");
			}
		});

		if (pickupPassengerModel.getBookingStatus().equals("Waiting")) {
			holder.binding.bookingStatus.setTextColor(Color.BLUE);
			holder.binding.chatPassengerBtn.setVisibility(View.GONE);
		}
		holder.binding.bookingStatus.setText(pickupPassengerModel.getBookingStatus());

		if (pickupPassengerModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.pickupBtn.setVisibility(View.GONE);
			holder.binding.renavigateBtn.setVisibility(View.VISIBLE);

			holder.binding.renavigateBtn.setOnClickListener(v -> {
				if (pickupPassengerClickListener != null) {
					pickupPassengerClickListener.onPickupPassengerItemClick(pickupPassengerModelList.get(position));
				}
			});

			holder.binding.chatPassengerBtn.setOnClickListener(view -> {
				Intent intent = new Intent(context, ChatPassengerActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("chatUserID", pickupPassengerModel.getPassengerUserID());
				intent.putExtra("bookingID", pickupPassengerModel.getBookingID());
				intent.putExtra("firstname", pickupPassengerModel.getPassengerFirstname());
				intent.putExtra("lastname", pickupPassengerModel.getPassengerLastname());
				intent.putExtra("profilePicture", pickupPassengerModel.getPassengerProfilePicture());
				intent.putExtra("fcmToken", pickupPassengerModel.getFcmToken());
				context.startActivity(intent);
			});
		}

		holder.binding.pickupBtn.setOnClickListener(v -> {
			if (pickupPassengerClickListener != null) {
				pickupPassengerClickListener.onPickupPassengerItemClick(pickupPassengerModelList.get(position));
			}
		});
	}

	@Override
	public int getItemCount() {
		return pickupPassengerModelList.size();
	}

	public class PickupPassengerViewHolder extends RecyclerView.ViewHolder {
		private ItemPickupPassengerBinding binding;

		public PickupPassengerViewHolder(@NonNull ItemPickupPassengerBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
