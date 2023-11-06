package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.FavoritesModel;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.ItemFavoritesBinding;

import java.util.List;

public class FavoritesAdapter
		extends RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder> {

	private final String TAG = "FavoritesAdapter";
	private Context context;
	private List<FavoritesModel> favoritesModelList;
	private ItemFavoriteClickListener itemFavoriteClickListener;

	public interface ItemFavoriteClickListener {
		void onItemFavoriteClick(FavoritesModel favoritesModel);
	}

	public FavoritesAdapter(Context context,
	                        List<FavoritesModel> favoritesModelList,
	                        ItemFavoriteClickListener itemFavoriteClickListener) {
		this.context = context;
		this.favoritesModelList = favoritesModelList;
		this.itemFavoriteClickListener = itemFavoriteClickListener;
	}

	@NonNull
	@Override
	public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemFavoritesBinding binding = ItemFavoritesBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new FavoritesViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull FavoritesViewHolder holder, int position) {
		FavoritesModel favoritesModel = favoritesModelList.get(position);

		holder.binding.destinationTextView.setText(favoritesModel.getDestination());

		holder.binding.heartImageView.setOnClickListener(v -> {
			if (itemFavoriteClickListener != null) {
				holder.binding.heartImageView.setImageResource(R.drawable.black_heart_48);
				itemFavoriteClickListener.onItemFavoriteClick(favoritesModelList.get(position));
			}
		});
	}

	@Override
	public int getItemCount() {
		return favoritesModelList.size();
	}

	public class FavoritesViewHolder extends RecyclerView.ViewHolder {
		private ItemFavoritesBinding binding;

		public FavoritesViewHolder(@NonNull ItemFavoritesBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
