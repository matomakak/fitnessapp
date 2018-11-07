/*
 * Copyright (c) 2016 Martin Hlavačka
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
    private static final String PREF_KEY = "preference";
    /**
     * Method causes device to vibrate for the given duration (in millis). If duration is set to 0, then it
     * will use the <code>DEFAULT_VIBRATION_DURATION_MS</code>.
     * @param context activity context
     * @param duration duration of vibration in ms
     */
    public static void vibrate(Context context, Integer duration) {
        if (duration == null) {
            duration = DEFAULT_VIBRATION_DURATION_MS;
        }
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration);
    }
    /**
     * Saves the integer value in the preference storage. If <code>value</code>
     * is negative, then the value will be removed from the preferences.
     * @param context activity context
     * @param value value to be saved
     */
    public static void savePreference(Context context, int value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (value < 0) {
            pref.edit().remove(PREF_KEY).apply();
        } else {
            pref.edit().putInt(PREF_KEY, value).apply();
        }
    }
    /**
     * Retrieves the value of counter from preference manager. If no value exists, it will return 0.
     * @param context activity context
     * @return saved preference value
     */
    public static int getPreference(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getInt(PREF_KEY, 0);
    }
    
    public static boolean isNetworkConnectionAvailable(Context context) {
		ConnectivityManager cm =
			(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	public static float[] toFloatArray(List<Float> list) {
		int i = 0;
		float[] array = new float[list.size()];

		for (Float f : list) {
			array[i++] = (f != null ? f : Float.NaN);
		}
		return array;
	}

	public static void initializeSensor(SensorEventListener listener, SensorManager sensorManager, int sensorType) {
		Sensor mSensor = sensorManager.getDefaultSensor(sensorType);
		if (mSensor != null)
			sensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
	}


	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	public static List<String> readRecognitionLabels(Context context) {
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				context.getString(R.string.download_labels)));
		} catch (FileNotFoundException e) {
			return Collections.emptyList();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		List<String> labels = new ArrayList<>();

		String strLine;
		try {
			while ((strLine = br.readLine()) != null)   {
				labels.add(strLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return labels;
	}
}
