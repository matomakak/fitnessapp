package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.activity.LearnActivity;
import com.hlavackamartin.fitnessapp.recognition.data.Exercise;
import com.hlavackamartin.fitnessapp.recognition.data.HeartRateData;
import com.hlavackamartin.fitnessapp.recognition.fragment.SensorEnabledFragment;
import com.hlavackamartin.fitnessapp.recognition.provider.ActivityInference;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hlavackamartin.fitnessapp.recognition.Utilities.toFloatArray;
import static java.lang.Math.round;


public class RecognitionFragment extends SensorEnabledFragment implements
	MenuItem.OnMenuItemClickListener,
	WearableNavigationDrawerView.OnItemSelectedListener,
	View.OnClickListener{
	
	private TensorFlowInferenceInterface inferenceInterface;
	
	private String mSelectedExercise;
	private ValueType mValueShowing = ValueType.REPS;

	private TextView mTitle;
	private TextView mValue;

	private final int N_SAMPLES = 90;
	private static List<Float> x;
	private static List<Float> y;
	private static List<Float> z;
	private static List<Float> input_signal;

	private ActivityInference activityInference;
	private Map<String,Exercise> exerciseStats = new HashMap();
	private HeartRateData heartRateData = new HeartRateData();

	private List<String> mLabels;


	@Override
	public void onClick(View view) {
		mValueShowing = mValueShowing.next();
		updateValue();
	}
	
	public void onSensorChanged(SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				this.onMotionChanged(sensorEvent.values);
				break;
			case Sensor.TYPE_HEART_RATE:
				this.onHeartRateChanged((int) sensorEvent.values[0]);
				break;
		}
		updateValue();
	}
	
	private void onMotionChanged (float[] values) {
		activityPrediction();
		x.add(values[0]);
		y.add(values[1]);
		z.add(values[2]);
	}

	private void onHeartRateChanged (int tHearRate) {
		heartRateData.updateHR(tHearRate);
	}

	private void updateValue() {
		Exercise exercise = exerciseStats.get(mSelectedExercise);
		String value = "...";
		switch (mValueShowing) {
			case HR:
				if (heartRateData.getCurrentHR() > 30)
					value = heartRateData.getCurrentHR().toString();
				break;
			case AVG_HR:
				value = heartRateData.getAvgHR().toString();
				break;
			case MAX_HR:
				value = heartRateData.getMaxHR().toString();
				break;
			case REPS:
				if (exercise != null) {
					value = exercise.getReps().toString();
				}
				break;
		}
		mValue.setText(value);
	}

	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_main, container, false);

		mSelectedExercise = "";

		//TODO add to mLabels Utilities.readRecognitionLabels(getContext());

		mTitle = rootView.findViewById(R.id.detect_title);
		mValue = rootView.findViewById(R.id.detect_value);
		mValue.setOnClickListener(this);

		x = new ArrayList<>();
		y = new ArrayList<>();
		z = new ArrayList<>();
		input_signal = new ArrayList<>();
		activityInference = new ActivityInference(getContext());
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	private void activityPrediction() {
		if(x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
			// Copy all x,y and z values to one array of shape N_SAMPLES*3
			input_signal.addAll(x); input_signal.addAll(y); input_signal.addAll(z);
			// Perform inference using Tensorflow
			float[] results = activityInference.getActivityProb(toFloatArray(input_signal));

			for (int i=0; i<mLabels.size(); i++) {
				if (round(results[0]) > 0.7) {
					getExerciseStat(mLabels.get(i)).addRep();
				}
			}
			// Clear all the values
			x.clear(); y.clear(); z.clear(); input_signal.clear();
		}
	}

	private Exercise getExerciseStat(String name) {
		Exercise stat = exerciseStats.get(name);
		if (stat != null) {
			stat = new Exercise(name);
			exerciseStats.put(name, stat);
		}
		return stat;
	}

	public enum ValueType {
		REPS,
		HR,
		AVG_HR,
		MAX_HR;

		private static ValueType[] vals = values();

		public ValueType next() {
			return vals[(this.ordinal() + 1) % vals.length];
		}
	}
}
