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
 * Service class providing detection of exercise movement called Jumping Jack.
 */
public class JumpJackService extends Service implements SensorEventListener {

    public static final String JUMP_JACK_INTENT = "JUMP_JACK";

    private SensorManager mSensorManager;
    /** an up-down movement that takes more than this will not be registered as such **/
    private static final long TIME_THRESHOLD_NS = 2000000000; // in nanoseconds (= 2sec)
    /**
     * Earth gravity is around 9.8 m/s^2 but user may not completely direct his/her hand vertical
     * during the exercise so we leave some room. Basically if the x-component of gravity, as
     * measured by the Gravity sensor, changes with a variation (delta) > GRAVITY_THRESHOLD,
     * we consider that a successful count.
     */
    private static final float GRAVITY_THRESHOLD = 7.0f;
    private long mLastTime = 0;
    private boolean mUp = false;
    /**
     * Method registers listener for gravity sensor from sensor manager.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
    /**
     * Method which gets called when value of gravity was changed.
     * @see JumpJackService#detectMove(float, long)
     * @param sensorEvent data object containing information about acquired value
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        detectMove(sensorEvent.values[0], sensorEvent.timestamp);
    }
    /**
     * Method implementing algorithm for detection Jumping Jack movement.
     * When successful repetition was identified sends information through local broadcast to parent
     * activity.
     * @param xValue number value of gravity in x-axis
     * @param timestamp occurrence in time
     */
    private void detectMove(float xValue, long timestamp) {
        if ((Math.abs(xValue) > GRAVITY_THRESHOLD)) {
            if(timestamp - mLastTime < TIME_THRESHOLD_NS && mUp != (xValue > 0)) {
                if (mUp)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(JUMP_JACK_INTENT));
            }
            mUp = xValue > 0;
            mLastTime = timestamp;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {    }
}
