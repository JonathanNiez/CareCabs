package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.os.Bundle;

import com.capstone.carecabs.Fragments.BookingHistoryFragment;
import com.capstone.carecabs.Fragments.CurrentTripFragment;
import com.capstone.carecabs.Fragments.PendingBookingFragment;
import com.capstone.carecabs.Fragments.TripHistoryFragment;
import com.capstone.carecabs.Utility.ViewPagerAdapter;
import com.capstone.carecabs.databinding.ActivityTripsOverviewBinding;

import java.util.ArrayList;

public class TripsOverviewActivity extends AppCompatActivity {
	private ActivityTripsOverviewBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityTripsOverviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.imgBackBtn.setOnClickListener(v -> {
			finish();
		});

		ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
		viewPagerAdapter.addFragment(new CurrentTripFragment(), "Current");
		viewPagerAdapter.addFragment(new TripHistoryFragment(), "History");

		binding.viewPager.setAdapter(viewPagerAdapter);
		binding.tabLayout.setupWithViewPager(binding.viewPager);
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	public class ViewPagerAdapter extends FragmentPagerAdapter {
		private final ArrayList<Fragment> fragmentArrayList;
		private final ArrayList<String> stringArrayList;

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