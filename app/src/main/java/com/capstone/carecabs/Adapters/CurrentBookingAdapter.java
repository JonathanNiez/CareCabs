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
import com.capstone.carecabs.Firebase.FirebaseMain;
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
		holder.binding.rateYourDriverTextView.setVisibility(View.GONE);
		holder.binding.loadingDestinationLocation.setVisibility(View.GONE);
		holder.binding.loadingPickupLocation.setVisibility(View.GONE);
		holder.binding.pickupLocationTextView.setText(currentBookingModel.getPickupLocation());
		holder.binding.destinationTextView.setText(currentBookingModel.getDestination());
		holder.binding.bookingStatusTextView.setText(currentBookingModel.getBookingStatus());

		final String userID = FirebaseMain.getUser().getUid();

		if (
				currentBookingModel.getPassengerUserID().equals(userID) &&
						currentBookingModel.getBookingStatus().equals("Driver on the way")
		) {

			holder.binding.bookingStatusTextView.setVisibility(View.GONE);
			holder.binding.cancelBookingBtn.setVisibility(View.GONE);
			holder.binding.driverDetailsLayout.setVisibility(View.VISIBLE);

			holder.binding.driverArrivalTimeTextView.setText("Arrival time:\nEstimated "
					+ currentBookingModel.getDriverArrivalTime() + " minute(s)");
			holder.binding.vehicleColorTextView
					.setText("Vehicle Color: " + currentBookingModel.getVehicleColor());
			holder.binding.vehiclePlateNumberTextView
					.setText("Vehicle plate number: " + currentBookingModel.getVehiclePlateNumber());

			holder.binding.chatDriverBtn.setOnClickListener(v -> {
				Intent intent = new Intent(context, ChatDriverActivity.class);
				intent.putExtra("driverID", currentBookingModel.getDriverUserID());
				context.startActivity(intent);
			});

		} else if (
				currentBookingModel.getPassengerUserID().equals(userID) &&
						currentBookingModel.getBookingStatus().equals("Transported to destination") &&
						currentBookingModel.getRatingStatus().equals("driver not rated")
		) {

			holder.binding.cancelBookingBtn.setVisibility(View.GONE);
			holder.binding.rateYourDriverTextView.setVisibility(View.VISIBLE);
			holder.binding.rateDriverBtn.setVisibility(View.VISIBLE);

			holder.binding.rateDriverBtn.setOnClickListener(v -> {
				Bundle bundle = new Bundle();
				bundle.putString("driverID", currentBookingModel.getDriverUserID());
				bundle.putString("bookingID", currentBookingModel.getBookingID());

				TripRatingsBottomSheet tripRatingsBottomSheet = new TripRatingsBottomSheet();
				tripRatingsBottomSheet.setArguments(bundle);
				tripRatingsBottomSheet.show(fragmentActivity.getSupportFragmentManager(),
						tripRatingsBottomSheet.getTag());
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

	public static class CurrentBookingViewHolder extends RecyclerView.ViewHolder {

		private final ItemCurrentBookingBinding binding;

		public CurrentBookingViewHolder(@NonNull ItemCurrentBookingBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
