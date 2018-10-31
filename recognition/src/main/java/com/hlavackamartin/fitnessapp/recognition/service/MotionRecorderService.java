/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.recognition.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;


public class MotionRecorderService extends Service implements SensorEventListener{

	private static final String FILE_NAME = "FITness_recordings.csv";
	
	private final IBinder mBinder = new LocalBinder();

	private String recordingExercise = null;
	
	private FileOutputStream writer;
	private File recordFile = null;
	
	private String recordings = "";

	public void forgetLast(String name) {
		if (recordingExercise != null && recordingExercise.equals(name)) {
			recordings = "";
			writer = null;
			recordingExercise = null;
		}
	}

	public void forgetAll() {
		//TODO remove whole file
	}

	public boolean executeRepRecording(String name) {
		if (recordingExercise != null && writer != null) {
			recordings += "\n";
			try {
				writer.write(recordings.getBytes());
				writer.flush();
				writer.close();
			} catch (IOException ignored) {
			}
			recordings = "";
			writer = null;
			recordingExercise = null;
			return false;
		}
		else {
			try {
				recordings = "";
				writer = new FileOutputStream(getFile(), true);
				recordingExercise = name;
			} catch (FileNotFoundException e) {
				return false;
			}
			return true;
		}
	}
	
	public File getFile() throws FileNotFoundException {
		if (recordFile == null) {
			File path = this.getExternalFilesDir(null);
			if (null == path) {
				path = this.getFilesDir();
			}
			if (!path.exists()){
				if (!path.mkdirs()){
					throw new FileNotFoundException();
				}
			}
			recordFile = new File(path.getPath() + File.separator + FILE_NAME);
		}
		return recordFile;
	}
    
    @Override
    public void onDestroy() {
        if (writer != null) {
			try {
				writer.close();
			} catch (IOException ignored) {
			}
		}
        super.onDestroy();
    }
    
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
		if (this.recordingExercise != null && this.writer != null) {
			String data = String.format(Locale.ENGLISH,"%s,%d,%f,%f,%f\n",
				recordingExercise,
				System.currentTimeMillis(),
				sensorEvent.values[0],
				sensorEvent.values[1],
				sensorEvent.values[2]
			);
			recordings += data;
			Log.d("ACCELEROMETER_DATA",data);
		}
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
		public MotionRecorderService getService() {
			return MotionRecorderService.this;
		}
	}
}
