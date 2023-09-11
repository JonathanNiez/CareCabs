package com.capstone.carecabs.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.capstone.carecabs.Adapters.CarouselPagerAdapter;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.capstone.carecabs.RegisterDriver;
import com.capstone.carecabs.RegisterPWD;
import com.capstone.carecabs.RegisterSenior;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment";
    private int currentPage = 0;
    private final long AUTOSLIDE_DELAY = 3000; // Delay in milliseconds (3 seconds)
    private Handler handler;
    private Runnable runnable;
    private List<Fragment> slideFragments = new ArrayList<>();
    private AlertDialog noInternetDialog, registerNotCompleteDialog;
    private AlertDialog.Builder builder;
    private Context context;
    private Intent intent;
    private NetworkChangeReceiver networkChangeReceiver;
    private FragmentHomeBinding binding;
    private DocumentReference documentReference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        context = getContext();
        initializeNetworkChecker();

        checkUserIfRegisterComplete();

        FirebaseApp.initializeApp(context);

        slideFragments.add(new CarouselFragment1());
        slideFragments.add(new CarouselFragment2());
        slideFragments.add(new CarouselFragment3());
        slideFragments.add(new CarouselFragment4());

        CarouselPagerAdapter adapter = new CarouselPagerAdapter(getChildFragmentManager(), slideFragments);
        binding.viewPager.setAdapter(adapter);

        startAutoSlide();
        getCurrentTime();

        return view;
    }

    private void getCurrentTime() {
        // Get the current time
        LocalDateTime currentTime = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            currentTime = LocalDateTime.now();
        }

        // Get the hour of the day
        int hour = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            hour = currentTime.getHour();
        }

        // Set the greeting message based on the time
        String greeting;
        if (hour >= 0 && hour < 12) {
            greeting = "Good Morning!";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good Afternoon!";
        } else {
            greeting = "Good Evening!";
        }

        // Display the greeting message and formatted time in a TextView
        String amPm = (hour < 12) ? "AM" : "PM";
        if (hour > 12) {
            hour -= 12;
        } else if (hour == 0) {
            hour = 12;
        }

        // Format the time
        String formattedTime = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            formattedTime = String.format("%02d:%02d %s", hour, currentTime.getMinute(), amPm);
        }

        // Concatenate the greeting message and formatted time
        binding.greetTextView.setText(greeting);
        binding.currentTimeTextView.setText("The time is " + formattedTime);
    }

    private void startAutoSlide() {

        if (handler == null) {
            handler = new Handler();
        }

        if (runnable == null) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    currentPage = (currentPage + 1) % slideFragments.size();
                    binding.viewPager.setCurrentItem(currentPage, true);
                    handler.postDelayed(this, AUTOSLIDE_DELAY);
                }
            };
        }

        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, AUTOSLIDE_DELAY);
    }

    private void stopAutoSlide() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopAutoSlide();
        closeNoInternetDialog();
        closeRegisterNotCompleteDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAutoSlide();
        closeNoInternetDialog();
        closeRegisterNotCompleteDialog();
    }

    @Override
    public void onStart() {
        super.onStart();

        startAutoSlide();
    }

    private void checkUserIfRegisterComplete(){
        if (FirebaseMain.getUser() != null){
            String getUserID = FirebaseMain.getUser().getUid();
            documentReference = FirebaseMain.getFireStoreInstance()
                    .collection(StaticDataPasser.userCollection).document(getUserID);
            documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()){
                        boolean getUserRegisterStatus = documentSnapshot.getBoolean("isRegisterComplete");
                        String getRegisterUserType = documentSnapshot.getString("userType");

                        StaticDataPasser.storeUserType = getRegisterUserType;

                        if (!getUserRegisterStatus){
                            showRegisterNotCompleteDialog();
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            });

        }else{
            FirebaseMain.signOutUser();

            Intent intent = new Intent(context, Login.class);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void showRegisterNotCompleteDialog() {
        builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.profile_info_not_complete_dialog, null);

        Button okBtn = dialogView.findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> {
            switch (StaticDataPasser.storeUserType) {
                case "Driver":
                    intent = new Intent(getActivity(), RegisterDriver.class);

                    break;

                case "Senior Citizen":
                    intent = new Intent(getActivity(), RegisterSenior.class);

                    break;

                case "Persons with Disabilities (PWD)":
                    intent = new Intent(getActivity(), RegisterPWD.class);

                    break;

            }
            startActivity(intent);
            getActivity().finish();
        });

        builder.setView(dialogView);

        registerNotCompleteDialog = builder.create();
        registerNotCompleteDialog.show();
    }

    private void closeRegisterNotCompleteDialog(){
        if (registerNotCompleteDialog != null && registerNotCompleteDialog.isShowing()){
            registerNotCompleteDialog.dismiss();
        }
    }

    private void showNoInternetDialog() {

        builder = new AlertDialog.Builder(getContext());
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