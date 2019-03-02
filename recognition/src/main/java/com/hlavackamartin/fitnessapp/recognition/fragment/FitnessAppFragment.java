package com.hlavackamartin.fitnessapp.recognition.fragment;

import android.app.Fragment;
import android.content.res.Resources;
import android.support.wearable.activity.WearableActivityDelegate;
import android.view.MenuItem;

import java.util.List;


public abstract class FitnessAppFragment extends Fragment implements 
	WearableActivityDelegate.AmbientCallback,
	MenuItem.OnMenuItemClickListener {

	public abstract List<String> getActionMenu(Resources resources);
	
}
