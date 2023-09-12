package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.os.Bundle;

import com.capstone.carecabs.Fragments.BookingHistoryFragment;
import com.capstone.carecabs.Fragments.PendingBookingFragment;
import com.capstone.carecabs.databinding.ActivityBookingsBinding;

import java.util.ArrayList;

public class BookingsActivity extends AppCompatActivity {
    private ActivityBookingsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imgBackBtn.setOnClickListener(v -> {
            finish();
        });

        //Add the Fragments
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new PendingBookingFragment(), "Pending");
        viewPagerAdapter.addFragment(new BookingHistoryFragment(), "History");

        binding.viewPager.setAdapter(viewPagerAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
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