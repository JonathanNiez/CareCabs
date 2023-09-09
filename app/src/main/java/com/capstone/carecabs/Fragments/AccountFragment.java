package com.capstone.carecabs.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.LoggingOut;
import com.capstone.carecabs.R;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentAccountBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Arrays;

public class AccountFragment extends Fragment {
    private DocumentReference documentReference;
    private final String TAG = "AccountFragment";
    private Intent intent;
    private AlertDialog signOutDialog, pleaseWaitDialog,
            noInternetDialog, profileInfoNotCompleteDialog;
    private AlertDialog.Builder builder;
    private NetworkChangeReceiver networkChangeReceiver;
    private Context context;
    private FragmentAccountBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        context = getContext();
        initializeNetworkChecker();

        FirebaseApp.initializeApp(context);
        searchTheFireStoreCollectionName();

        binding.editProfileBtn.setOnClickListener(v -> goToEditAccountFragment());

        binding.aboutBtn.setOnClickListener(v -> goToAboutFragment());

        binding.contactUsBtn.setOnClickListener(v -> goToContactUsFragment());

        binding.appSettingsBtn.setOnClickListener(v -> goToAppSettingsFragment());

        binding.changePasswordBtn.setOnClickListener(v -> goToChangePasswordFragment());

        binding.imgBackBtn.setOnClickListener(v -> backToHomeFragment());

        binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());

        return view;
    }

    private void logoutUser() {
        intent = new Intent(getActivity(), LoggingOut.class);
        startActivity(intent);
        getActivity().finish();

        StaticDataPasser.storeSelectedSex = null;
        StaticDataPasser.storeSelectedDisability = null;
        StaticDataPasser.storeCurrentBirthDate = null;
        StaticDataPasser.storeRegisterType = null;
        StaticDataPasser.storeRegisterUserType = null;
        StaticDataPasser.storeCurrentAge = 0;
        StaticDataPasser.storeFirstName = null;
        StaticDataPasser.storeLastName = null;
        StaticDataPasser.storeHashedPassword = null;
    }

    private void getTheUserType() {
        showPleaseWaitDialog();

        if (FirebaseMain.getUser() != null) {

            searchTheFireStoreCollectionName();

        } else {
            closePleaseWaitDialog();
            showIncompleteProfileInfoDialog();
        }
    }

    private void searchTheFireStoreCollectionName() {
        showPleaseWaitDialog();

        if (FirebaseMain.getUser() != null) {
            String getUserID = FirebaseMain.getUser().getUid();

            String[] collectionsToCheck = {"Drivers", "Senior Citizen", "PWD"};

            for (String collection : collectionsToCheck) {
                // Construct a reference to the user's document in the current collection
                documentReference = FirebaseMain.getFireStoreInstance()
                        .collection(collection).document(getUserID);

                documentReference.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists in this collection
                        String getUserType = documentSnapshot.getString("userType");
                        String getFirstName = documentSnapshot.getString("firstname");
                        String getLastName = documentSnapshot.getString("lastname");
                        String fullName = getFirstName + " " + getLastName;
                        // Do something with the user's data
                        // For example, display it or perform some action based on the collection
                        if (getUserType != null) {
                            // Determine the collection based on userType

                            switch (getUserType) {
                                case "Driver":
                                    binding.userTypeTextView.setText(getUserType);
                                    binding.fullNameTextView.setText(fullName);
                                    break;

                                case "Senior Citizen":
                                    binding.userTypeTextView.setText(getUserType);
                                    binding.fullNameTextView.setText(fullName);
                                    break;

                                case "PWD":
                                    binding.userTypeTextView.setText(getUserType);
                                    binding.fullNameTextView.setText(fullName);
                                    break;

                                default:
                                    binding.userTypeTextView.setText("Unknown");
                                    binding.fullNameTextView.setText("Unknown");
                                    break;
                            }
                        }

                        closePleaseWaitDialog();
                        Log.i(TAG, "Exists");

                    } else {

                        Log.e(TAG, "Not Exists");
                        closePleaseWaitDialog();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage());
                    closePleaseWaitDialog();
                });
            }

        } else {
            Log.e(TAG, "User is null");
            closePleaseWaitDialog();
        }
    }


    private void loadUserProfileInfo() {
        showPleaseWaitDialog();
        getTheUserType();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (networkChangeReceiver != null) {
            getContext().unregisterReceiver(networkChangeReceiver);
        }

        closeSignOutDialog();
        closePleaseWaitDialog();
        closeIncompleteProfileInfoDialog();
        closeNoInternetDialog();
    }


    @Override
    public void onPause() {
        super.onPause();

        closeSignOutDialog();
        closePleaseWaitDialog();
        closeIncompleteProfileInfoDialog();
        closeNoInternetDialog();
    }

    private void showIncompleteProfileInfoDialog() {
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.profile_info_not_complete_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            getTheUserType();

            intent = new Intent(getActivity(), LoggingOut.class);
            startActivity(intent);
            getActivity().finish();
        });

        builder.setView(dialogView);

        profileInfoNotCompleteDialog = builder.create();
        profileInfoNotCompleteDialog.show();
    }

    private void closeIncompleteProfileInfoDialog() {
        if (profileInfoNotCompleteDialog != null && profileInfoNotCompleteDialog.isShowing()) {
            profileInfoNotCompleteDialog.isShowing();
        }
    }

    private void showSignOutDialog() {
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sign_out, null);

        Button signOutBtn = dialogView.findViewById(R.id.signOutBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        signOutBtn.setOnClickListener(v -> {
            logoutUser();

        });

        cancelBtn.setOnClickListener(v -> {
            closeSignOutDialog();
        });

        builder.setView(dialogView);

        signOutDialog = builder.create();
        signOutDialog.show();
    }

    private void closeSignOutDialog() {
        if (signOutDialog != null && signOutDialog.isShowing()) {
            signOutDialog.dismiss();
        }
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


    private void goToEditAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new EditAccountFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToAppSettingsFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AppSettingsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToAboutFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new AboutFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToContactUsFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new ContactUsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToChangePasswordFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new ChangePasswordFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void backToHomeFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, new HomeFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showNoInternetDialog() {
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

        Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

        tryAgainBtn.setOnClickListener(v -> {
            closeNoInternetDialog();
        });

        builder.setView(dialogView);

        noInternetDialog = builder.create();
        noInternetDialog.show();
    }

    private void closeNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();

            boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(context);
            updateConnectionStatus(isConnected);
        }
    }

    private void initializeNetworkChecker() {
        networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkChangeListener() {
            @Override
            public void onNetworkChanged(boolean isConnected) {
                updateConnectionStatus(isConnected);
            }
        });

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(networkChangeReceiver, intentFilter);

        // Initial network status check
        boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(getContext());
        updateConnectionStatus(isConnected);

    }

    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
        } else {
            showNoInternetDialog();
        }
    }
}