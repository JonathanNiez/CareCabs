package com.capstone.carecabs.Adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.carecabs.Model.TutorialsModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.ItemTutorialsBinding;

import java.util.List;

public class TutorialsAdapter extends
		RecyclerView.Adapter<TutorialsAdapter.TutorialsViewHolder> {
	private String fontSize = StaticDataPasser.storeFontSize;
	private Context context;
	private List<TutorialsModel> tutorialsModelList;
	private TutorialClickListener tutorialClickListener;

	public interface TutorialClickListener {
		void onTutorialClick(TutorialsModel tutorialsModel);
	}

	public TutorialsAdapter(Context context,
	                        List<TutorialsModel> tutorialsModelList,
	                        TutorialClickListener tutorialClickListener) {
		this.context = context;
		this.tutorialsModelList = tutorialsModelList;
		this.tutorialClickListener = tutorialClickListener;
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

		if (fontSize.equals("large")) {
			holder.binding.tutorialTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			holder.binding.tutorialBodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		}

		holder.binding.tutorialImageView.setImageResource(tutorialsModel.getTutorialImage());
		holder.binding.tutorialTitleTextView.setText(tutorialsModel.getTutorialTitle());
		holder.binding.tutorialBodyTextView.setText(tutorialsModel.getTutorialBody());

		holder.itemView.setOnClickListener(v -> {
			if (tutorialClickListener != null) {
				tutorialClickListener.onTutorialClick(tutorialsModelList.get(position));
			}
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
