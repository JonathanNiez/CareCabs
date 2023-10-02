package com.capstone.carecabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.capstone.carecabs.Fragments.CurrentTripFragment;
import com.capstone.carecabs.Fragments.TripHistoryFragment;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.databinding.ActivityTripsOverviewBinding;

import java.util.ArrayList;

public class TripsOverviewActivity extends AppCompatActivity {
	private final String TAG = "TripsOverviewActivity";
	private AlertDialog noInternetDialog;
	private NetworkChangeReceiver networkChangeReceiver;
	private ActivityTripsOverviewBinding binding;

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();

	}

	@Override
	protected void onPause() {
		super.onPause();

		closeNoInternetDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeNoInternetDialog();
	}

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

	private void showNoInternetDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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
		registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
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