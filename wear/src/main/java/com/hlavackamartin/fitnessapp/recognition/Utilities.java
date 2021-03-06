/*
 * Copyright (c) 2019. Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.recognition;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilities class providing system-wide functions as vibration and preference saving/restoring
 */
public class Utilities {

  private static final int DEFAULT_VIBRATION_DURATION_MS = 200; // in millis
  private static final int DEFAULT_SENSOR_DURATION_US = 20000; // in micros
  private static final String PREF_KEY = "preference";

  /**
   * Method causes device to vibrate for the given duration (in millis). If duration is set to 0,
   * then it will use the <code>DEFAULT_VIBRATION_DURATION_MS</code>.
   *
   * @param duration duration of vibration in ms
   */
  public static void vibrate(Context context, Integer duration) {
    if (duration == null) {
      duration = DEFAULT_VIBRATION_DURATION_MS;
    }
    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    if (v != null) {
      v.vibrate(duration);
    }
  }

  /**
   * Saves the integer value in the preference storage. If <code>value</code> is negative, then the
   * value will be removed from the preferences.
   *
   * @param value value to be saved
   */
  public static void savePreference(Context context, String value) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    pref.edit().putString(PREF_KEY, value).apply();
  }

  /**
   * Retrieves the value of counter from preference manager. If no value exists, it will return 0.
   *
   * @return saved preference value
   */
  public static String getPreference(Context context) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    return pref.getString(PREF_KEY, null);
  }

  /**
   * Checks network availability
   *
   * @return current network availability
   */
  public static boolean isNetworkConnectionAvailable(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  /**
   * Parses data structure used in collection functionality within application to neural network
   * readable data
   */
  public static float[][][][] toFloatArray(List<List<Float>> list) {
    int axis = 0;
    int i, j;
    float[][][][] array = new float[1][1][list.get(0).size()][list.size()];
    float ir = 1.0f / 20;

    for (List<Float> l : list) {
      for (i = 0; i < l.size(); i++) {
        for (j = 0; j < 20; j++) {
          if (i - j < 0) {
            continue;
          }
          array[0][0][i][axis] += l.get(i - j) * ir;
        }
      }
      axis++;
    }
    return array;
  }

  /**
   * Provides functionality to initialize sensor reading for given context
   *
   * @param listener given context listener
   * @param sensorType type of sensor to subscribe to
   * @param handler thread handler to avoid overly use UI thread
   * @return success of subscription
   */
  public static boolean initializeSensor(SensorEventListener listener, SensorManager sensorManager,
      int sensorType, Handler handler) {
    Sensor mSensor = sensorManager.getDefaultSensor(sensorType);
    return mSensor != null && (handler != null ?
        sensorManager.registerListener(listener, mSensor, DEFAULT_SENSOR_DURATION_US, handler)
        : sensorManager.registerListener(listener, mSensor, DEFAULT_SENSOR_DURATION_US));
  }

  /**
   * Retrieves file according to name from external application dir
   *
   * @param fileName name of file
   * @return found file
   * @throws FileNotFoundException when no file was found
   */
  public static File getFile(Context context, String fileName) throws FileNotFoundException {
    File path = context.getExternalFilesDir(null);
    if (null == path) {
      path = context.getFilesDir();
    }
    if (!path.exists()) {
      if (!path.mkdirs()) {
        throw new FileNotFoundException();
      }
    }
    return new File(path.getPath() + File.separator + fileName);
  }

  /**
   * Checks if file exists according to name from external application dir
   *
   * @param fileName name of file
   * @return found file
   */
  public static boolean fileExist(Context context, String fileName) {
    File file = null;
    try {
      file = getFile(context, fileName);
    } catch (Exception ignored) {
    }
    return file != null && file.exists();
  }

  /**
   * Checks write permissions for external application dir
   *
   * @return possibility to write in external application dir
   */
  public static boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

  /**
   * Reads support file for neural network containing sorted labels of exercises.
   *
   * @return list of exercise names
   */
  public static List<String> readRecognitionLabels(Context context) {
    return readAttributesFile(context, true);
  }

  /**
   * Reads support file for neural network containg size of datasets used for training cnn.
   *
   * @return required size for dataset fed to neural network
   */
  public static int readSampleSize(Context context) {
    List<String> data = readAttributesFile(context, false);
    for (String config : data) {
      if (config.startsWith("#SAMPLES#") && config.length() > 9) {
        try {
          return Integer.parseInt(config.substring(9));
        } catch (NumberFormatException nfe) {
          return -1;
        }
      }
    }
    return -1;
  }

  /**
   * Common functionality for {@link #readRecognitionLabels(Context)} (Context, boolean)} and {@link
   * #readSampleSize(Context)}
   *
   * @param readLabels reading labels or sample size
   */
  private static List<String> readAttributesFile(Context context, boolean readLabels) {
    FileInputStream fstream;
    try {
      fstream = new FileInputStream(getFile(context, context.getString(R.string.download_labels)));
    } catch (FileNotFoundException e) {
      return Collections.emptyList();
    }
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    List<String> records = new ArrayList<>();

    String strLine;
    try {
      while ((strLine = br.readLine()) != null) {
        if ((readLabels && !strLine.startsWith("#")) || (!readLabels && strLine.startsWith("#"))) {
          records.add(strLine);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return records;
  }
}
