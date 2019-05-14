package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import static com.hlavackamartin.fitnessapp.recognition.Utilities.fileExist;

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
import com.hlavackamartin.fitnessapp.recognition.data.InfoValueType;
import com.hlavackamartin.fitnessapp.recognition.data.Recognition;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.provider.ActivityInference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Implementation for neural network inference and detection module
 */
public class DetectionFragment extends FitnessAppFragment
    implements View.OnClickListener, View.OnLongClickListener {

  private static final String RECOGNITION_STARTED_BUNDLE_KEY = "RECOGNITION_STARTED_BUNDLE_KEY";
  /**
   * Threshold for accepting detection as valid rep, while 1.0 is max value
   */
  public static final double CONFIDENCE_THRESHOLD = 0.92;
  private boolean recognitionInProgress;

  private SensorManager mSensorManager;
  private ActivityInference activityInference = null;
  /**
   * thread used within heart rate parsing
   */
  private HandlerThread handlerThreadHR = new HandlerThread("workerHR");
  /**
   * thread used within accelerometer data parsing for neural network
   */
  private HandlerThread handlerThreadAcc = new HandlerThread("workerAcc");
  private Handler workerHR;
  private Handler workerAcc;

  private static int SLIDING_WINDOW_STEP_SIZE;
  private static int UPPER_WINDOW_SIZE;
  private static int N_SAMPLES = -1;
  private static int dataPos = 0;

  private static List<Float> x;
  private static List<Float> y;
  private static List<Float> z;
  private static List<List<Float>> input_signal;

  private String mSelectedExercise;
  private InfoValueType mValueShowing = InfoValueType.REPS;
  private TextView mTitle;
  private TextView mValue;
  private NavigableMap<String, Exercise> exerciseStats = new TreeMap<>();
  private HeartRateData heartRateData = new HeartRateData();

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putBoolean(RECOGNITION_STARTED_BUNDLE_KEY, recognitionInProgress);
    super.onSaveInstanceState(savedInstanceState);
  }

  /**
   * Initializes threads and retrieves last state of recognition(was running before app pause
   */
  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    handlerThreadAcc.start();
    workerAcc = new Handler(handlerThreadAcc.getLooper());
    handlerThreadHR.start();
    workerHR = new Handler(handlerThreadHR.getLooper());
    this.recognitionInProgress =
        state != null && state.getBoolean(RECOGNITION_STARTED_BUNDLE_KEY, false);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_detect, container, false);

    mTitle = rootView.findViewById(R.id.detect_title);
    mTitle.setText(recognitionInProgress ? "" : getString(R.string.stopped));
    mValue = rootView.findViewById(R.id.detect_value);
    mValue.setOnClickListener(this);
    mValue.setOnLongClickListener(this);

    x = new ArrayList<>();
    y = new ArrayList<>();
    z = new ArrayList<>();
    input_signal = new ArrayList<>();
    if (!fileExist(getContext(), getString(R.string.download_file)) || !fileExist(getContext(),
        getString(R.string.download_labels))) {
      activityInference = null;
      mTitle.setText(R.string.error__no_data);
    } else {
      activityInference = ActivityInference.getInstance(getContext());
    }
    return rootView;
  }

  /**
   * Resets all data and reads support information for neural network
   */
  @Override
  public void onStart() {
    super.onStart();
    mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    mSelectedExercise = "";
    N_SAMPLES = Utilities.readSampleSize(getContext());
    UPPER_WINDOW_SIZE = N_SAMPLES - 1;
    SLIDING_WINDOW_STEP_SIZE = N_SAMPLES / 4;
  }

  /**
   * When paused and resumed, restarts previous state
   */
  @Override
  public void onResume() {
    super.onResume();
    if (recognitionInProgress) {
      startTask();
    }
  }

  /**
   * Resets data and prepare for new recognition process. Subscribes to sensor
   */
  private void startTask() {
    if (activityInference == null) {
      return;
    }
    dataPos = 0;
    x = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    y = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    z = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
    recognitionInProgress = Utilities
        .initializeSensor(mSensorEventListener, mSensorManager, Sensor.TYPE_LINEAR_ACCELERATION,
            workerAcc);
    Utilities
        .initializeSensor(mSensorEventListener, mSensorManager, Sensor.TYPE_HEART_RATE, workerHR);
    if (recognitionInProgress) {
      mTitle.setText(R.string.detecting);
    }
  }

  /**
   * Listener used to differenciate type of sensor and providing implementation of parsing data
   */
  private final SensorEventListener mSensorEventListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
      switch (sensorEvent.sensor.getType()) {
        case Sensor.TYPE_LINEAR_ACCELERATION:
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

    /**
     * Motion data parsing and feeding to neural network. Jumping window is used so lower
     * computational power of wearable can handle. Execute inference process and parses result
     * with visual indication on UI.
     *
     * @param values data from sensor
     */
    private void onMotionChanged(float[] values) {
      if (activityInference != null) {
        x.set(dataPos, values[0]);
        y.set(dataPos, values[1]);
        z.set(dataPos, values[2]);
        dataPos++;
        if (dataPos >= UPPER_WINDOW_SIZE) {
          dataPos = 0;
        }
        if (dataPos != 0 && dataPos % SLIDING_WINDOW_STEP_SIZE != 0) {
          return;
        }
        // Copy all x,y and z values to one array of shape N_SAMPLES*3input_signal
        input_signal.add(x.subList(x.size() - N_SAMPLES, x.size()));
        input_signal.add(y.subList(y.size() - N_SAMPLES, y.size()));
        input_signal.add(z.subList(z.size() - N_SAMPLES, z.size()));
        // Perform inference using Tensorflow
        List<Recognition> recognitions = activityInference
            .getActivityProb(Utilities.toFloatArray(input_signal));
        input_signal.clear();

        for (Recognition r : recognitions) {
          exerciseStats.computeIfAbsent(r.getTitle(), Exercise::new)
              .setConfidence(r.getConfidence());
          if (r.getConfidence() > CONFIDENCE_THRESHOLD) {
            if (mSelectedExercise.equals("")) {
              mSelectedExercise = r.getTitle();
            }
            exerciseStats.computeIfAbsent(r.getTitle(), Exercise::new).addRep();
          }
        }
        getActivity().runOnUiThread(() -> updateUI());
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

  /**
   * When application stopped, destroying running threads
   */
  @Override
  public void onStop() {
    handlerThreadAcc.quitSafely();
    handlerThreadHR.quitSafely();
    super.onStop();
  }

  /**
   * Unregisters from sensor
   */
  private void endTask() {
    mSensorManager.unregisterListener(mSensorEventListener);
  }

  /**
   * Cycling through showed data on UI via click on value
   */
  @Override
  public void onClick(View view) {
    mValueShowing = mValueShowing.next();
    updateUI();
  }

  /**
   * Cycling through exercise types recorded via long click on value
   */
  @Override
  public boolean onLongClick(View v) {
    if (!exerciseStats.isEmpty() && mValueShowing == InfoValueType.REPS) {
      Entry<String, Exercise> ex = exerciseStats.higherEntry(mSelectedExercise);
      mSelectedExercise = ex == null ? exerciseStats.firstEntry().getKey() : ex.getKey();
      updateUI();
    }
    return true;
  }

  @Override
  public List<String> getActionMenu(Resources resources) {
    return new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.controls)));
  }

  /**
   * Provides functionality for bottom menu and behaviour of each entry
   *
   * @param menuItem selected bottom menu item
   * @return consumed click
   */
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

  /**
   * Updated UI with current values according to showed type of data selected via {@link
   * #onClick(View)}
   */
  private void updateUI() {
    if (activityInference == null) {
      mTitle.setText(R.string.error__no_data);
      return;
    }
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
          title = String.format("%s-%.0f %%", exercise.getName(), exercise.getConfidence() * 100);
          value = exercise.getReps().toString();
        } else {
          title = mTitle.getText().toString();
        }
        break;
    }
    mTitle.setText(recognitionInProgress ? title : "");
    mValue.setText(value);
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onEnterAmbient(Bundle bundle) {
    mTitle.getPaint().setAntiAlias(false);
    mValue.getPaint().setAntiAlias(false);
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onExitAmbient() {
    mTitle.getPaint().setAntiAlias(true);
    mValue.getPaint().setAntiAlias(true);
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onUpdateAmbient() {
  }
}
