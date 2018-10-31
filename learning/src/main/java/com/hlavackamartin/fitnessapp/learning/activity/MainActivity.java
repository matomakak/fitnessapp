package com.hlavackamartin.fitnessapp.learning;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener, View.OnClickListener {

	private WearableActionDrawerView mWearableActionDrawer;
	private Button mButton;
	private ProgressBar mProgressBar;
	
	private String selectedExercise;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mButton = findViewById(R.id.btn);
		mButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progressBar1);
		mProgressBar.getIndeterminateDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);
		
		mWearableActionDrawer = findViewById(R.id.bottom_action_drawer);
		// Peeks action drawer on the bottom.
		mWearableActionDrawer.getController().peekDrawer();
		mWearableActionDrawer.setOnMenuItemClickListener(this);

		// Enables Always-on
		setAmbientEnabled();
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		this.selectedExercise = menuItem.getTitle().toString();
		mWearableActionDrawer.getController().closeDrawer();
		return true;
	}

	@Override
	public void onClick(View view) {
		setProgressBarVisible(true);
	}
	/**
	 * @param visible boolean value for setting visibility of progressbar around button
	 */
	public void setProgressBarVisible(boolean visible){
		if(mProgressBar != null)
			mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
}
