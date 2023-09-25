package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.carecabs.Model.PassengerModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemPassengersBinding;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder> {

	public interface ItemClickLister{
		void onItemClick(PassengerModel passengerModel);
	}

	private ItemClickLister itemClickLister;
	private final List<PassengerModel> passengerModelList;

	private Context context;

	public PassengerAdapter(Context context,
	                        List<PassengerModel> passengerModelList,
	                        ItemClickLister itemClickLister) {
		this.context = context;
		this.passengerModelList = passengerModelList;
		this.itemClickLister = itemClickLister;
	}

	@NonNull
	@Override
	public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemPassengersBinding binding = ItemPassengersBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new PassengerViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
		PassengerModel passengerModel = passengerModelList.get(position);

		String getPassengerFirstname = passengerModel.getPassengerFirstname();
		String getPassengerLastname = passengerModel.getPassengerLastname();
		String getPassengerImage = passengerModel.getPassengerProfilePicture();
		String getPassengerType = passengerModel.getPassengerUserType();
		String getBookingDate = passengerModel.getLocationTime();

		String fullName = getPassengerFirstname + " " + getPassengerLastname;

		holder.binding.passengerName.setText(fullName);
		if (!getPassengerImage.equals("default")) {
			Glide.with(context)
					.load(getPassengerImage)
					.placeholder(R.drawable.loading_gif)
					.into(holder.binding.passengerImage);
		}
		holder.binding.passengerType.setText(getPassengerType);
		holder.binding.bookingDate.setText("Booking Date: " + getBookingDate);

		holder.itemView.setOnClickListener(view -> {
			itemClickLister.onItemClick(passengerModelList.get(position));
		});
	}

	@Override
	public int getItemCount() {
		return passengerModelList.size();
	}

	public static class PassengerViewHolder extends RecyclerView.ViewHolder {

		private final ItemPassengersBinding binding;

		public PassengerViewHolder(ItemPassengersBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
