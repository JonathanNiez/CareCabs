package com.capstone.carecabs.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.capstone.carecabs.Adapters.CarouselPagerAdapter;
import com.capstone.carecabs.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView currentTimeTextView, greetTextView, usernameTextView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String userID;
    private String TAG = "HomeFragment";
    private ViewPager viewPager;
    private int currentPage = 0;
    private final long AUTOSLIDE_DELAY = 3000; // Delay in milliseconds (3 seconds)
    private Handler handler;
    private Runnable runnable;
    private List<Fragment> slideFragments = new ArrayList<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        viewPager = view.findViewById(R.id.viewPager);
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView);
        greetTextView = view.findViewById(R.id.greetTextView);

        slideFragments.add(new CarouselFragment1());
        slideFragments.add(new CarouselFragment2());
        slideFragments.add(new CarouselFragment3());
        slideFragments.add(new CarouselFragment4());

        CarouselPagerAdapter adapter = new CarouselPagerAdapter(getChildFragmentManager(), slideFragments);
        viewPager.setAdapter(adapter);

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
        greetTextView.setText(greeting);
        currentTimeTextView.setText("The time is " + formattedTime);
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
                    viewPager.setCurrentItem(currentPage, true);
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
    }

    @Override
    public void onStart() {
        super.onStart();

        startAutoSlide();
    }
}