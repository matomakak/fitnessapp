/*
 * Copyright (c) 2016 Martin Hlavačka
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


public class MotionRecordingService extends Service implements SensorEventListener {
	
	private final IBinder mBinder = new LocalBinder();

	private SensorManager mSensorManager;

	private RecordingStatus recordingStatus;
	private String recordingExercise = null;
	
	private FileOutputStream writer;
	private File recordFile = null;

	@Override
	public IBinder onBind(Intent intent) {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		Utilities.initializeSensor(this, mSensorManager, Sensor.TYPE_ACCELEROMETER);
		Utilities.initializeSensor(this, mSensorManager, Sensor.TYPE_HEART_RATE);
		recordingStatus = RecordingStatus.STOPPED;
		return mBinder;
	}

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

	public RecordingStatus getRecordingStatus() {
		return this.recordingStatus;
	}

	public boolean startRepRecording(String name) {
		if (recordingStatus == RecordingStatus.STOPPED) {
			loadWriter();
			recordingExercise = name;
			recordingStatus = RecordingStatus.RECORDING;
			return true;
		}
		return false;
	}

	public void setRepsForFinishedRecording(int reps) {
		if (recordingStatus == RecordingStatus.WAITING_FOR_REPS && writer != null) {
			try {
				if (reps != 0) {
					writer.write(String.format(Locale.ENGLISH, "#reps,%d,0,0,0\n\n", reps).getBytes());
				}
				writer.flush();
				recordingStatus = RecordingStatus.STOPPED;
			} catch (IOException ignored) {
			}
		}
	}

	public void stopRepRecording(boolean noRepsProvided) {
		if (recordingStatus == RecordingStatus.RECORDING) {
			recordingStatus = RecordingStatus.WAITING_FOR_REPS;
		}
		if (noRepsProvided) {
			setRepsForFinishedRecording(0);
		}
	}

	public void deleteData() {
		if (recordingStatus == RecordingStatus.STOPPED) {
			try {
				if (getFile().exists())
					getFile().delete();
			} catch (FileNotFoundException ignored) {
			}
		}
	}

	private File getFile() throws FileNotFoundException {
		if (recordFile == null) {
			recordFile = Utilities.getFile(this, getString(R.string.upload_file));
		}
		return recordFile;
	}

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
    
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
		if (recordingStatus == RecordingStatus.RECORDING && writer != null) {
			String data = String.format(Locale.ENGLISH,"%s,%d,%f,%f,%f\n",
				recordingExercise,
				System.currentTimeMillis(),
				sensorEvent.values[0],
				sensorEvent.values[1],
				sensorEvent.values[2]
			);
			try {
				writer.write(data.getBytes());
			} catch (IOException ignored) {
				//TODO
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
    public void onAccuracyChanged(Sensor sensor, int i) {    }
	
	public class LocalBinder extends Binder {
		public MotionRecordingService getService() {
			return MotionRecordingService.this;
		}
	}
	
	public enum RecordingStatus {
		RECORDING,
		WAITING_FOR_REPS,
		STOPPED
	}
}
