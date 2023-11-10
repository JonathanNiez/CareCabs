package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.TutorialsModel;
import com.capstone.carecabs.databinding.ItemTutorialsBinding;

import java.util.List;

public class TutorialsAdapter extends RecyclerView.Adapter<TutorialsAdapter.TutorialsViewHolder> {

	private Context context;

	private List<TutorialsModel> tutorialsModelList;

	public TutorialsAdapter(Context context, List<TutorialsModel> tutorialsModelList) {
		this.context = context;
		this.tutorialsModelList = tutorialsModelList;
	}

	@NonNull
	@Override
	public TutorialsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemTutorialsBinding binding = ItemTutorialsBinding
				.inflate(LayoutInflater.from(parent.getContext()), parent, false);

		return new TutorialsViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull TutorialsViewHolder holder, int position) {
		TutorialsModel tutorialsModel = tutorialsModelList.get(position);

		holder.binding.tutorialImageView.setImageResource(tutorialsModel.getTutorialImage());
		holder.binding.tutorialTitleTextView.setText(tutorialsModel.getTutorialTitle());
		holder.binding.tutorialBodyTextView.setText(tutorialsModel.getTutorialBody());

		holder.itemView.setOnClickListener(v -> {

		});
	}

	@Override
	public int getItemCount() {
		return tutorialsModelList.size();
	}

	public class TutorialsViewHolder extends RecyclerView.ViewHolder {

		private ItemTutorialsBinding binding;

		public TutorialsViewHolder(@NonNull ItemTutorialsBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
