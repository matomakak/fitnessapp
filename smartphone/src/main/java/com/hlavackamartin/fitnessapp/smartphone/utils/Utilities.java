package com.hlavackamartin.fitnessapp.smartphone.utils;

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
import com.hlavackamartin.fitnessapp.smartphone.R;
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

  public static final int DEFAULT_SENSOR_DURATION_US = 20000; // in micros
  private static final int DEFAULT_VIBRATION_DURATION_MS = 200; // in millis
  private static final String PREF_KEY = "preference";

  /**
   * Method causes device to vibrate for the given duration (in millis). If duration is set to 0,
   * then it will use the <code>DEFAULT_VIBRATION_DURATION_MS</code>.
   *
   * @param context activity context
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
   * Saves the integer value in the preference storage. If <code>value</code>
   * is negative, then the value will be removed from the preferences.
   *
   * @param context activity context
   * @param value value to be saved
   */
  public static void savePreference(Context context, String value) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    pref.edit().putString(PREF_KEY, value).apply();
  }

  /**
   * Retrieves the value of counter from preference manager. If no value exists, it will return 0.
   *
   * @param context activity context
   * @return saved preference value
   */
  public static String getPreference(Context context) {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    return pref.getString(PREF_KEY, null);
  }

  public static boolean isNetworkConnectionAvailable(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  public static float[][][][] toFloatArray(List<List<Float>> list, float scaling) {
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
          array[0][0][i][axis] += (l.get(i - j) / scaling) * ir;
        }
      }
      axis++;
    }
    return array;
  }

  public static void initializeSensor(SensorEventListener listener, SensorManager sensorManager,
      int sensorType, Handler handler) {
    Sensor mSensor = sensorManager.getDefaultSensor(sensorType);
    if (mSensor != null) {
      sensorManager.registerListener(listener, mSensor, DEFAULT_SENSOR_DURATION_US, handler);
    }
  }


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


  public static boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

  public static List<String> readRecognitionLabels(Context context) {
    return readAttributesFile(context, true);
  }

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

  public static List<String> readAttributesFile(Context context, boolean readLabels) {
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

  public static String formatNsDuration(long nanos) {
    long micros = nanos / 1000;
    long millis = nanos / 1000 / 1000;
    long seconds = nanos / 1000 / 1000 / 1000;
    if (seconds > 0) {
      return seconds + " s";
    }
    if (millis > 0) {
      return millis + " ms";
    }
    if (micros > 0) {
      return micros + " µs";
    }
    if (nanos > 0) {
      return nanos + " ns";
    }
    return "-";
  }
}
