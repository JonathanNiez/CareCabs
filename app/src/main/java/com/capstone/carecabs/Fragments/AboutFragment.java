package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentAboutBinding;

import java.util.Objects;

public class AboutFragment extends Fragment implements SettingsBottomSheet.FontSizeChangeListener {
    private Context context;
    private VoiceAssistant voiceAssistant;
    private FragmentAboutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        context = getContext();

        binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

        SharedPreferences preferences = Objects.requireNonNull(context).getSharedPreferences("userSettings", Context.MODE_PRIVATE);
        String voiceAssistantToggle = preferences.getString("voiceAssistant", "disabled");

        if (voiceAssistantToggle.equals("enabled")){
            voiceAssistant = VoiceAssistant.getInstance(context);
            voiceAssistant.speak("About");
        }

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