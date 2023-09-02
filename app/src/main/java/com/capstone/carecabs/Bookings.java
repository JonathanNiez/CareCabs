package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.widget.ImageButton;

import com.capstone.carecabs.Fragments.BookingHistoryFragment;
import com.capstone.carecabs.Fragments.PendingBookingFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class Bookings extends AppCompatActivity {

    private ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager viewPager = findViewById(R.id.viewPager);

        imageButton = findViewById(R.id.imgBackBtn);

        imageButton.setOnClickListener(v -> {
            finish();
        });

        //Add the Fragments
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new PendingBookingFragment(), "Pending");
        viewPagerAdapter.addFragment(new BookingHistoryFragment(), "History");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragmentArrayList;
        private ArrayList<String> stringArrayList;

        ViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragmentArrayList = new ArrayList<>();
            this.stringArrayList = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentArrayList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentArrayList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentArrayList.add(fragment);
            stringArrayList.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return stringArrayList.get(position);
        }
    }

}