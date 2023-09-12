package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoginActivity;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentChangePasswordBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import org.mindrot.jbcrypt.BCrypt;

public class ChangePasswordFragment extends Fragment {
    private AlertDialog.Builder builder;
    private AlertDialog passwordWarningDialog, passwordChangeSuccessDialog,
            passwordChangeFailedDialog;
    private String userID;
    private Intent intent;
    private final String TAG = "ChangePasswordFragment";
    private FragmentChangePasswordBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        checkUserSignInMethod();

        binding.imgBackBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        binding.okBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        binding.changePasswordBtn.setOnClickListener(v -> {
            String stringOldPassword = binding.oldPassword.getText().toString();
            String stringNewPassword = binding.newPassword.getText().toString();

            if (stringOldPassword.isEmpty()) {
                binding.oldPassword.setError("Please Enter your Old Password");
            } else if (stringNewPassword.isEmpty()) {
                binding.newPassword.setError("Please Enter your New Password");
            } else {
                if (FirebaseMain.getUser() != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(FirebaseMain.getUser().getEmail(), stringOldPassword);
                    FirebaseMain.getUser().reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseMain.getUser().updatePassword(stringNewPassword)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            getCurrentUser(stringNewPassword);
                                            showPasswordChangeSuccessDialog();
                                        }
                                    }).addOnFailureListener(e -> {
                                        showPasswordChangeFailedDialog();
                                        e.printStackTrace();
                                    });
                        }
                    }).addOnFailureListener(e -> {
                        showPasswordChangeFailedDialog();
                        e.printStackTrace();
                    });
                }
            }
        });

        return view;
    }

    private void checkUserSignInMethod() {
        if (FirebaseMain.getUser() != null) {
            boolean isGoogleSignIn = false;

            for (UserInfo userInfo : FirebaseMain.getUser().getProviderData()) {
                if (userInfo.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {

                    isGoogleSignIn = true;

                    break;

                }
            }

            if (isGoogleSignIn) {
                binding.changePasswordBtn.setVisibility(View.GONE);
                binding.okBtn.setVisibility(View.VISIBLE);
                binding.oldPasswordLayout.setVisibility(View.GONE);
                binding.newPasswordLayout.setVisibility(View.GONE);
                binding.textView.setText("You are using Google Sign-in \n" +
                        "Google Sign-in cannot change Password");
            } else {
                showPasswordWarningDialog();
            }
        }
    }

    private void getCurrentUser(String xNewPassword) {

        String hashedPassword = BCrypt.hashpw(xNewPassword, BCrypt.gensalt());
        StaticDataPasser.storeHashedPassword = hashedPassword;

        if (FirebaseMain.getUser() != null) {
            userID = FirebaseMain.getUser().getUid();

        } else {
            intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        }
    }

    private void showPasswordChangeSuccessDialog() {
        builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.password_change_success_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (passwordChangeSuccessDialog != null && passwordChangeSuccessDialog.isShowing()) {
                passwordChangeSuccessDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        passwordChangeSuccessDialog = builder.create();
        passwordChangeSuccessDialog.show();
    }

    private void showPasswordChangeFailedDialog() {
        builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.password_change_failed_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (passwordChangeFailedDialog != null && passwordChangeFailedDialog.isShowing()) {
                passwordChangeFailedDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        passwordChangeFailedDialog = builder.create();
        passwordChangeFailedDialog.show();
    }

    private void showPasswordWarningDialog() {
        builder = new AlertDialog.Builder(getContext());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_change_confirmation, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            if (passwordWarningDialog != null && passwordWarningDialog.isShowing()) {
                passwordWarningDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        passwordWarningDialog = builder.create();
        passwordWarningDialog.show();
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
}