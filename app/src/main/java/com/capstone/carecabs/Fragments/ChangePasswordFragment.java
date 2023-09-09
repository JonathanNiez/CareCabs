package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import org.mindrot.jbcrypt.BCrypt;

public class ChangePasswordFragment extends Fragment {

    private ImageButton imgBackBtn;
    private Button changePasswordBtn, okBtn;
    private EditText oldPassword, newPassword;
    private TextInputLayout oldPasswordLayout, newPasswordLayout;
    private TextView textView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private AlertDialog.Builder builder;
    private AlertDialog passwordWarningDialog, passwordChangeSuccessDialog,
            passwordChangeFailedDialog;
    private String userID;
    private Intent intent;
    private String TAG = "ChangePasswordFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        imgBackBtn = view.findViewById(R.id.imgBackBtn);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        okBtn = view.findViewById(R.id.okBtn);
        oldPassword = view.findViewById(R.id.oldPassword);
        newPassword = view.findViewById(R.id.newPassword);
        newPasswordLayout = view.findViewById(R.id.newPasswordLayout);
        oldPasswordLayout = view.findViewById(R.id.oldPasswordLayout);
        textView = view.findViewById(R.id.textView);

        checkUserSignInMethod();

        imgBackBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        okBtn.setOnClickListener(v -> {
            backToAccountFragment();
        });

        changePasswordBtn.setOnClickListener(v -> {
            String stringOldPassword = oldPassword.getText().toString();
            String stringNewPassword = newPassword.getText().toString();

            if (stringOldPassword.isEmpty()) {
                oldPassword.setError("Please Enter your Old Password");
            } else if (stringNewPassword.isEmpty()) {
                newPassword.setError("Please Enter your New Password");
            } else {
                if (currentUser != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), stringOldPassword);
                    currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentUser.updatePassword(stringNewPassword)
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
        if (currentUser != null) {
            boolean isGoogleSignIn = false;

            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (userInfo.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {

                    isGoogleSignIn = true;

                    break;

                }
            }

            if (isGoogleSignIn) {
                changePasswordBtn.setVisibility(View.GONE);
                okBtn.setVisibility(View.VISIBLE);
                oldPasswordLayout.setVisibility(View.GONE);
                newPasswordLayout.setVisibility(View.GONE);
                textView.setText("You are using Google Sign-in \n" +
                        "Google Sign-in cannot change Password");
            } else {
                showPasswordWarningDialog();
            }
        }
    }

    private void getCurrentUser(String xNewPassword) {

        String hashedPassword = BCrypt.hashpw(xNewPassword, BCrypt.gensalt());
        StaticDataPasser.storeHashedPassword = hashedPassword;

        if (currentUser != null) {
            userID = currentUser.getUid();


        } else {
            intent = new Intent(getActivity(), Login.class);
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

        View dialogView = getLayoutInflater().inflate(R.layout.change_password_warning_dialog, null);

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