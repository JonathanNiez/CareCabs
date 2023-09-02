package com.capstone.carecabs.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.capstone.carecabs.R;

public class ChangeFontSizeFragment extends Fragment {

    private TextView fontSizePreview, fontSizeLabel, currentFontSize;
    private SeekBar fontSizeSeekBar;
    private Button saveBtn;
    private ImageButton imgBackBtn;
    private String fontSizeLabelString;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_font_size, container, false);

        fontSizeLabel = view.findViewById(R.id.fontSizeLabel);
        fontSizePreview = view.findViewById(R.id.fontSizePreview);
        currentFontSize = view.findViewById(R.id.currentFontSize);
        fontSizeSeekBar = view.findViewById(R.id.fontSizeSeekBar);
        saveBtn = view.findViewById(R.id.saveBtn);
        imgBackBtn = view.findViewById(R.id.imgBackBtn);

        currentFontSize.setText("Medium");

        imgBackBtn.setOnClickListener(v -> {
            backToAppSettingsFragment();
        });

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontSizeLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        saveBtn.setOnClickListener(v -> {
            // Save the selected font size to preferences or application settings
        });


        return view;
    }

    private void updateFontSizeLabel(int progress) {
        switch (progress) {
            case 0:
                fontSizeLabelString = "Font Size: Small";
                fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                break;
            case 1:
                fontSizeLabelString = "Font Size: Medium";
                fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                break;
            case 2:
                fontSizeLabelString = "Font Size: Large";
                fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                break;
            case 3:
                fontSizeLabelString = "Font Size: Extra Large";
                fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
                break;
            default:
                fontSizeLabelString = "Font Size: Medium";
        }
        currentFontSize.setText(fontSizeLabelString);
    }

    private void backToAppSettingsFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AppSettingsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}