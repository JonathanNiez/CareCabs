package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentChangePasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class ChangePasswordFragment extends Fragment {
	private final String TAG = "ChangePasswordFragment";
	private AlertDialog.Builder builder;
	private AlertDialog passwordResetConfirmationDialog, cancelPasswordResetDialog,
	passwordUpdateSuccessDialog, passwordUpdateFailedDialog, passwordWarningDialog;
	private Intent intent;
	private Context context;
	private FragmentChangePasswordBinding binding;

	@Override
	public void onPause() {
		super.onPause();

		closePasswordResetConfirmationDialog();
		closePasswordWarningDialog();
		closePasswordUpdateFailedDialog();
		closePasswordUpdateSuccessDialog();
		closeCancelPasswordResetDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		closePasswordResetConfirmationDialog();
		closePasswordWarningDialog();
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

		binding.googleSignInLayout.setVisibility(View.GONE);
		binding.progressBarLayout.setVisibility(View.GONE);

		context = getContext();
		checkUserRegisterMethod();

		binding.backFloatingBtn.setOnClickListener(v -> backToAccountFragment());

		binding.okayBtn.setOnClickListener(v -> backToAccountFragment());

		binding.resetPasswordBtn.setOnClickListener(v -> {
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

	private void checkUserRegisterMethod() {
		if (FirebaseMain.getUser() != null) {
			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(FirebaseMain.getUser().getUid());

			documentReference.get().addOnSuccessListener(documentSnapshot -> {
				if (documentSnapshot != null & documentSnapshot.exists()) {
					String getRegisterType = documentSnapshot.getString("registerType");

					if (getRegisterType.equals("google")) {
						binding.googleSignInLayout.setVisibility(View.VISIBLE);
						binding.editTextLayout.setVisibility(View.GONE);
						binding.resetPasswordBtn.setVisibility(View.GONE);
					}else {
						showPasswordWarningDialog();
					}
				}
			}).addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
		}
	}

	public void onBackPressed() {
		backToAccountFragment();
	}

	private void showPasswordWarningDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_password_warning, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			closePasswordWarningDialog();
		});

		builder.setView(dialogView);

		passwordWarningDialog = builder.create();
		passwordWarningDialog.show();
	}

	private void closePasswordWarningDialog() {
		if (passwordWarningDialog != null && passwordWarningDialog.isShowing()) {
			passwordWarningDialog.dismiss();
		}
	}

	private void showPasswordUpdateSuccessDialog(String email) {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_success, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);
		TextView emailTextView = dialogView.findViewById(R.id.emailTextView);

		String text = email;
		SpannableString underlinedText = new SpannableString(text);

		underlinedText.setSpan(new UnderlineSpan(), 0, text.length(), 0);

		emailTextView.setText(underlinedText);

		okBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), LoginActivity.class);
			startActivity(intent);
			getActivity().finish();
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

	public boolean isValidEmail(String email) {
		String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}";
		return email.matches(emailPattern);
	}

	private void resetPassword(String email) {
		FirebaseMain.getAuth().sendPasswordResetEmail(email)
				.addOnSuccessListener(unused -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateSuccessDialog(email);

				}).addOnFailureListener(e -> {
					binding.progressBarLayout.setVisibility(View.GONE);
					binding.resetPasswordBtn.setVisibility(View.VISIBLE);

					showPasswordUpdateFailedDialog();

					Log.e(TAG, e.getMessage());
				});
	}

	private void showPasswordUpdateFailedDialog() {
		builder = new AlertDialog.Builder(context);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_failed, null);

		Button okBtn = dialogView.findViewById(R.id.okBtn);

		okBtn.setOnClickListener(v -> {
			intent = new Intent(getActivity(), LoginActivity.class);
			startActivity(intent);
			getActivity().finish();
		});


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

		resetBtn.setOnClickListener(v -> {
			resetPassword(email);
		});

		cancelBtn.setOnClickListener(v -> {
			closePasswordResetConfirmationDialog();
		});

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