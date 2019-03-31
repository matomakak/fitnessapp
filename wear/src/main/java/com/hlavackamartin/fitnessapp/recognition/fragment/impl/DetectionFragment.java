package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class DetectionFragment extends FitnessAppFragment
    implements View.OnClickListener {

  private static final String RECOGNITION_STARTED_BUNDLE_KEY = "RECOGNITION_STARTED_BUNDLE_KEY";
  public static final double CONFIDENCE_THRESHOLD = 0.99;
  private boolean recognitionInProgress;

  private SensorManager mSensorManager;
  private ActivityInference activityInference = null;
  private HandlerThread handlerThread = new HandlerThread("worker");
  private Handler workHandler;

  private static int SLIDING_WINDOW_STEP_SIZE;
  private static int UPPER_WINDOW_SIZE;
  private static int N_SAMPLES = -1;
  private static int dataPos = 0;

  private static List<Float> x;
  private static List<Float> y;
  private static List<Float> z;
  private static List<List<Float>> input_signal;

  private String mSelectedExercise;
  private ValueType mValueShowing = ValueType.REPS;
  private TextView mTitle;
  private TextView mValue;
  private Map<String, Exercise> exerciseStats = new HashMap<>();
  private HeartRateData heartRateData = new HeartRateData();

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putBoolean(RECOGNITION_STARTED_BUNDLE_KEY, recognitionInProgress);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    handlerThread.start();
    workHandler = new Handler(handlerThread.getLooper());
    this.recognitionInProgress =
        state != null && state.getBoolean(RECOGNITION_STARTED_BUNDLE_KEY, false);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
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
    UPPER_WINDOW_SIZE = N_SAMPLES - 1;
    SLIDING_WINDOW_STEP_SIZE = N_SAMPLES / 4;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (recognitionInProgress) {
      startTask();
    }
  }

  private void startTask() {
    dataPos = 0;
    x = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    y = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    z = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    recognitionInProgress = Utilities
        .initializeSensor(mSensorEventListener, mSensorManager, Sensor.TYPE_LINEAR_ACCELERATION,
            workHandler);
  }

  private final SensorEventListener mSensorEventListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
      switch (sensorEvent.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
          this.onMotionChanged(sensorEvent.values);
          break;
        case Sensor.TYPE_HEART_RATE:
          onHeartRateChanged((int) sensorEvent.values[0]);
          break;
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void onMotionChanged(float[] values) {
      if (activityInference != null) {
        x.set(dataPos, values[0]);
        y.set(dataPos, values[1]);
        z.set(dataPos, values[2]);
        dataPos++;
        if (dataPos >= UPPER_WINDOW_SIZE) {
          dataPos = 0;
        }
        if (dataPos != 0 && dataPos % SLIDING_WINDOW_STEP_SIZE != 0) return;
        // Copy all x,y and z values to one array of shape N_SAMPLES*3input_signal
        input_signal.add(x.subList(x.size() - N_SAMPLES, x.size()));
        input_signal.add(y.subList(y.size() - N_SAMPLES, y.size()));
        input_signal.add(z.subList(z.size() - N_SAMPLES, z.size()));
        // Perform inference using Tensorflow
        List<Recognition> recognitions = activityInference
            .getActivityProb(Utilities.toFloatArray(input_signal));
        input_signal.clear();

        for (Recognition r : recognitions) {
          if (r.getConfidence() > CONFIDENCE_THRESHOLD) {
            mSelectedExercise = r.getTitle();
            exerciseStats.computeIfAbsent(r.getTitle(), Exercise::new).addRep();
            getActivity().runOnUiThread(() -> updateUI());
            return;
          }
        }
      }
    }

    private void onHeartRateChanged(int tHearRate) {
      heartRateData.updateHR(tHearRate);
      getActivity().runOnUiThread(() -> updateUI());
    }
  };

  @Override
  public void onPause() {
    endTask();
    super.onPause();
  }

  @Override
  public void onStop() {
    handlerThread.quitSafely();
    super.onStop();
  }

  private void endTask() {
    mSensorManager.unregisterListener(mSensorEventListener);
  }

  @Override
  public void onClick(View view) {
    mValueShowing = mValueShowing.next();
    updateUI();
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

  private void updateUI() {
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
