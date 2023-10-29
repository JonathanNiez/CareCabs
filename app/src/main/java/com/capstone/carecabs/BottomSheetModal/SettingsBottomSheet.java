package com.capstone.carecabs.BottomSheetModal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentSettingsBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsBottomSheet extends BottomSheetDialogFragment {
	private final String TAG = "SettingsBottomSheet";
	private static final String FONT_SIZE_NORMAL = "normal";
	private static final String FONT_SIZE_LARGE = "large";
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private static final String THEME_NORMAL = "normal";
	private static final String THEME_CONTRAST = "contrast";
	private static final String VOICE_ASSISTANT_ENABLED = "enabled";
	private static final String VOICE_ASSISTANT_DISABLED = "disabled";
	private DocumentReference documentReference;
	private Context context;
	private FontSizeChangeListener fontSizeChangeListener;
	private ThemeChangeListener themeChangeListener;
	private VoiceAssistantToggleListener voiceAssistantToggleListener;
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantToggle;
	private FragmentSettingsBottomSheetBinding binding;

	public interface VoiceAssistantToggleListener {
		void onVoiceAssistantToggled(boolean isToggled);
	}

	public interface ThemeChangeListener {
		void onThemeChanged(boolean isChecked);
	}

	public interface FontSizeChangeListener {
		void onFontSizeChanged(boolean isChecked);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentSettingsBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		getUserSettings();

		binding.fontSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String fontSize = isChecked ? FONT_SIZE_LARGE : FONT_SIZE_NORMAL;

			if (voiceAssistantToggle.equals("enabled")) {
				voiceAssistant = VoiceAssistant.getInstance(context);
				voiceAssistant.speak("Text size changed to " + fontSize);
			}

			setFontSize(fontSize);
			updateFontSizeToFireStore(fontSize);

			if (fontSizeChangeListener != null) {
				fontSizeChangeListener.onFontSizeChanged(isChecked);
			}
		});

		binding.contrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

			if (isChecked) {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
			} else {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			}

			String theme = isChecked ? THEME_CONTRAST : THEME_NORMAL;

			if (voiceAssistantToggle.equals("enabled")) {
				voiceAssistant = VoiceAssistant.getInstance(context);
				voiceAssistant.speak("App theme changed to " + theme);
			}

			setTheme(theme);
			updateThemeToFireStore(theme);

			if (themeChangeListener != null) {
				themeChangeListener.onThemeChanged(isChecked);
			}
		});

		binding.voiceAssistantSwitch.setOnCheckedChangeListener((buttonView, isToggled) -> {

			if (isToggled) {
				voiceAssistant.speak("voice assistant enabled");
			} else {
				voiceAssistant.speak("voice assistant disabled");
				voiceAssistant.shutdown();
			}

			String voiceAssistantState = isToggled ? VOICE_ASSISTANT_ENABLED : VOICE_ASSISTANT_DISABLED;

			if (voiceAssistantToggle.equals("enabled")) {
				voiceAssistant = VoiceAssistant.getInstance(context);
				voiceAssistant.speak("Voice Assistant " + voiceAssistantState);
			}

			updateVoiceAssistantToFireStore(voiceAssistantState);

		});

		return view;
	}

	public void setVoiceAssistantToggleListener(VoiceAssistantToggleListener voiceAssistantToggleListener) {
		this.voiceAssistantToggleListener = voiceAssistantToggleListener;
	}

	public void setThemeChangeListener(ThemeChangeListener themeChangeListener) {
		this.themeChangeListener = themeChangeListener;
	}

	public void setFontSizeChangeListener(FontSizeChangeListener fontSizeChangeListener) {
		this.fontSizeChangeListener = fontSizeChangeListener;
	}

	private void setTheme(String theme) {
		int fontImageResID;
		int eyeImageResID;
		int voiceImageResID;

		if (theme.equals(THEME_CONTRAST)) {

			fontImageResID = R.drawable.font_white_50;
			eyeImageResID = R.drawable.eye_white_50;
			voiceImageResID = R.drawable.voice_white_50;

//			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

		} else {

			fontImageResID = R.drawable.font_50;
			eyeImageResID = R.drawable.eye_50;
			voiceImageResID = R.drawable.voice_50;

//			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}

		setImageViewResource(binding.fontImageView, fontImageResID);
		setImageViewResource(binding.eyeImageView, eyeImageResID);
		setImageViewResource(binding.voiceImageView, voiceImageResID);
	}

	private void setImageViewResource(ImageView imageView, int imageResID) {
		if (imageView != null && imageResID != 0) {
			imageView.setImageResource(imageResID);
		}
	}

	private void setFontSize(String fontSize) {

		float textSizeSP;
		float textHeaderSizeSP;
		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}
		binding.accessibilityToolBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.swipeDownTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.increaseTextSizeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.changeContrastTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.voiceAssistantTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void getUserSettings() {
		SharedPreferences preferences = context.getSharedPreferences("userSettings", Context.MODE_PRIVATE);
		String theme = preferences.getString("theme", "normal");
		String fontSize = preferences.getString("fontSize", "normal");
		voiceAssistantToggle = preferences.getString("voiceAssistant", "disabled");

		setFontSize(fontSize);
		setTheme(theme);

		binding.fontSizeSwitch.setChecked(FONT_SIZE_LARGE.equals(fontSize));
		binding.contrastSwitch.setChecked(THEME_CONTRAST.equals(theme));
		binding.voiceAssistantSwitch.setChecked(VOICE_ASSISTANT_ENABLED.equals(voiceAssistantToggle));

		if (voiceAssistantToggle.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Accessibility Settings");
		}
	}

	private void updateFontSizeToFireStore(String fontSize) {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			Map<String, Object> updateFontSize = new HashMap<>();
			updateFontSize.put("fontSize", fontSize);

			documentReference.update(updateFontSize)
					.addOnSuccessListener(unused -> {

						Log.i(TAG, "updateFontSizeToFireStore - onSuccess: font size updated successfully");
					})
					.addOnFailureListener(e -> {

						Log.e(TAG, "updateFontSizeToFireStore - onFailure: " + e.getMessage());
					});
		}
	}

	private void updateThemeToFireStore(String theme) {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			Map<String, Object> updateFontSize = new HashMap<>();
			updateFontSize.put("theme", theme);

			documentReference.update(updateFontSize)
					.addOnSuccessListener(unused -> {

						Log.i(TAG, "updateThemeToFireStore - onSuccess: theme updated successfully");
					})
					.addOnFailureListener(e -> {

						Log.e(TAG, "updateThemeToFireStore - onFailure: " + e.getMessage());
					});
		}
	}

	private void updateVoiceAssistantToFireStore(String voiceAssistant) {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			Map<String, Object> updateFontSize = new HashMap<>();
			updateFontSize.put("voiceAssistant", voiceAssistant);

			documentReference.update(updateFontSize)
					.addOnSuccessListener(unused -> {

						Log.i(TAG, "updateVoiceAssistantToFireStore - onSuccess: voice assistant updated successfully");
					})
					.addOnFailureListener(e -> {

						Log.e(TAG, "updateVoiceAssistantToFireStore - onFailure: " + e.getMessage());
					});
		}
	}

	private void storeUserSettings(String theme, String fontSize, String voiceAssistant) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("userSettings", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("theme", theme);
		editor.putString("fontSize", fontSize);
		editor.putString("voiceAssistant", voiceAssistant);
		editor.apply();
	}
}