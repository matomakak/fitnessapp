package com.hlavackamartin.fitnessapp.recognition.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.service.MotionRecorderService;

import java.util.List;


public class LearningFragment extends SensorEnabledFragment implements
	MenuItem.OnMenuItemClickListener,
	View.OnClickListener,
	WearableNavigationDrawerView.OnItemSelectedListener{
	
	private static final int SPEECH_REQUEST_CODE = 1;
	private WearableActionDrawerView mWearableActionDrawer;

	private TextView mTitle;
	private Button mButton;
	private ProgressBar mProgressBar;

	private String selectedExercise = "";

	private boolean mServiceBound = false;
	private MotionRecorderService mService;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MotionRecorderService.LocalBinder binder = (MotionRecorderService.LocalBinder) service;
			mService = binder.getService();
			mServiceBound = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mServiceBound = false;
		}
	};
	
	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_learn, container, false);

		WearableNavigationDrawerView mWearableNavigationDrawer = rootView.findViewById(R.id.learn_top_nav);
		mWearableNavigationDrawer.setAdapter(new NavigationAdapter(getContext()));
		// Peeks navigation drawer on the top.
		mWearableNavigationDrawer.getController().peekDrawer();
		mWearableNavigationDrawer.addOnItemSelectedListener(this);

		mWearableActionDrawer = rootView.findViewById(R.id.learn_bottom_nav);
		// Peeks action drawer on the bottom.
		mWearableActionDrawer.getController().peekDrawer();
		mWearableActionDrawer.setOnMenuItemClickListener(this);

		mTitle = rootView.findViewById(R.id.learn_title);
		mButton = rootView.findViewById(R.id.learn_btn);

		if (Utilities.isExternalStorageWritable()) {
			mTitle.setText(selectedExercise);
			mButton.setOnClickListener(this);
			mProgressBar = rootView.findViewById(R.id.learn_progressBar);
			mProgressBar.getIndeterminateDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);
			// Enables Always-on

			Intent intent = new Intent(getActivity(), MotionRecorderService.class);
			getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		}
		else {
			mTitle.setText(R.string.error__no_storage);
			mButton.setEnabled(false);
		}

		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (mButton != null) {
			mButton.setEnabled(Utilities.isExternalStorageWritable());
		}
	}
	
	@Override
	public void onItemSelected(int pos) {
		if (pos == 0) {
			this.mService.forgetLast(this.selectedExercise);
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.exercise_unknown) {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			startActivityForResult(intent, SPEECH_REQUEST_CODE);
		}
		this.mWearableActionDrawer.getController().closeDrawer();
		updateSelectedExercise(menuItem.getTitle().toString());
		return true;
	}

	private void updateSelectedExercise(String name) {
		this.selectedExercise = name;
		this.mTitle.setText(selectedExercise);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_REQUEST_CODE) {
			try {
				List<String> results = data.getStringArrayListExtra(
					RecognizerIntent.EXTRA_RESULTS);
				this.updateSelectedExercise(results.get(0));
			} catch (NullPointerException ex) {
				//TODO log
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View view) {
		if(mServiceBound) {
			boolean inProgress = this.mService.executeRepRecording(this.selectedExercise);
			indicateRepProcessing(inProgress);
		}
	}

	public void indicateRepProcessing(boolean processing){
		if(this.mProgressBar != null)
			this.mProgressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
		if(this.mButton != null) {
			this.mButton.setText(processing ? R.string.complete_rep : R.string.record_rep);
		}
	}

	@Override
	public void onPause() {
		endTask();
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		endTask();
		super.onDestroy();
	}

	private void endTask(){
		if (mServiceBound) {
			getActivity().unbindService(mServiceConnection);
			mServiceBound = false;
		}
	}
	
	private final class NavigationAdapter
		extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

		private final Context mContext;

		NavigationAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return 1;
		}

		@Override
		public String getItemText(int pos) {
			return getResources().getString(R.string.forget);
		}

		@Override
		public Drawable getItemDrawable(int pos) {
			return mContext.getDrawable(R.drawable.btn_end_normal_200);
		}
	}
}
