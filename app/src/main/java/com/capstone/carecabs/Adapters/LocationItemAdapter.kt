package com.capstone.carecabs.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.carecabs.Model.PassengerBookingModel
import com.capstone.carecabs.R

class LocationItemAdapter(private val locationData: List<PassengerBookingModel>) :
    RecyclerView.Adapter<LocationItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_passengers, parent, false)
        return LocationItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationItemViewHolder, position: Int) {
        val currentItem = locationData[position]
    }

    override fun getItemCount(): Int {
        return locationData.size
    }
}