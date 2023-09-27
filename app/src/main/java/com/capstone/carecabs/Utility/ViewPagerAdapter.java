package com.capstone.carecabs.Utility;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

public class ViewPagerAdapter extends PagerAdapter {
	private final ArrayList<Fragment> fragmentArrayList;
	private final ArrayList<String> stringArrayList;

	public ViewPagerAdapter(FragmentManager fragmentManager) {
		this.fragmentArrayList = new ArrayList<>();
		this.stringArrayList = new ArrayList<>();
	}

	@NonNull
	public Fragment getItem(int position) {
		return fragmentArrayList.get(position);
	}

	public int getCount() {
		return fragmentArrayList.size();
	}

	@Override
	public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		return false;
	}

	public void addFragment(Fragment fragment, String title) {
		fragmentArrayList.add(fragment);
		stringArrayList.add(title);
	}

	@Nullable
	public CharSequence getPageTitle(int position) {
		return stringArrayList.get(position);
	}

}
