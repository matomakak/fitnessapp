/*
 * Copyright (c) 2019. Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.recognition.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Class providing functionality for learning module. Contains logic for sensor subscription and
 * parsing and writing recorded data within service type of application component
 */
public class MotionRecordingService extends Service implements SensorEventListener {

  private final IBinder mBinder = new LocalBinder();

  private SensorManager mSensorManager;

  private RecordingStatus recordingStatus;
  private String recordingExercise = null;

  private FileOutputStream writer;
  private File recordFile = null;

  /**
   * When binded to activity, subscribe to sensor
   */
  @Override
  public IBinder onBind(Intent intent) {
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Utilities.initializeSensor(this, mSensorManager, Sensor.TYPE_LINEAR_ACCELERATION, null);
    recordingStatus = RecordingStatus.STOPPED;

    return mBinder;
  }

  /**
   * When unbinded from activity unsubscribes from sensor
   */
  @Override
  public boolean onUnbind(Intent intent) {
    mSensorManager.unregisterListener(this);
    getWriter().ifPresent(w -> {
      try {
        w.flush();
      } catch (IOException ignored) {
      }
    });
    return super.onUnbind(intent);
  }

  /**
   * @return Current state of recording
   */
  public RecordingStatus getRecordingStatus() {
    return this.recordingStatus;
  }

  /**
   * Start recording process
   *
   * @param name name of exercise recorded
   * @return success of staring
   */
  public boolean startRepRecording(String name) {
    if (recordingStatus == RecordingStatus.STOPPED) {
      loadWriter();
      recordingExercise = name;
      recordingStatus = RecordingStatus.RECORDING;
      return true;
    }
    return false;
  }

  /**
   * Provides functionality for deprecated implementation of number of reps selection after
   * finishing recording
   *
   * @param reps number of executed reps within last recording
   */
  public void setRepsForFinishedRecording(Integer reps) {
    if (recordingStatus == RecordingStatus.WAITING_FOR_REPS && writer != null) {
      try {
        if (reps != null) {
          writer.write(String.format(Locale.ENGLISH, "#reps,%d,0,0,0\n\n", reps).getBytes());
        }
        writer.flush();
        recordingStatus = RecordingStatus.STOPPED;
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Stops recording process
   *
   * @param shouldWaitForRepsProviding indicated that number of reps will be provided shortly
   */
  public void stopRepRecording(boolean shouldWaitForRepsProviding) {
    if (recordingStatus == RecordingStatus.RECORDING) {
      recordingStatus = RecordingStatus.WAITING_FOR_REPS;
      if (!shouldWaitForRepsProviding) {
        setRepsForFinishedRecording(null);
      }
    }
  }

  /**
   * Deletes all previously recorded data
   *
   * @return success of operation
   */
  public boolean deleteData() {
    if (recordingStatus == RecordingStatus.STOPPED) {
      try {
        if (!getFile().exists()) {
          getFile().createNewFile();
        }
        return getFile().delete();
      } catch (IOException ignored) {
      }
    }
    return false;
  }

  /**
   * Retrieves cached file
   *
   * @return recordings file
   * @throws FileNotFoundException when file not found
   */
  private File getFile() throws FileNotFoundException {
    if (recordFile == null) {
      recordFile = Utilities.getFile(this, getString(R.string.upload_file));
    }
    return recordFile;
  }

  /**
   * Initializes writer for recordings file
   */
  private void loadWriter() {
    if (writer == null) {
      try {
        writer = new FileOutputStream(getFile(), true);
      } catch (FileNotFoundException e) {
        writer = null;
      }
    }
  }

  private Optional<FileOutputStream> getWriter() {
    return Optional.ofNullable(writer);
  }

  /**
   * Implementation of data obtaining and writing to file from sensor in specified format
   *
   * @param sensorEvent data from sensor
   */
  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (recordingStatus == RecordingStatus.RECORDING && writer != null) {
      String data = String.format(Locale.ENGLISH, "%s,%d,%f,%f,%f\n",
          recordingExercise,
          System.currentTimeMillis(),
          sensorEvent.values[0],
          sensorEvent.values[1],
          sensorEvent.values[2]
      );
      try {
        writer.write(data.getBytes());
      } catch (IOException ignored) {
      }
    }
  }

  @Override
  public void onDestroy() {
    getWriter().ifPresent(w -> {
      try {
        w.close();
      } catch (IOException ignored) {
      }
    });
    super.onDestroy();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
  }

  /**
   * Status indication of state of recording process
   */
  public enum RecordingStatus {
    RECORDING,
    WAITING_FOR_REPS,
    STOPPED
  }

  /**
   * Provides connection between activity and service via bounded connection
   */
  public class LocalBinder extends Binder {

    public MotionRecordingService getService() {
      return MotionRecordingService.this;
    }
  }
}
