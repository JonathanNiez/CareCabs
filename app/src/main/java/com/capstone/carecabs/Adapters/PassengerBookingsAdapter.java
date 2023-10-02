package com.capstone.carecabs.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Model.PassengerBookingModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemPassengersBinding;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public class PassengerBookingsAdapter extends RecyclerView.Adapter<PassengerBookingsAdapter.PassengerViewHolder> {
	private Context context;
	private final List<PassengerBookingModel> passengerBookingModelList;
	private ItemPassengerClickListener itemPassengerClickListener;

	public interface ItemPassengerClickListener {
		void onItemClick(PassengerBookingModel passengerBookingModel);
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
		holder.binding.passengerType.setText(passengerBookingModel.getPassengerUserType());
		holder.binding.bookingDate.setText("Booking Date: " + passengerBookingModel.getBookingDate());

		if (passengerBookingModel.getBookingStatus().equals("Waiting")){
			holder.binding.bookingStatus.setTextColor(Color.BLUE);
		}
		holder.binding.bookingStatus.setText(passengerBookingModel.getBookingStatus());

		holder.itemView.setOnClickListener(view -> {
			itemPassengerClickListener.onItemClick(passengerBookingModelList.get(position));
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
