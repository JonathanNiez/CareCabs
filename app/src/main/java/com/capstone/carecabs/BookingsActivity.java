package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.content.Intent;
import android.os.Bundle;

import com.capstone.carecabs.Fragments.BookingHistoryFragment;
import com.capstone.carecabs.Fragments.CurrentBookingFragment;
import com.capstone.carecabs.databinding.ActivityBookingsBinding;

import java.util.ArrayList;

public class BookingsActivity extends AppCompatActivity {
	private ActivityBookingsBinding binding;
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityBookingsBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		if (getIntent().hasExtra("dataSent")) {
			binding.imgBackBtn.setOnClickListener(v -> {
				finish();
			});
		} else {
			binding.imgBackBtn.setOnClickListener(v -> {
				intent = new Intent(BookingsActivity.this, MainActivity.class);
				startActivity(intent);
				finish();
			});
		}


		ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
		viewPagerAdapter.addFragment(new CurrentBookingFragment(), "Current");
		viewPagerAdapter.addFragment(new BookingHistoryFragment(), "History");

		binding.viewPager.setAdapter(viewPagerAdapter);
		binding.tabLayout.setupWithViewPager(binding.viewPager);
	}

	@Override
	public void onBackPressed() {
		checkIntent();
	}

	public void checkIntent() {
		if (getIntent().hasExtra("dataSent")) {
			finish();
		} else {
			intent = new Intent(BookingsActivity.this, MainActivity.class);
			startActivity(intent);
			finish();
		}
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