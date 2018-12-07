package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.service.MotionRecordingService;

import java.util.List;
import java.util.Locale;


public class LearningFragment extends FitnessAppFragment implements 
	View.OnClickListener,
	NumberPicker.OnValueChangeListener {
	
	private static final int SPEECH_REQUEST_CODE = 1;

	private TextView mTitle;
	private Button mButton;
	private ProgressBar mProgressBar;

	private String selectedExercise = "";

	private boolean mServiceBound = false;
	private MotionRecordingService mService;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MotionRecordingService.LocalBinder binder = (MotionRecordingService.LocalBinder) service;
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
		mButton.setEnabled(false);

		if (Utilities.isExternalStorageWritable()) {
			mTitle.setText(R.string.select_exercise);
			mButton.setOnClickListener(this);
			mProgressBar = rootView.findViewById(R.id.learn_progressBar);
			mProgressBar.getIndeterminateDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
			mProgressBar.setVisibility(View.GONE);
			
			Intent intent = new Intent(getActivity(), MotionRecordingService.class);
			getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		}
		else {
			mTitle.setText(R.string.error__no_storage);
		}

		return rootView;
	}

	private void enableSelectedExercise(String name) {
		this.selectedExercise = name;
		this.mTitle.setText(selectedExercise);
		if (mButton != null) {
			mButton.setEnabled(Utilities.isExternalStorageWritable());
		}
	}

	@Override
	public void onClick(View view) {
		if(mButton.isEnabled() && mServiceBound) {
			MotionRecordingService.RecordingStatus status = 
				mService.getRecordingStatus();
			if (status == MotionRecordingService.RecordingStatus.STOPPED) {
				showStartCountDownDialog();
			}
			else if (status == MotionRecordingService.RecordingStatus.RECORDING) {
				mService.stopRepRecording();
				indicateRepProcessing(false);
				showRepCountPickerDialog();
			}
		}
	}
	
	public void showRepCountPickerDialog() {
		NumberPickerDialog newFragment = new NumberPickerDialog();
		newFragment.setValueChangeListener(this);
		newFragment.show(getFragmentManager(), "rep count");
	}
	
	public void showStartCountDownDialog() {
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
		alertDialog.setTitle("Start in");
		alertDialog.setMessage("3");
		alertDialog.show();

		new CountDownTimer(3000, 100) {
			@Override
			public void onTick(long millisUntilFinished) {
				alertDialog.setMessage(String.format(Locale.ENGLISH,"%.1f",millisUntilFinished / (float)1000));
			}

			@Override
			public void onFinish() {
				alertDialog.dismiss();
				startRecording();
			}
		}.start();
	}

	public void startRecording() {
		indicateRepProcessing(true);
		if (this.mService.startRepRecording(this.selectedExercise)) {
			Utilities.vibrate(getActivity(), null);
		} 
		else {
			indicateRepProcessing(false);
		}
	}

	public void indicateRepProcessing(boolean processing){
		if (this.mProgressBar != null) {
			this.mProgressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
		}
		if (this.mButton != null) {
			mButton.setSelected(processing);
			mButton.setPressed(processing);
		}
		if (this.mTitle != null) {
			this.mTitle.setText(processing ? getString(R.string.complete_rep) : selectedExercise );
		}
	}

	@Override
	public void onValueChange(NumberPicker numberPicker, int i, int i1) {
		//AFTER NUMBER PICKER DIALOG
		if (mServiceBound) {
			this.mService.setRepsForFinishedRecording(numberPicker.getValue());
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
				this.enableSelectedExercise(menuItem.getTitle().toString());
				break;
			case R.id.menu_custom:
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				startActivityForResult(intent, SPEECH_REQUEST_CODE);
				break;
			case R.id.reset_current:
			case R.id.reset_all:
				if (mServiceBound) {
					mService.deleteData();
				}
				break;
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_REQUEST_CODE) {
			try {
				List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				this.enableSelectedExercise(results.get(0));
			} catch (NullPointerException ignored) {
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
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
