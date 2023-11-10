package com.capstone.carecabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import com.capstone.carecabs.Adapters.TutorialsAdapter;
import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Model.TutorialsModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityHelpBinding;
import com.capstone.carecabs.databinding.DialogExitAppBinding;
import com.capstone.carecabs.databinding.DialogTutorialBinding;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "HelpActivity";
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private AlertDialog tutorialDialog;
	private ActivityHelpBinding binding;

	@Override
	protected void onPause() {
		super.onPause();

		closeTutorialDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		closeTutorialDialog();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityHelpBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.settingsFloatingBtn.setOnClickListener(v -> showSettingsBottomSheet());

		binding.backFloatingBtn.setOnClickListener(v -> finish());

		loadTutorials();

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {

	}

	private void showSettingsBottomSheet() {
		SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
		settingsBottomSheet.setFontSizeChangeListener(this);
		settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
	}

	private void loadTutorials() {
		List<TutorialsModel> tutorialsModelList = new ArrayList<>();
		TutorialsAdapter tutorialsAdapter = new TutorialsAdapter(this,
				tutorialsModelList,
				tutorialsModel ->
						showTutorialDialog(tutorialsModel.getTutorialImage(),
								tutorialsModel.getTutorialTitle(),
								tutorialsModel.getTutorialBody()));

		binding.tutorialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.tutorialsRecyclerView.setAdapter(tutorialsAdapter);

		TutorialsModel tutorial1 = new TutorialsModel(R.drawable.car_64_2, "Ride Booking", "body");
		TutorialsModel tutorial2 = new TutorialsModel(R.drawable.mar, "Edit Account", "body");
		TutorialsModel tutorial3 = new TutorialsModel(R.drawable.chat_50, "Chat", "body");

		tutorialsModelList.add(tutorial1);
		tutorialsModelList.add(tutorial2);
		tutorialsModelList.add(tutorial3);
	}

	private void showTutorialDialog(int tutorialImage,
	                                String tutorialTitle,
	                                String tutorialBody) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		DialogTutorialBinding dialogTutorialBinding = DialogTutorialBinding.inflate(getLayoutInflater());
		View dialogView = dialogTutorialBinding.getRoot();

		if (fontSize.equals("large")) {
			dialogTutorialBinding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogTutorialBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			dialogTutorialBinding.nextBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			dialogTutorialBinding.closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Are you sure you want to exit the App?");
		}

		dialogTutorialBinding.tutorialImageView.setImageResource(tutorialImage);
		dialogTutorialBinding.titleTextView.setText(tutorialTitle);
		dialogTutorialBinding.bodyTextView.setText(tutorialBody);

		dialogTutorialBinding.nextBtn.setOnClickListener(v -> {

		});

		dialogTutorialBinding.closeBtn.setOnClickListener(v -> closeTutorialDialog());

		builder.setView(dialogView);

		tutorialDialog = builder.create();
		tutorialDialog.show();
	}

	private void closeTutorialDialog() {
		if (tutorialDialog != null && tutorialDialog.isShowing()) {
			tutorialDialog.dismiss();
		}
	}
}