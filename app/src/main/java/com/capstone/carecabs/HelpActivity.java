package com.capstone.carecabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;

import com.capstone.carecabs.Adapters.TutorialsAdapter;
import com.capstone.carecabs.Model.TutorialsModel;
import com.capstone.carecabs.databinding.ActivityHelpBinding;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {
	private ActivityHelpBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityHelpBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		loadTutorials();

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	private void loadTutorials() {
		List<TutorialsModel> tutorialsModelList = new ArrayList<>();
		TutorialsAdapter tutorialsAdapter = new TutorialsAdapter(this, tutorialsModelList);
		binding.tutorialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.tutorialsRecyclerView.setAdapter(tutorialsAdapter);

		TutorialsModel tutorial1 = new TutorialsModel(R.drawable.car_64_2, "Car", "yeah");
		TutorialsModel tutorial2 = new TutorialsModel(R.drawable.mar, "Mar", "hooyah");
		TutorialsModel tutorial3 = new TutorialsModel(R.drawable.navigate_32, "nig", "ga");
		TutorialsModel tutorial4 = new TutorialsModel(R.drawable.calendar_64, "Caleasne", "adsdadasd");

		tutorialsModelList.add(tutorial1);
		tutorialsModelList.add(tutorial2);
		tutorialsModelList.add(tutorial3);
		tutorialsModelList.add(tutorial4);

	}
}