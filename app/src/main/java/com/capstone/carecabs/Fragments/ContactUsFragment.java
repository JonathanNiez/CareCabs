package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.FeedbackActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentContactUsBinding;

import java.util.Objects;

public class ContactUsFragment extends Fragment implements
		SettingsBottomSheet.FontSizeChangeListener {
	private FragmentContactUsBinding binding;
	private Context context;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentContactUsBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		getUserSettings();

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		binding.submitFeedBackBtn.setOnClickListener(v -> {
			Intent intent = new Intent(context, FeedbackActivity.class);
			startActivity(intent);
		});

		return view;
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void backToAccountFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		String fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);

	}

	private void getUserSettings() {
		String fontSize = StaticDataPasser.storeFontSize;

		setFontSize(fontSize);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Contact us");
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

		binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView3.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView4.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView5.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView6.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.textView7.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.textView8.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.submitFeedBackBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}
}

