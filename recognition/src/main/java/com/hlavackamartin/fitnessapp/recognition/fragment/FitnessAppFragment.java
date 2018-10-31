package com.hlavackamartin.fitnessapp.recognition.fragment;

import android.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;


public abstract class SensorEnabledFragment extends Fragment implements SensorEventListener {
	
	public abstract void executeReset();
	public abstract void executeResetAll();
	public abstract void onEnterAmbientInFragment(Bundle ambientDetails);
	public abstract void onExitAmbientInFragment();
	
	@Override
	public abstract void onSensorChanged(SensorEvent sensorEvent);

	@Override
	public abstract void onAccuracyChanged(Sensor sensor, int i);
}
