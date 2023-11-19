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
import com.capstone.carecabs.databinding.DialogTutorialBinding;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "HelpActivity";
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private AlertDialog tutorialDialog;
	private SettingsBottomSheet.FontSizeChangeListener fontSizeChangeListener;
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

		if (fontSize.equals("large")) {
			binding.helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Help");
		}

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
						showTutorialDialog(
								tutorialsModel.getTutorialImage(),
								tutorialsModel.isGif(),
								tutorialsModel.getTutorialTitle(),
								tutorialsModel.getTutorialBody()));

		binding.tutorialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.tutorialsRecyclerView.setAdapter(tutorialsAdapter);

		TutorialsModel tutorial1 = new TutorialsModel(R.drawable.ride_booking_gif, true, "Ride Booking",
				getString(R.string.help_how_to_book));
		TutorialsModel tutorial2 = new TutorialsModel(R.drawable.ss_accessibility, false, "Quick Accessibility Settings",
				getString(R.string.help_accessibility));
		TutorialsModel tutorial3 = new TutorialsModel(R.drawable.ss_chat, false, "Chat",
				getString(R.string.help_chat));
		TutorialsModel tutorial4 = new TutorialsModel(R.drawable.recenter_location_gif, true, "Recenter the Camera to you Current Location",
				getString(R.string.help_recenter_location));
		TutorialsModel tutorial5 = new TutorialsModel(R.drawable.toggle_fullscreen_gif, true, "Toggle Map Fullscreen",
				getString(R.string.help_fullscreen_map));
		TutorialsModel tutorial6 = new TutorialsModel(R.drawable.zoom_map_gif, true, "Zoom Map",
				getString(R.string.help_zoom_map));
		TutorialsModel tutorial7 = new TutorialsModel(R.drawable.change_map_style_gif, true, "Change Map Style",
				getString(R.string.help_map_style));

		tutorialsModelList.add(tutorial1);
		tutorialsModelList.add(tutorial2);
		tutorialsModelList.add(tutorial3);
		tutorialsModelList.add(tutorial4);
		tutorialsModelList.add(tutorial5);
		tutorialsModelList.add(tutorial6);
		tutorialsModelList.add(tutorial7);
	}

	private void showTutorialDialog(int tutorialImage,
	                                boolean isGif,
	                                String tutorialTitle,
	                                String tutorialBody) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		DialogTutorialBinding dialogTutorialBinding =
				DialogTutorialBinding.inflate(getLayoutInflater());
		View dialogView = dialogTutorialBinding.getRoot();

		dialogTutorialBinding.tutorialGif.setVisibility(View.GONE);
		if (isGif) {
			dialogTutorialBinding.tutorialImageView.setVisibility(View.GONE);
			dialogTutorialBinding.tutorialGif.setVisibility(View.VISIBLE);
			dialogTutorialBinding.tutorialGif.setImageResource(tutorialImage);
		}

		dialogTutorialBinding.tutorialImageView.setImageResource(tutorialImage);
		dialogTutorialBinding.titleTextView.setText(tutorialTitle);
		dialogTutorialBinding.bodyTextView.setText(tutorialBody);

		if (fontSize.equals("large")) {
			dialogTutorialBinding.bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogTutorialBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			dialogTutorialBinding.closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		}

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak(tutorialBody);
		}


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