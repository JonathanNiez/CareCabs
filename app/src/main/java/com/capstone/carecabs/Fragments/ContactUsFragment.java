package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.FeedbackActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentContactUsBinding;

import java.util.Objects;

public class ContactUsFragment extends Fragment implements SettingsBottomSheet.FontSizeChangeListener {
	private Context context;
	private VoiceAssistant voiceAssistant;
	private FragmentContactUsBinding binding;

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
		SharedPreferences preferences = Objects.requireNonNull(context).getSharedPreferences("userSettings", Context.MODE_PRIVATE);
		String voiceAssistantToggle = preferences.getString("voiceAssistant", "disabled");

		if (voiceAssistantToggle.equals("enabled")){
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak("Contact Us");
		}

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		binding.submitFeedBackBtn.setOnClickListener(v -> {
			Intent intent = new Intent(context, FeedbackActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
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

	}
}