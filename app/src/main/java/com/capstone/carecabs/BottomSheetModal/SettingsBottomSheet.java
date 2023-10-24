package com.capstone.carecabs.BottomSheetModal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.FontSizeManager;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentSettingsBottomSheetBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class SettingsBottomSheet extends BottomSheetDialogFragment {
	private final String TAG = "SettingsBottomSheet";
	private static final String FONT_SIZE_NORMAL = "normal";
	private static final String FONT_SIZE_LARGE = "large";
	private String fontSize = FONT_SIZE_NORMAL;
	private DocumentReference documentReference;
	private FragmentSettingsBottomSheetBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentSettingsBottomSheetBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		getFontSizeFromFireStore();

		binding.fontSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String newFontSize = isChecked ? FONT_SIZE_LARGE : FONT_SIZE_NORMAL;

			setFontSize(newFontSize);
			updateFontSizeToFireStore(newFontSize);
		});

		return view;
	}

	private void setFontSize(String newFontSize) {
		fontSize = newFontSize;
		int textSize = FONT_SIZE_NORMAL.equals(newFontSize) ? 17 : 20;

		binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		binding.textView3.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
	}

	private void getFontSizeFromFireStore() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot.exists()){
							String getFontSize = documentSnapshot.getString("fontSize");

							if (getFontSize.equals("large")){
								binding.fontSizeSwitch.setChecked(true);
							}

							setFontSize(getFontSize);
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "getFontSizeFromFireStore - onFailure: " + e.getMessage()));
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
}