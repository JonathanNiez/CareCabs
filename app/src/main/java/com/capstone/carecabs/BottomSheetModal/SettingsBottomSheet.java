package com.capstone.carecabs.BottomSheetModal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.R;
import com.capstone.carecabs.databinding.FragmentSettingsBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

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
	private DocumentReference documentReference;
	private FragmentSettingsBottomSheetBinding binding;
	private Context context;
	private FontSizeChangeListener fontSizeChangeListener;
	private ThemeChangeListener themeChangeListener;

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

		getAppSettingsFromFireStore();
		context = getContext();

		binding.fontSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String fontSize = isChecked ? FONT_SIZE_LARGE : FONT_SIZE_NORMAL;

			setFontSize(fontSize);
			updateFontSizeToFireStore(fontSize);

			if (fontSizeChangeListener != null) {
				fontSizeChangeListener.onFontSizeChanged(isChecked);
			}
		});

		binding.contrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String theme = isChecked ? THEME_CONTRAST : THEME_NORMAL;

			setTheme(theme);
			updateThemeToFireStore(theme);

			if (themeChangeListener != null) {
				themeChangeListener.onThemeChanged(isChecked);
			}

		});

		return view;
	}

	public void setThemeChangeListener(ThemeChangeListener themeChangeListener) {
		this.themeChangeListener = themeChangeListener;
	}

	public void setFontSizeChangeListener(FontSizeChangeListener listener) {
		this.fontSizeChangeListener = listener;
	}

	private void setTheme(String theme) {

		int backgroundColorResID;
		int layoutColorResID;
		int textColorResID;
		int fontImageResID;
		int eyeImageResID;
		int voiceImageResID;

		if (theme.equals(THEME_CONTRAST)) {
			backgroundColorResID = R.color.darker_gray;
			layoutColorResID = R.color.dark_blue;
			textColorResID = R.color.white;

			fontImageResID = R.drawable.font_white_50;
			eyeImageResID = R.drawable.eye_white_50;
			voiceImageResID = R.drawable.voice_white_50;

			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

		} else {
			backgroundColorResID = R.color.white_blue;
			layoutColorResID = R.color.white;
			textColorResID = R.color.black;

			fontImageResID = R.drawable.font_50;
			eyeImageResID = R.drawable.eye_50;
			voiceImageResID = R.drawable.voice_50;

			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}

		setImageViewResource(binding.fontImageView, fontImageResID);
		setImageViewResource(binding.eyeImageView, eyeImageResID);
		setImageViewResource(binding.voiceImageView, voiceImageResID);
	}

	//		int backgroundColor = ContextCompat.getColor(context, backgroundColorResID);
//		ColorStateList colorStateList = ColorStateList.valueOf(backgroundColor);
//		binding.parentLayout.setBackgroundTintList(colorStateList);
//
//		int layoutColor = ContextCompat.getColor(context, layoutColorResID);
//		ColorStateList layoutColorStateList = ColorStateList.valueOf(layoutColor);
//		binding.textSizeLayout.setBackgroundTintList(layoutColorStateList);
//		binding.changeContrastLayout.setBackgroundTintList(layoutColorStateList);
//		binding.voiceAssistantLayout.setBackgroundTintList(layoutColorStateList);
//
//		int textColor = ContextCompat.getColor(context, textColorResID);
//		binding.accessibilityToolBarTextView.setTextColor(textColor);
//		binding.swipeDownTextView.setTextColor(textColor);
//		binding.increaseTextSizeTextView.setTextColor(textColor);
//		binding.changeContrastTextView.setTextColor(textColor);
//		binding.voiceAssistantTextView.setTextColor(textColor);
//
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

	private void getAppSettingsFromFireStore() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot.exists()) {
							String getFontSize = documentSnapshot.getString("fontSize");
							String getTheme = documentSnapshot.getString("theme");

							if (getFontSize != null && getTheme != null) {
								setFontSize(getFontSize);
								setTheme(getTheme);

								binding.fontSizeSwitch.setChecked(FONT_SIZE_LARGE.equals(getFontSize));
								binding.contrastSwitch.setChecked(THEME_CONTRAST.equals(getTheme));
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "getAppSettingsFromFireStore - onFailure: " + e.getMessage()));
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
}