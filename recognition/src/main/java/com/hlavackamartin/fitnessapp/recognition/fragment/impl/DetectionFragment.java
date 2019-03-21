package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.data.Exercise;
import com.hlavackamartin.fitnessapp.recognition.data.HeartRateData;
import com.hlavackamartin.fitnessapp.recognition.data.Recognition;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.provider.ActivityInference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class DetectionFragment extends FitnessAppFragment
    implements View.OnClickListener, SensorEventListener {

  private static final String RECOGNITION_STARTED_BUNDLE_KEY = "RECOGNITION_STARTED_BUNDLE_KEY";
  public static final double CONFIDENCE_THRESHOLD = 0.92;
  private boolean recognitionInProgress;

  private static int SLIDING_WINDOW_JUMP;
  private static int UPPER_WINDOW_SIZE;
  private static List<Float> x;
  private static List<Float> y;
  private static List<Float> z;
  private static List<List<Float>> input_signal;
  private SensorManager mSensorManager;
  private String mSelectedExercise;
  private ValueType mValueShowing = ValueType.REPS;
  private TextView mTitle;
  private TextView mValue;
  private ActivityInference activityInference = null;
  private Map<String, Exercise> exerciseStats = new HashMap<>();
  private HeartRateData heartRateData = new HeartRateData();

  private int N_SAMPLES = -1;

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putBoolean(RECOGNITION_STARTED_BUNDLE_KEY, recognitionInProgress);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    this.recognitionInProgress =
        state != null && state.getBoolean(RECOGNITION_STARTED_BUNDLE_KEY, false);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_detect, container, false);

    mTitle = rootView.findViewById(R.id.detect_title);
    mValue = rootView.findViewById(R.id.detect_value);
    mValue.setOnClickListener(this);

    x = new ArrayList<>();
    y = new ArrayList<>();
    z = new ArrayList<>();
    input_signal = new ArrayList<>();
    activityInference = ActivityInference.getInstance(getContext());

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    mSelectedExercise = "";
    N_SAMPLES = Utilities.readSampleSize(getContext());
    SLIDING_WINDOW_JUMP = N_SAMPLES / 6;
    UPPER_WINDOW_SIZE = N_SAMPLES - 1;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (recognitionInProgress) {
      startTask();
    }
  }

  private void startTask() {
    recognitionInProgress = true;
    Utilities.initializeSensor(this, mSensorManager, Sensor.TYPE_ACCELEROMETER);
    //Utilities.initializeSensor(this, mSensorManager, Sensor.TYPE_HEART_RATE);
  }

  @Override
  public void onPause() {
    endTask();
    super.onPause();
  }

  private void endTask() {
    mSensorManager.unregisterListener(this);
  }

  @Override
  public void onClick(View view) {
    mValueShowing = mValueShowing.next();
    updateValue();
  }

  @Override
  public List<String> getActionMenu(Resources resources) {
    return new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.controls)));
  }

  @Override
  public boolean onMenuItemClick(MenuItem menuItem) {
    if (menuItem.getTitle().equals(getString(R.string.restart_current))) {
      Optional.of(exerciseStats.get(mSelectedExercise)).ifPresent(Exercise::clearStats);
    } else if (menuItem.getTitle().equals(getString(R.string.restart_all))) {
      exerciseStats.clear();
    } else if (menuItem.getTitle().toString().contains((getString(R.string.start)))) {
      startTask();
    } else if (menuItem.getTitle().toString().contains((getString(R.string.stop)))) {
      endTask();
    }

    return true;
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
    updateValue();
  }

  private void onMotionChanged(float[] values) {
    x.add(values[0]);
    y.add(values[1]);
    z.add(values[2]);
    activityPrediction();
  }

  private void activityPrediction() {
    if (x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES
        && activityInference != null) {
      // Copy all x,y and z values to one array of shape N_SAMPLES*3
      input_signal.add(x);
      input_signal.add(y);
      input_signal.add(z);
      // Perform inference using Tensorflow
      List<Recognition> recognitions = activityInference
          .getActivityProb(Utilities.toFloatArray(input_signal));

      for (Recognition r : recognitions) {
        if (r.getConfidence() > CONFIDENCE_THRESHOLD) {
          x.clear(); y.clear(); z.clear(); input_signal.clear();
          mSelectedExercise = r.getTitle();
          getExerciseStat(r.getTitle()).addRep();
          updateValue();
          return;
        }
      }
      // Clear half of the values
      x = x.subList(SLIDING_WINDOW_JUMP, UPPER_WINDOW_SIZE);
      y = y.subList(SLIDING_WINDOW_JUMP, UPPER_WINDOW_SIZE);
      z = z.subList(SLIDING_WINDOW_JUMP, UPPER_WINDOW_SIZE);
      input_signal.clear();
    }
  }

  private void updateValue() {
    String title = mValueShowing.getName();
    String value = "...";
    switch (mValueShowing) {
      case HR:
        if (heartRateData.getCurrentHR() > 30) {
          value = heartRateData.getCurrentHR().toString();
        }
        break;
      case AVG_HR:
        value = heartRateData.getAvgHR().toString();
        break;
      case MAX_HR:
        value = heartRateData.getMaxHR().toString();
        break;
      case REPS:
        Exercise exercise = exerciseStats.get(mSelectedExercise);
        if (exercise != null) {
          title = exercise.getName();
          value = exercise.getReps().toString();
        }
        break;
    }
    mTitle.setText(title);
    mValue.setText(value);
  }

  private void onHeartRateChanged(int tHearRate) {
    heartRateData.updateHR(tHearRate);
  }

  private Exercise getExerciseStat(String name) {
    return exerciseStats.computeIfAbsent(name, Exercise::new);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
  }

  @Override
  public void onEnterAmbient(Bundle bundle) {
    mTitle.getPaint().setAntiAlias(false);
    mValue.getPaint().setAntiAlias(false);
  }

  @Override
  public void onExitAmbient() {
    mTitle.getPaint().setAntiAlias(true);
    mValue.getPaint().setAntiAlias(true);
  }

  @Override
  public void onUpdateAmbient() {
  }

  public enum ValueType {
    REPS("Reps"),
    HR("HR"),
    AVG_HR("Avg HR"),
    MAX_HR("Max HR");

    private static ValueType[] vals = values();
    private final String name;

    ValueType(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public ValueType next() {
      return vals[(this.ordinal() + 1) % vals.length];
    }
  }
}
