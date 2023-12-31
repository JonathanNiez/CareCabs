package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.FragmentChangePasswordBinding;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

public class ChangePasswordFragment extends Fragment implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "ChangePasswordFragment";
	private FragmentChangePasswordBinding binding;
	private VoiceAssistant voiceAssistant;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private AlertDialog.Builder builder;
	private AlertDialog passwordResetConfirmationDialog, cancelPasswordResetDialog,
			passwordUpdateSuccessDialog, passwordUpdateFailedDialog, passwordWarningDialog;
	private Intent intent;
	private Context context;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;

	@Override
	public void onPause() {
		super.onPause();

		closePasswordResetConfirmationDialog();
		closeChangePasswordWarningDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closeCancelPasswordResetDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		closePasswordResetConfirmationDialog();
		closeChangePasswordWarningDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closeCancelPasswordResetDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
		View view = binding.getRoot();

		binding.progressBarLayout.setVisibility(View.GONE);
		binding.googleSignInLayout.setVisibility(View.GONE);
		binding.emailNotVerifiedLayout.setVisibility(View.GONE);

		context = getContext();

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);

			binding.emailEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Email"));
			binding.oldPasswordEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("Old Password"));
			binding.newPasswordEditText.setOnFocusChangeListener((v, hasFocus) ->
					voiceAssistant.speak("New Password"));
		}

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		binding.okayBtn1.setOnClickListener(v -> backToAccountFragment());

		binding.okayBtn2.setOnClickListener(v -> backToAccountFragment());

		binding.changePasswordBtn.setOnClickListener(v -> {
			String stringOldPassword = binding.oldPasswordEditText.getText().toString();
			String stringNewPassword = binding.newPasswordEditText.getText().toString();
			String email = binding.emailEditText.getText().toString().trim();

			if (email.isEmpty()) {
				binding.emailEditText.setError("Please enter your Email");
			} else if (stringOldPassword.isEmpty()) {
				binding.oldPasswordEditText.setError("Please enter your old Password");
			} else if (stringNewPassword.isEmpty()) {
				binding.newPasswordEditText.setError("Please enter your new Password");
			} else {
				showPasswordResetConfirmationDialog(email);
			}
		});

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (isAdded()) {
			getUserSettings();
			checkUserRegisterMethod();
		}
	}

	private void checkUserRegisterMethod() {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot != null & documentSnapshot.exists()) {
							String getRegisterType = documentSnapshot.getString("registerType");

							if (getRegisterType != null) {
								if (getRegisterType.equals("google")) {
									binding.googleSignInLayout.setVisibility(View.VISIBLE);
									binding.changePasswordLayout.setVisibility(View.GONE);
									binding.changePasswordBtn.setVisibility(View.GONE);
								} else {
									if (FirebaseMain.getUser().isEmailVerified()) {
										if (isAdded()) {
											showChangePasswordWarningDialog();
										}
									} else {
										binding.emailNotVerifiedLayout.setVisibility(View.VISIBLE);
										binding.changePasswordLayout.setVisibility(View.GONE);
										binding.changePasswordBtn.setVisibility(View.GONE);
										binding.passwordWarningTextView.setVisibility(View.GONE);
									}
								}
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "checkUserRegisterMethod: " + e.getMessage()));
		}
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void getUserSettings() {
		setFontSize(fontSize);
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		String fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
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
		binding.changePasswordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.googleSignInTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.emailNotVerifiedTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.passwordWarningTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.emailNotVerifiedBodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.oldPasswordEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.newPasswordEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.changePasswordBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.okayBtn1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.okayBtn1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);

	}

	private void showChangePasswordWarningDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_change_password_warning, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		String changePasswordWarning = getString(R.string.change_password_warning);

		if (voiceAssistantState.equals("enabled")) {
			voiceAssistant = VoiceAssistant.getInstance(context);
			voiceAssistant.speak(changePasswordWarning);
		}

		okayBtn.setOnClickListener(v -> closeChangePasswordWarningDialog());

		builder.setView(dialogView);
		passwordWarningDialog = builder.create();
		passwordWarningDialog.show();
	}

	private void closeChangePasswordWarningDialog() {
		if (passwordWarningDialog != null && passwordWarningDialog.isShowing()) {
			passwordWarningDialog.dismiss();
		}
	}

	private void showPasswordUpdateSuccessDialog(String email) {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_password_change_success, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);
		TextView emailTextView = dialogView.findViewById(R.id.emailTextView);

		emailTextView.setText(email);

		okBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), LoginActivity.class);
			startActivity(intent);
			Objects.requireNonNull(getActivity()).finish();
		});

		builder.setView(dialogView);

		passwordUpdateSuccessDialog = builder.create();
		passwordUpdateSuccessDialog.show();
	}

	private void closePasswordUpdateSuccessDialog() {
		if (passwordUpdateSuccessDialog != null && passwordUpdateSuccessDialog.isShowing()) {
			passwordUpdateSuccessDialog.dismiss();
		}
	}


	private void resetPassword(String email) {
		FirebaseMain.getAuth().sendPasswordResetEmail(email)
				.addOnSuccessListener(unused -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.changePasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateSuccessDialog(email);

				})
				.addOnFailureListener(e -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.changePasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateFailedDialog();

					Log.e(TAG, "resetPassword: " + e.getMessage());
				});
	}

	private void showPasswordUpdateFailedDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_password_change_failed, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> closePasswordUpdateFailedDialog());

		builder.setView(dialogView);
		passwordUpdateFailedDialog = builder.create();
		passwordUpdateFailedDialog.show();
	}

	private void closePasswordUpdateFailedDialog() {
		if (passwordUpdateFailedDialog != null && passwordUpdateFailedDialog.isShowing()) {
			passwordUpdateFailedDialog.dismiss();
		}

	}

	private void showCancelPasswordResetDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_password_reset, null);

		Button noBtn = dialogView.findViewById(R.id.noBtn);
		Button yesBtn = dialogView.findViewById(R.id.yesBtn);

		yesBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), LoginActivity.class);
			startActivity(intent);
			getActivity().finish();
		});

		noBtn.setOnClickListener(view -> {
			closeCancelPasswordResetDialog();
		});

		builder.setView(dialogView);

		cancelPasswordResetDialog = builder.create();
		cancelPasswordResetDialog.show();
	}

	private void closeCancelPasswordResetDialog() {
		if (cancelPasswordResetDialog != null && cancelPasswordResetDialog.isShowing()) {
			cancelPasswordResetDialog.dismiss();
		}
	}

	private void closePasswordResetConfirmationDialog() {
		if (passwordResetConfirmationDialog != null && passwordResetConfirmationDialog.isShowing()) {
			passwordResetConfirmationDialog.dismiss();
		}
	}

	private void showPasswordResetConfirmationDialog(String email) {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_confirmation, null);

		Button resetBtn = dialogView.findViewById(R.id.resetBtn);
		Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

		resetBtn.setOnClickListener(v -> resetPassword(email));

		cancelBtn.setOnClickListener(v -> closePasswordResetConfirmationDialog());

		builder.setView(dialogView);
		passwordResetConfirmationDialog = builder.create();
		passwordResetConfirmationDialog.show();
	}

	private void backToAccountFragment() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.fragmentContainer, new AccountFragment());
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

}