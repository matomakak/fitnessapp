package com.hlavackamartin.fitnessapp.recognition;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.activity.ActivityInference;
import com.hlavackamartin.fitnessapp.recognition.fragment.Exercise;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements
	SensorEventListener,
	MenuItem.OnMenuItemClickListener,
	WearableNavigationDrawerView.OnItemSelectedListener, 
	View.OnClickListener{
	
	private TensorFlowInferenceInterface inferenceInterface;
	
	private SensorManager mSensorManager;

	private WearableNavigationDrawerView mWearableNavigationDrawer;
	private WearableActionDrawerView mWearableActionDrawer;

	private ArrayList<Exercise> mExerciseList;
	private int mSelectedExercise;
	private ValueType mValueShowing = ValueType.REPS;
	
	private TextView mTitle;
	private TextView mValue;
	
	private ActivityInference activityInference;

	@Override
	protected void onDestroy() {
		//TODO UNREGISTER AND CLOSE
		mSensorManager.unregisterListener(this);
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//TODO try to obtain new data from server, so that we can always update them
		super.onCreate(savedInstanceState);
		setContentView(com.hlavackamartin.fitnessapp.recognition.R.layout.activity_main);

		initializeExerciseList();
		// Top Navigation Drawer
		mWearableNavigationDrawer = findViewById(R.id.top_navigation_drawer);
		mWearableNavigationDrawer.setAdapter(new NavigationAdapter(this));
		// Peeks navigation drawer on the top.
		mWearableNavigationDrawer.getController().peekDrawer();
		mWearableNavigationDrawer.addOnItemSelectedListener(this);

		// Bottom Action Drawer
		mWearableActionDrawer = findViewById(R.id.bottom_action_drawer);
		// Peeks action drawer on the bottom.
		mWearableActionDrawer.getController().peekDrawer();
		mWearableActionDrawer.setOnMenuItemClickListener(this);
		
		mTitle = findViewById(R.id.title);
		mValue = findViewById(R.id.text);
		mValue.setOnClickListener(this);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		initializeSensor(Sensor.TYPE_ACCELEROMETER);
		initializeSensor(Sensor.TYPE_HEART_RATE);

		x = new ArrayList();
		y = new ArrayList();
		z = new ArrayList();
		input_signal = new ArrayList();
		activityInference = new ActivityInference(getApplicationContext());
	}

	private void initializeSensor(int sensorType) {
		Sensor mSensor = mSensorManager.getDefaultSensor(sensorType);
		if( mSensor != null )
			mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
	}

	private void initializeExerciseList() {
		String[] exerciseNames = getResources().getStringArray(R.array.planets_array_names);

		for (String exercise : exerciseNames) {
			int planetResourceId =
				getResources().getIdentifier(exercise, "array", getPackageName());
			String[] planetInformation = getResources().getStringArray(planetResourceId);

			mExerciseList.add(new Exercise(
				planetInformation[0],   // Name
				planetInformation[1]));    // Navigation icon
		}
		mSelectedExercise = 0;
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		//TODO Add functionality
		switch (menuItem.getItemId()) {
			case R.id.reset:
				break;
			case R.id.end:
				break;
		}
		return true;
	}
	
	private void updateExerciseInfo() {
		Exercise exercise = mExerciseList.get(mSelectedExercise);
	}

	@Override
	public void onItemSelected(int position) {
		mSelectedExercise = position;
		updateExerciseInfo();
		//TODO UPDATE INFORMATION - load exercise and display
	}

	@Override
	public void onClick(View view) {
		mValueShowing = mValueShowing.next();
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				this.onMotionChanged(sensorEvent.values);
				break;
			case Sensor.TYPE_HEART_RATE:
				this.onHeartRateChanged((int) sensorEvent.values[0]);
				break;
		}
	}
	
	private final int N_SAMPLES = 90;
	private static List<Float> x;
	private static List<Float> y;
	private static List<Float> z;
	private static List<Float> input_signal;

	public void onMotionChanged (float[] values) {
		activityPrediction();
		x.add(values[0]);
		y.add(values[1]);
		z.add(values[2]);
	}
	
	private void activityPrediction() {
		if(x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
			// Copy all x,y and z values to one array of shape N_SAMPLES*3
			input_signal.addAll(x); input_signal.addAll(y); input_signal.addAll(z);

			// Perform inference using Tensorflow
			float[] results = activityInference.getActivityProb(toFloatArray(input_signal));

			//downstairsTextView.setText(Float.toString(round(results[0],2)));
			//joggingTextView.setText(Float.toString(round(results[1],2)));
			//sittingTextView.setText(Float.toString(round(results[2],2)));
			//standingTextView.setText(Float.toString(round(results[3],2)));
			//upstairsTextView.setText(Float.toString(round(results[4],2)));
			//walkingTextView.setText(Float.toString(round(results[5],2)));

			// Clear all the values
			x.clear(); y.clear(); z.clear(); input_signal.clear();
		}
	}

	private float[] toFloatArray(List<Float> list) {
		int i = 0;
		float[] array = new float[list.size()];

		for (Float f : list) {
			array[i++] = (f != null ? f : Float.NaN);
		}
		return array;
	}
	
	public void onHeartRateChanged (int tHearRate) {
		if (tHearRate > 30) {
			mExerciseList.get(mSelectedExercise).updateHR(tHearRate);
			if (mValueShowing == ValueType.HR) {
				mValue.setText(tHearRate > 30 ? String.valueOf(tHearRate) : "...");
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {   }

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

	private final class NavigationAdapter
		extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

		private final Context mContext;

		public NavigationAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mExerciseList.size();
		}

		@Override
		public String getItemText(int pos) {
			return mExerciseList.get(pos).getName();
		}

		@Override
		public Drawable getItemDrawable(int pos) {
			String navigationIcon = mExerciseList.get(pos).getImage();

			int drawableNavigationIconId =
				getResources().getIdentifier(navigationIcon, "drawable", getPackageName());

			return mContext.getDrawable(drawableNavigationIconId);
		}
	}

}
