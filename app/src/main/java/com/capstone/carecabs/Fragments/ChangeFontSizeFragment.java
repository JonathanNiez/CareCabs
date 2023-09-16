package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentChangeFontSizeBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChangeFontSizeFragment extends Fragment {
	private Context context;
	private String TAG = "ChangeFontSizeFragment";
	private DocumentReference documentReference;
	private String fontSizeLabelString;
	private AlertDialog pleaseWaitDialog;
	private AlertDialog.Builder builder;
	private FragmentChangeFontSizeBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentChangeFontSizeBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		context = getContext();
		showCurrentFontSizeFromUserSetting();

		binding.smallFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(0);

		});
		binding.normalFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(1);

		});
		binding.largeFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(2);

		});
		binding.veryLargeFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(3);
		});

		binding.imgBackBtn.setOnClickListener(v -> {
			backToAppSettingsFragment();
		});


		binding.saveBtn.setOnClickListener(v -> {
			showPleaseWaitDialog();

			updateFontSizeToFireStore(StaticDataPasser.storeFontSize);
		});


		return view;
	}

	private void updateFontSizeToFireStore(int fontSize) {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid());
			Map<String, Object> updateFontSize = new HashMap<>();
			updateFontSize.put("fontSize", fontSize);

			documentReference.update(updateFontSize).addOnSuccessListener(new OnSuccessListener<Void>() {
				@Override
				public void onSuccess(Void unused) {
					closePleaseWaitDialog();

				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					closePleaseWaitDialog();

					Log.e(TAG, e.getMessage());
				}
			});
		}
	}


	private void showCurrentFontSizeFromUserSetting() {
		if (FirebaseMain.getUser() != null) {
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(StaticDataPasser.userCollection)
					.document(FirebaseMain.getUser().getUid());
			documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
				@Override
				public void onSuccess(DocumentSnapshot documentSnapshot) {
					if (documentSnapshot != null && documentSnapshot.exists()) {
						Long getFontSizeLong = documentSnapshot.getLong("fontSize");
						int getFontSize = getFontSizeLong.intValue();

						StaticDataPasser.storeFontSize = getFontSize;

						switch (getFontSize) {
							case 15:
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.currentFontSize.setText("Font Size: Small");

								break;

							case 17:
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.currentFontSize.setText("Font Size: Normal");

								break;

							case 19:
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.currentFontSize.setText("Font Size: Large");

								break;

							case 21:
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.currentFontSize.setText("Font Size: Very Large");

								break;

						}

					} else {

					}
				}
			}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.e(TAG, e.getMessage());

				}
			});
		}
	}

	private void updateFontSizeLabel(int fontSize) {
		switch (fontSize) {
			case 0:
				fontSizeLabelString = "Font Size: Small";
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				StaticDataPasser.storeFontSize = 15;

				break;

			case 1:
				fontSizeLabelString = "Font Size: Normal";
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				StaticDataPasser.storeFontSize = 17;

				break;

			case 2:
				fontSizeLabelString = "Font Size: Large";
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				StaticDataPasser.storeFontSize = 19;

				break;

			case 3:
				fontSizeLabelString = "Font Size: Extra Large";
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				StaticDataPasser.storeFontSize = 21;

				break;

			default:
				fontSizeLabelString = "Font Size: Normal";
		}
		binding.currentFontSize.setText(fontSizeLabelString);
	}

	public void onBackPressed() {
		backToAppSettingsFragment();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		closePleaseWaitDialog();
	}

	@Override
	public void onPause() {
		super.onPause();

		closePleaseWaitDialog();
	}

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.please_wait_dialog, null);

		builder.setView(dialogView);

		pleaseWaitDialog = builder.create();
		pleaseWaitDialog.show();
	}

	private void closePleaseWaitDialog() {
		if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
			pleaseWaitDialog.dismiss();
		}
	}


	private void backToAppSettingsFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AppSettingsFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}


}