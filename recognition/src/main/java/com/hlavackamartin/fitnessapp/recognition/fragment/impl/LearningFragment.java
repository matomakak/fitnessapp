package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.service.MotionRecorderService;

import java.util.List;


public class LearningFragment extends FitnessAppFragment implements View.OnClickListener{
	
	private static final int SPEECH_REQUEST_CODE = 1;

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
	
	public LearningFragment() {}
	
	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_learn, container, false);

		mTitle = rootView.findViewById(R.id.learn_title);
		mButton = rootView.findViewById(R.id.learn_btn);

		if (Utilities.isExternalStorageWritable()) {
			mTitle.setText(R.string.record_rep);
			mButton.setOnClickListener(this);
			mProgressBar = rootView.findViewById(R.id.learn_progressBar);
			mProgressBar.getIndeterminateDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);
			
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

	//TODO exercise unknown?
	/*@Override
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
	}*/

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
		if (this.mProgressBar != null) {
			this.mProgressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
		}
		if (this.mButton != null) {
			this.mButton.setBackground(getActivity().getDrawable(
				processing ? R.drawable.btn_pause_normal_200 : R.drawable.btn_start_normal_200));
		}
		if (this.mTitle != null) {
			this.mTitle.setText(processing ? R.string.complete_rep : R.string.record_rep);
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

	private void executeReset() {
		if (mServiceBound) {
			this.mService.forgetLast(this.selectedExercise);
		}
	}

	private void executeResetAll() {
		if (mServiceBound) {
			this.mService.forgetAll();
		}
	}

	@Override
	public int getActionMenu() {
		return R.menu.learning_type_menu;
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case R.id.menu_backflips:
			case R.id.menu_squats:
			case R.id.menu_unknown:
				this.selectedExercise = menuItem.getTitle().toString();
				this.mTitle.setText(selectedExercise);
				break;
			case R.id.reset_current:
				executeReset();
				break;
			case R.id.reset_all:
				executeResetAll();
				break;
		}
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (mServiceBound) {
			mService.onSensorChanged(sensorEvent);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
		if (mServiceBound) {
			mService.onAccuracyChanged(sensor, i);
		}
	}

	@Override
	public void onEnterAmbient(Bundle bundle) {
		//TODO ambient
	}

	@Override
	public void onUpdateAmbient() {
		//TODO ambient
	}

	@Override
	public void onExitAmbient() {
		//TODO ambient
	}
}
