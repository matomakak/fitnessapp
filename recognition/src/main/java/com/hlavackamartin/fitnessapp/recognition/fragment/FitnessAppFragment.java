package com.hlavackamartin.fitnessapp.recognition.fragment;

import android.app.Fragment;
import android.support.wearable.activity.WearableActivityDelegate;
import android.view.MenuItem;


public abstract class FitnessAppFragment extends Fragment implements 
	WearableActivityDelegate.AmbientCallback,
	MenuItem.OnMenuItemClickListener {
	
	public abstract int getActionMenu();
	
}
