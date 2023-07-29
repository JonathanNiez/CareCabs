package com.capstone.carecabs.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.capstone.carecabs.Adapters.CarouselPagerAdapter;
import com.capstone.carecabs.GetStarted;
import com.capstone.carecabs.Login;
import com.capstone.carecabs.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private TextView usernameTextView;
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

        slideFragments.add(new CarouselFragment1());
        slideFragments.add(new CarouselFragment2());

        CarouselPagerAdapter adapter = new CarouselPagerAdapter(getChildFragmentManager(), slideFragments);
        viewPager.setAdapter(adapter);

        startAutoSlide();

        return view;
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