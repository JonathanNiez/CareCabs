package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.BottomSheetModal.RateDriverBottomSheet;
import com.capstone.carecabs.Chat.ChatDriverActivity;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.CurrentBookingModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ItemCurrentBookingBinding;

import java.util.List;

public class CurrentBookingAdapter extends
		RecyclerView.Adapter<CurrentBookingAdapter.CurrentBookingViewHolder> {
	private String fontSize = StaticDataPasser.storeFontSize;
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

		if (fontSize.equals("large")) {
			float HEADER_TEXT_SIZE = 25;
			float TEXT_SIZE = 22;

			holder.binding.pingedLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.yourCurrentBookingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.pickupLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SIZE);
			holder.binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.destinationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SIZE);
			holder.binding.bookingStatusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SIZE);
			holder.binding.driverArrivalTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
			holder.binding.vehicleColorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SIZE);
			holder.binding.vehiclePlateNumberTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SIZE);
			holder.binding.chatDriverTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.cancelBookingBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.rateDriverTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
			holder.binding.rateYourDriverTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
		}

		holder.binding.driverDetailsLayout.setVisibility(View.GONE);
		holder.binding.rateDriverBtn.setVisibility(View.GONE);
		holder.binding.rateYourDriverTextView.setVisibility(View.GONE);
		holder.binding.loadingDestinationLocation.setVisibility(View.GONE);
		holder.binding.loadingPickupLocation.setVisibility(View.GONE);
		holder.binding.pickupLocationTextView.setText(currentBookingModel.getPickupLocation());

		if (currentBookingModel.getBookingStatus().equals("Transported to destination")){
			holder.binding.destinationTextView.setText("Your Driver safely dropped you to your Destination");
		}else {
			holder.binding.destinationTextView.setText(currentBookingModel.getDestination());
		}

		holder.binding.bookingStatusTextView.setText(currentBookingModel.getBookingStatus());

		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			if (currentBookingModel.getPassengerUserID().equals(userID) &&
					currentBookingModel.getBookingStatus().equals("Driver on the way")) {

				holder.binding.bookingStatusTextView.setVisibility(View.GONE);
				holder.binding.cancelBookingBtn.setVisibility(View.GONE);
				holder.binding.driverDetailsLayout.setVisibility(View.VISIBLE);

				holder.binding.driverArrivalTimeTextView.setText("Arrival time:\nEstimated "
						+ currentBookingModel.getDriverArrivalTime() + " minute(s)");
				holder.binding.vehicleColorTextView
						.setText("Vehicle Color: " + currentBookingModel.getVehicleColor());
				holder.binding.vehiclePlateNumberTextView
						.setText("Vehicle plate number: " + currentBookingModel.getVehiclePlateNumber());
				holder.binding.pingedLocationTextView.setText("Ping location:\n" +
						currentBookingModel.getDriverPingedLocation());

				holder.binding.chatDriverBtn.setOnClickListener(v -> {
					Intent intent = new Intent(context, ChatDriverActivity.class);
					intent.putExtra("driverID", currentBookingModel.getDriverUserID());
					context.startActivity(intent);
				});

			} else if (currentBookingModel.getPassengerUserID().equals(userID) &&
					currentBookingModel.getBookingStatus().equals("Transported to destination") &&
					currentBookingModel.getRatingStatus().equals("Driver not rated")) {

				holder.binding.yourCurrentBookingTextView.setText("DRIVER NOT RATED");
				holder.binding.cancelBookingBtn.setVisibility(View.GONE);
				holder.binding.rateYourDriverTextView.setVisibility(View.VISIBLE);
				holder.binding.rateDriverBtn.setVisibility(View.VISIBLE);

				holder.binding.rateDriverBtn.setOnClickListener(v -> {
					Bundle bundle = new Bundle();
					bundle.putString("driverID", currentBookingModel.getDriverUserID());
					bundle.putString("bookingID", currentBookingModel.getBookingID());

					RateDriverBottomSheet rateDriverBottomSheet = new RateDriverBottomSheet();
					rateDriverBottomSheet.setArguments(bundle);
					rateDriverBottomSheet.show(fragmentActivity.getSupportFragmentManager(),
							rateDriverBottomSheet.getTag());
				});
			}
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
