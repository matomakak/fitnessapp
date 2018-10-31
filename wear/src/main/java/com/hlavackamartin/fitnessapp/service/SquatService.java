/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Service class providing detection of exercise squat movement.
 */
public class SquatService extends Service implements SensorEventListener {

    public static final String SQUAT_INTENT = "SQUAT";

    private SensorManager mSensorManager;

    private static final long TIME_THRESHOLD_MAX = 2500000000l; // in nanoseconds
    private static final float STANDING_GRAVITY = 8f;
    private static final float SQUATTING_THRESHOLD = 3f;
    private static final float RESTING_THRESHOLD = 1f;
    private long mLastTime = 0;
    private boolean mDown = false;
    /**
     * Method registers listener for accelerometer sensor from sensor manager.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if( mSensor != null )
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    /**
     * When service is closing, unregister created listener.
     */
    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    private final float alpha = 0.8f;
    private float[] gravity = {0f,0f,0f};
    private float[] acceleration = new float[3];
    /**
     * Method which gets called when value of accelerometer was changed. Calculating gravity and
     * acceleration with low-pass filtering.
     * @param event data object containing information about acquired value
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        if( -STANDING_GRAVITY > gravity[0] || STANDING_GRAVITY < gravity[1]){
            acceleration[0] = event.values[0] - gravity[0];
            acceleration[1] = event.values[1] - gravity[1];
            acceleration[2] = event.values[2] - gravity[2];
            if(Math.abs(acceleration[1]) < RESTING_THRESHOLD &&  Math.abs(acceleration[2]) < RESTING_THRESHOLD)//Hands down
                detectMove(acceleration[0], event.timestamp);
            else if(Math.abs(acceleration[0]) < RESTING_THRESHOLD &&  Math.abs(acceleration[2]) < RESTING_THRESHOLD)//Hands behind neck
                detectMove(-acceleration[1], event.timestamp);
        }
    }
    /**
     * Method implementing algorithm for detection squat movement.
     * When successful repetition was detected, sends information through local broadcast to parent
     * activity.
     * @param value number value of upward acceleration
     * @param timestamp occurrence in time
     */
    private void detectMove(float value, long timestamp) {
        if( Math.abs(value) > SQUATTING_THRESHOLD){
            if( value < 0 ){
                mDown = true;
                mLastTime = timestamp;
            }
            if( mDown && value > 0){
                long timeDiff = timestamp - mLastTime;
                if(timeDiff < TIME_THRESHOLD_MAX){
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(SQUAT_INTENT));
                }
                mDown = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
