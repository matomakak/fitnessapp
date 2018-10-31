package com.hlavackamartin.fitnessapp.recognition.activity;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.data.MainMenuItem;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.fragment.impl.LearningFragment;
import com.hlavackamartin.fitnessapp.recognition.fragment.impl.DetectionFragment;
import com.hlavackamartin.fitnessapp.recognition.task.SynchronizeTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainActivity extends WearableActivity implements
	SensorEventListener,
	WearableNavigationDrawerView.OnItemSelectedListener,
	MenuItem.OnMenuItemClickListener {

	private List<FitnessAppFragment> mFragments = new ArrayList<>();
	private int mActiveFragment;

	private SensorManager mSensorManager;

	private NavigationAdapter mNavigationAdapter;
	private WearableNavigationDrawerView mWearableNavigationDrawer;
	private WearableActionDrawerView mWearableActionDrawer;
	
	@Override
	protected void onDestroy() {
		mSensorManager.unregisterListener(this);
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		// Top Navigation Drawer
		mWearableNavigationDrawer = findViewById(R.id.top_nav);
		mNavigationAdapter = new NavigationAdapter(this);
		mWearableNavigationDrawer.setAdapter(mNavigationAdapter);
		mWearableNavigationDrawer.getController().peekDrawer();
		mWearableNavigationDrawer.addOnItemSelectedListener(this);

		// Bottom Action Drawer
		mWearableActionDrawer = findViewById(R.id.bottom_nav);
		// Peeks action drawer on the bottom.
		mWearableActionDrawer.getController().peekDrawer();
		mWearableActionDrawer.setOnMenuItemClickListener(this);
		
		mFragments.add(new DetectionFragment());
		mFragments.add(new LearningFragment());
		mActiveFragment = 1;
		updateCurrentFragment();
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		initializeSensor(Sensor.TYPE_ACCELEROMETER);
		initializeSensor(Sensor.TYPE_HEART_RATE);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setAmbientEnabled();
	}
	
	private void updateCurrentFragment() {
		FragmentManager fragmentManager = getFragmentManager();
		FitnessAppFragment fragment = getCurrentFragment().get();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
		
		getMenuInflater().inflate(fragment.getActionMenu(), mWearableActionDrawer.getMenu());
	}
	
	private Optional<FitnessAppFragment> getCurrentFragment() {
		return Optional.ofNullable(mFragments.get(mActiveFragment));
	}

	private void initializeSensor(int sensorType) {
		Sensor mSensor = mSensorManager.getDefaultSensor(sensorType);
		if (mSensor != null)
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		getCurrentFragment().ifPresent(f -> f.onSensorChanged(sensorEvent));
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
		getCurrentFragment().ifPresent(f -> f.onAccuracyChanged(sensor, i));
	}

	@Override
	public void onItemSelected(int position) {
		mNavigationAdapter.executeAction(position);
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		return getCurrentFragment().isPresent() && getCurrentFragment().get().onMenuItemClick(menuItem);
	}

	@Override
	public void onEnterAmbient(Bundle ambientDetails) {
		super.onEnterAmbient(ambientDetails);
		Log.d("", "onEnterAmbient() " + ambientDetails);
		getCurrentFragment().ifPresent(f -> f.onEnterAmbient(ambientDetails));
		mWearableNavigationDrawer.getController().closeDrawer();
		mWearableActionDrawer.getController().closeDrawer();
	}

	@Override
	public void onExitAmbient() {
		super.onExitAmbient();
		Log.d("", "onExitAmbient()");
		getCurrentFragment().ifPresent(FitnessAppFragment::onExitAmbient);
		mWearableActionDrawer.getController().peekDrawer();
	}

	private void executeSyncTask() {
		ProgressDialog mProgressDialog;

		mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.DOWNLOAD);
		final SynchronizeTask downloadTask =
			new SynchronizeTask(this, mProgressDialog, SynchronizeTask.SyncActionType.DOWNLOAD);
		downloadTask.execute();

		mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.UPLOAD);
		final SynchronizeTask uploadTask =
			new SynchronizeTask(this, mProgressDialog, SynchronizeTask.SyncActionType.UPLOAD);
		uploadTask.execute();

		Utilities.readRecognitionLabels(this);
	}

	private ProgressDialog createSyncDialog(SynchronizeTask.SyncActionType type) {
		ProgressDialog mProgressDialog;
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage(getString(
			type == SynchronizeTask.SyncActionType.UPLOAD ? R.string.upload_message : R.string.download_message
		));
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(true);
		return mProgressDialog;
	}

	private final class NavigationAdapter
		extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

		private final MainActivity mContext;
		private final ArrayList<MainMenuItem> mMainMenu;

		NavigationAdapter(MainActivity context) {
			mContext = context;
			mMainMenu = new ArrayList<>();

			for (MainMenuItem.MenuType type : MainMenuItem.MenuType.values()) {
				int resourceId =
					mContext.getResources()
						.getIdentifier(type.getName(), "array", getPackageName());
				String[] details = mContext.getResources().getStringArray(resourceId);

				mMainMenu.add(new MainMenuItem(type, details[0], details[1]));
			}
		}

		@Override
		public int getCount() {
			return mMainMenu.size();
		}

		@Override
		public String getItemText(int pos) {
			return mMainMenu.get(pos).getName();
		}

		@Override
		public Drawable getItemDrawable(int pos) {
			String navigationIcon = mMainMenu.get(pos).getImage();

			int drawableNavigationIconId =
				mContext.getResources()
					.getIdentifier(navigationIcon, "drawable", getPackageName());

			return mContext.getDrawable(drawableNavigationIconId);
		}

		void executeAction(int pos) {
			switch (mMainMenu.get(pos).getType()) {
				case LEARNING:
					mActiveFragment = 0;
					updateCurrentFragment();					
					break;
				case RECOGNITION:
					mActiveFragment = 1;
					updateCurrentFragment();
					break;
				case SYNC:
					mContext.executeSyncTask();
					break;
				case EXIT:
					//TODO finish only or show some summary?
					mContext.finish();
					break;
			}
		}
	}
}
