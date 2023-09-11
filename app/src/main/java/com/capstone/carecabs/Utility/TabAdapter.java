package com.capstone.carecabs.Utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class TabAdapter extends FragmentPagerAdapter {
	private ArrayList<Fragment> fragmentArrayList;
	private ArrayList<String> stringArrayList;

	public TabAdapter(FragmentManager fragmentManager) {
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
