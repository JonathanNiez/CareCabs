package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Model.FeedbackModel;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityFeedbackBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

public class FeedbackActivity extends AppCompatActivity implements SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "FeedbackActivity";
	private float textSizeSP;
	private float textHeaderSizeSP;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private VoiceAssistant voiceAssistant;
	private ActivityFeedbackBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		getUserSettings();

		binding.backFloatingBtn.setOnClickListener(v -> {
			finish();
		});

		binding.settingsFloatingBtn.setOnClickListener(v -> {
			SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
			settingsBottomSheet.setFontSizeChangeListener(this);
			settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
		});

		binding.submitBtn.setOnClickListener(v -> {
			String comment = binding.commentEditText.getText().toString();
			if (comment.isEmpty()) {
				Toast.makeText(this, "Please enter your comment", Toast.LENGTH_LONG).show();
				return;
			} else {
				submitFeedback(generateRandomFeedbackID(), comment);
			}
		});
	}

	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

	private void getUserSettings() {
		String fontSize = StaticDataPasser.storeFontSize;
		String voiceAssistantToggle = StaticDataPasser.storeVoiceAssistantState;

		setFontSize(fontSize);

		if (voiceAssistantToggle.equals("enabled")) {
			VoiceAssistant voiceAssistant = VoiceAssistant.getInstance(this);
			voiceAssistant.speak("Personal Info");
		}
	}

	private void setFontSize(String fontSize) {

		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		if (isChecked) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;

		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.submitFeedbackTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.feedbackThankYouTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.commentEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.submitBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

	}

	private String generateRandomFeedbackID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	private String getCurrentTimeAndDate() {
		Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based, so add 1
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		return month + "-" + day + "-" + year + " " + hour + ":" + minute + ":" + second;
	}

	private void submitFeedback
			(
					String feedbackID,
					String comment
			) {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.feedbackCollection)
					.document(feedbackID);

			FeedbackModel feedbackModel = new FeedbackModel(
					feedbackID,
					FirebaseMain.getUser().getUid(),
					comment,
					getCurrentTimeAndDate()
			);
			documentReference.set(feedbackModel)
					.addOnSuccessListener(unused -> {

						binding.commentEditText.setText("");
						Toast.makeText(FeedbackActivity.this, "Feedback submitted", Toast.LENGTH_SHORT).show();

					})
					.addOnFailureListener(e -> {
						Toast.makeText(FeedbackActivity.this, "Feedback failed to submit\nPlease try again", Toast.LENGTH_LONG).show();

						Log.e(TAG, "submitFeedback: " + e.getMessage());
					});
		}
	}
}