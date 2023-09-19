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
	private final String TAG = "ChangeFontSizeFragment";
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
		getCurrentFontSizeFromUserSetting();

		binding.smallFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(15);

		});
		binding.normalFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(17);

		});
		binding.largeFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(19);

		});
		binding.veryLargeFontSizeBtn.setOnClickListener(view1 -> {
			updateFontSizeLabel(21);
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


	private void getCurrentFontSizeFromUserSetting() {
		showPleaseWaitDialog();
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

						closePleaseWaitDialog();
						StaticDataPasser.storeFontSize = getFontSize;

						switch (getFontSize) {
							case 15:
								binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
								binding.currentFontSize.setText("Current Font Size: Small");
								binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

								break;

							case 17:
								binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
								binding.currentFontSize.setText("Current Font Size: Normal");
								binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

								break;

							case 19:
								binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
								binding.currentFontSize.setText("Current Font Size: Large");
								binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);

								break;

							case 21:
								binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
								binding.currentFontSize.setText("Current Font Size: Very Large");
								binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

								break;

						}

					} else {
						closePleaseWaitDialog();
					}
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

	private void updateFontSizeLabel(int fontSize) {
		switch (fontSize) {
			case 15:
				fontSizeLabelString = "Current Font Size: Small";
				binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				StaticDataPasser.storeFontSize = fontSize;

				break;

			case 17:
				fontSizeLabelString = "Current Font Size: Normal";
				binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
				StaticDataPasser.storeFontSize = fontSize;

				break;

			case 19:
				fontSizeLabelString = "Current Font Size: Large";
				binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				binding.currentFontSize.setText(fontSizeLabelString);
				binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
				StaticDataPasser.storeFontSize = fontSize;

				break;

			case 21:
				fontSizeLabelString = "Current Font Size: Very Large";
				binding.textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.fontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				binding.currentFontSize.setText(fontSizeLabelString);
				binding.textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				StaticDataPasser.storeFontSize = fontSize;

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

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_please_wait, null);

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