package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Chat.ChatPassengerActivity;
import com.capstone.carecabs.Map.MapDriverActivity;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.PassengerBookingsOverviewActivity;
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
	private Intent intent;

	public interface ItemPassengerClickListener {
		void onPassengerItemClick(PassengerBookingModel passengerBookingModel);
	}

	private ItemPassengerClickListener itemPassengerClickListener;
	private Context context;
	private List<PassengerBookingModel> passengerBookingModelList;
	private PassengerBookingsOverviewActivity passengerBookingsOverviewActivity;

	public PassengerBookingsAdapter(Context context,
	                                List<PassengerBookingModel> passengerBookingModelList,
	                                PassengerBookingsOverviewActivity passengerBookingsOverviewActivity
	) {
		this.context = context;
		this.passengerBookingModelList = passengerBookingModelList;
		this.passengerBookingsOverviewActivity = passengerBookingsOverviewActivity;
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
		holder.binding.chatPassengerBtn.setVisibility(View.GONE);
		String passengerName = passengerBookingModel.getPassengerName();

		holder.binding.passengerNameTextView.setText(passengerName);
		if (!passengerBookingModel.getPassengerProfilePicture().equals("default")) {
			Glide.with(context)
					.load(passengerBookingModel.getPassengerProfilePicture())
					.placeholder(R.drawable.loading_gif)
					.into(holder.binding.passengerImage);
		}

		switch (passengerBookingModel.getPassengerType()) {
			case "Senior Citizen":
				holder.binding.passengerTypeImageView.setImageResource(R.drawable.senior_32);
				break;

			case "Persons with Disability (PWD)":
				holder.binding.passengerTypeImageView.setImageResource(R.drawable.pwd_32);
				break;
		}
		holder.binding.passengerTypeTextView.setText(passengerBookingModel.getPassengerType());
		holder.binding.bookingDateTextView.setText("Booking Date: " + passengerBookingModel.getBookingDate());

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
				if (response.isSuccessful()) {
					if (response.body() != null && !response.body().features().isEmpty()) {
						CarmenFeature feature = response.body().features().get(0);
						String locationName = feature.placeName();

						holder.binding.pickupLocationTextView.setText(locationName);
					} else {
						Log.e(TAG, "onResponse: ");
						holder.binding.pickupLocationTextView.setText("Location not found");
					}
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
				if (response.isSuccessful()) {
					if (response.body() != null && !response.body().features().isEmpty()) {
						CarmenFeature feature = response.body().features().get(0);
						String locationName = feature.placeName();

						holder.binding.destinationLocationTextView.setText(locationName);
					} else {

						holder.binding.destinationLocationTextView.setText("Location not found");
					}
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
			holder.binding.bookingStatusTextView.setTextColor(Color.BLUE);
		}
		holder.binding.bookingStatusTextView.setText(passengerBookingModel.getBookingStatus());

		if (passengerBookingModel.getBookingStatus().equals("Driver on the way")) {
			holder.binding.chatPassengerBtn.setVisibility(View.VISIBLE);
			holder.binding.viewOnMapBtn.setVisibility(View.GONE);

			holder.binding.chatPassengerBtn.setOnClickListener(view -> {
				intent = new Intent(context, ChatPassengerActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("chatUserID", passengerBookingModel.getPassengerUserID());
				intent.putExtra("bookingID", passengerBookingModel.getBookingID());
				intent.putExtra("fullName", passengerBookingModel.getPassengerName());
				intent.putExtra("profilePicture", passengerBookingModel.getPassengerProfilePicture());
				intent.putExtra("fcmToken", passengerBookingModel.getFcmToken());
				context.startActivity(intent);
			});
		}

		holder.binding.viewOnMapBtn.setOnClickListener(view -> {
			if (passengerBookingsOverviewActivity != null) {
				intent = new Intent(context, MapDriverActivity.class);
				context.startActivity(intent);
				passengerBookingsOverviewActivity.finish();
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
