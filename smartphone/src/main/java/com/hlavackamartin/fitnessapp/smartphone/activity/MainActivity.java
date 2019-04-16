package com.hlavackamartin.fitnessapp.smartphone.activity;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static com.hlavackamartin.fitnessapp.smartphone.utils.Utilities.DEFAULT_SENSOR_DURATION_US;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.hlavackamartin.fitnessapp.smartphone.ActivityInference;
import com.hlavackamartin.fitnessapp.smartphone.R;
import com.hlavackamartin.fitnessapp.smartphone.data.Recognition;
import com.hlavackamartin.fitnessapp.smartphone.utils.TimestampAxisFormatter;
import com.hlavackamartin.fitnessapp.smartphone.utils.Utilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private SensorManager mSensorManager;
  private ActivityInference activityInference = null;

  private HandlerThread handlerThread1 = new HandlerThread("worker1");
  private Handler workHandler1;
  private HandlerThread handlerThread2 = new HandlerThread("worker2");
  private Handler workHandler2;

  private int N_SAMPLES = -1;
  private int dataPos = 0;
  private long firstTimestamp = -1;
  private static List<List<Float>> input_signal = new ArrayList<>();
  private List<Float> recordingDataX;
  private List<Float> recordingDataY;
  private List<Float> recordingDataZ;

  private LineChart chart;
  private static final int X_INDEX = 0;
  private static final int Y_INDEX = 1;
  private static final int Z_INDEX = 2;
  private static List<String> LINE_DESCRIPTIONS;
  private static int[] LINE_COLORS = {
      0x70660000,
      0x70FF00FF,
      0x7000FF00,
      0x70FFCCCC,
      0x70FF6000,
      0x70FFFF33,
      0x7000CCCC,
      0x7000CCFF,
      0x703366FF,
      0x709933FF,
      0x70CCCC99};

  private ToggleButton toggleRec;
  private ToggleButton toggleAcc;
  private ToggleButton toggle70;

  private boolean recStarted = false;
  private boolean showAccelerometer = false;
  private boolean showAbove70Only = false;
  /**
   * called from worker thread
   */
  private final SensorEventListener sensorEventListener = new SensorEventListener() {
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
      if (firstTimestamp == -1) {
        firstTimestamp = event.timestamp;
      }
      final float floatTimestampMicros = (event.timestamp - firstTimestamp) / 1000000f;
      if (event.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION) {

        recordingDataX.set(dataPos, event.values[0]); // DATA_NORMALIZATION_COEF;
        recordingDataY.set(dataPos, event.values[1]); // DATA_NORMALIZATION_COEF;
        recordingDataZ.set(dataPos, event.values[2]); // DATA_NORMALIZATION_COEF;
        dataPos++;
        if (dataPos >= N_SAMPLES - 1) {
          dataPos = 0;
        }

        input_signal
            .add(recordingDataX.subList(recordingDataX.size() - N_SAMPLES, recordingDataX.size()));
        input_signal
            .add(recordingDataY.subList(recordingDataY.size() - N_SAMPLES, recordingDataY.size()));
        input_signal
            .add(recordingDataZ.subList(recordingDataZ.size() - N_SAMPLES, recordingDataZ.size()));
        // Perform inference using Tensorflow
        final List<Recognition> recognitions = activityInference
            .getActivityProb(Utilities.toFloatArray(input_signal));

        input_signal.clear();
        runOnUiThread(
            () -> updateUI1(floatTimestampMicros, recognitions));
      } else if (showAccelerometer) {
        runOnUiThread(
            () -> updateUI2(floatTimestampMicros, event.values[0], event.values[1],
                event.values[2]));
      }
    }
  };

  private static LineDataSet createLineDataSet(String description, int color) {
    LineDataSet set = new LineDataSet(null, description);
    set.setAxisDependency(YAxis.AxisDependency.RIGHT);
    set.setColor(color);
    set.setDrawCircles(false);
    set.setDrawCircleHole(false);
    set.setLineWidth(1f);
    set.setFillAlpha(65);
    set.setFillColor(ColorTemplate.getHoloBlue());
    set.setHighLightColor(Color.WHITE);
    set.setValueTextColor(Color.WHITE);
    set.setValueTextSize(9f);
    set.setDrawValues(false);
    set.setDrawHighlightIndicators(true);
    set.setDrawIcons(false);
    set.setDrawHorizontalHighlightIndicator(false);
    set.setDrawFilled(false);
    return set;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    handlerThread1.start();
    workHandler1 = new Handler(handlerThread1.getLooper());
    handlerThread2.start();
    workHandler2 = new Handler(handlerThread2.getLooper());

    N_SAMPLES = Utilities.readSampleSize(this);

    // Load the TensorFlow model
    activityInference = ActivityInference.getInstance(this);

    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    initViews();
  }

  @Override
  protected void onStop() {
    handlerThread1.quitSafely();
    handlerThread2.quitSafely();
    super.onStop();
  }

  private void initViews() {
    chart = findViewById(R.id.chart);
    toggleRec = findViewById(R.id.toggleRec);
    toggleAcc = findViewById(R.id.toggleAcc);
    toggle70 = findViewById(R.id.toggle70);

    toggleRec.setOnClickListener(view -> {
      if (recStarted) {
        stopRecInt();
      } else {
        startRec();
      }
    });
    toggleAcc.setOnClickListener(view -> {
      showAccelerometer = !showAccelerometer;
      toggleAcc.setChecked(showAccelerometer);
    });
    toggle70.setOnClickListener(view -> {
      showAbove70Only = !showAbove70Only;
      toggle70.setChecked(showAbove70Only);
    });

    //chart.setLogEnabled(true);
    chart.setTouchEnabled(true);
    chart.setData(new LineData());
    chart.getLineData().setValueTextColor(Color.WHITE);

    chart.getDescription().setEnabled(false);
    chart.getLegend().setEnabled(true);
    chart.getLegend().setTextColor(Color.WHITE);

    XAxis xAxis = chart.getXAxis();
    xAxis.setTextColor(Color.WHITE);
    xAxis.setDrawGridLines(true);
    xAxis.setAvoidFirstLastClipping(true);
    xAxis.setEnabled(true);

    xAxis.setValueFormatter(new TimestampAxisFormatter());

    YAxis leftAxis = chart.getAxisLeft();
    leftAxis.setEnabled(false);

    YAxis rightAxis = chart.getAxisRight();
    rightAxis.setTextColor(Color.WHITE);
    rightAxis.setAxisMaximum(10f);
    rightAxis.setAxisMinimum(-10f);
    rightAxis.setDrawGridLines(true);
  }

  private void startRec() {
    LINE_DESCRIPTIONS = new ArrayList<>(Arrays.asList("X", "Y", "Z"));
    LINE_DESCRIPTIONS.addAll(Utilities.readRecognitionLabels(this));
    chart.getLineData().clearValues();
    createDataSets();

    if (!startRecInt()) {
      Toast.makeText(MainActivity.this, "FAILED SENSOR", Toast.LENGTH_SHORT).show();
      toggleRec.setChecked(false);
    }
  }

  private boolean startRecInt() {
    if (!recStarted) {
      recStarted = !recStarted;
      firstTimestamp = -1;
      chart.highlightValue(null);

      recordingDataX = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
      recordingDataY = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
      recordingDataZ = new ArrayList<>(Collections.nCopies(N_SAMPLES, 0f));
      dataPos = 0;

      Utilities
          .initializeSensor(sensorEventListener, mSensorManager, TYPE_ACCELEROMETER, workHandler1);
      Utilities.initializeSensor(sensorEventListener, mSensorManager, TYPE_LINEAR_ACCELERATION,
          workHandler2);
    }
    return recStarted;
  }

  private void stopRecInt() {
    if (recStarted) {
      mSensorManager.unregisterListener(sensorEventListener);
      recStarted = false;
    }
  }

  private void updateUI1(float timestampUs, List<Recognition> recognitions) {
    float probabilityTimestamp =
        timestampUs - ((N_SAMPLES * DEFAULT_SENSOR_DURATION_US) / 1000 / 2);

    for (Recognition recognition : recognitions) {
      if (probabilityTimestamp > 0) {
        float conf = recognition.getConfidence();
        if (showAbove70Only && conf < 0.7) conf = 0f;
        addPoint(Integer.parseInt(recognition.getId()) + 3, probabilityTimestamp, conf * 10);
      }
    }

    chart.notifyDataSetChanged();
    chart.invalidate();
  }

  private void updateUI2(float timestampUs, float x, float y, float z) {
    addPoint(X_INDEX, timestampUs, 20 * x / 9);
    addPoint(Y_INDEX, timestampUs, 20 * y / 9);
    addPoint(Z_INDEX, timestampUs, 20 * z / 9);

    chart.notifyDataSetChanged();
    chart.invalidate();
  }

  private void createDataSets() {
    for (int i = 0; i < LINE_DESCRIPTIONS.size(); i++) {
      chart.getLineData().addDataSet(createLineDataSet(LINE_DESCRIPTIONS.get(i), LINE_COLORS[i]));
    }
  }

  private void addPoint(int dataSetIndex, float x, float y) {
    chart.getLineData().addEntry(new Entry(x, y), dataSetIndex);
    chart.getLineData().notifyDataChanged();
  }

}
