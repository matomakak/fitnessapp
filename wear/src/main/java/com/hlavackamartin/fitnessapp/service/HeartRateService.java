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
 * Service class providing acquisition of users heart rate through heartrate sensor.
 */
public class HeartRateService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    public static final String HEAR_RATE_INTENT = "HEART_RATE";
    public static final String HEAR_RATE_INTENT_KEY = "value";
    /**
     * Method registers listener for heart rate sensor from sensor manager.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if( mSensor != null )
            mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
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
     * Method which gets called when new heart rate value was measured. Sending this value through
     * local broadcast to parent activity.
     * @param sensorEvent data object containing information about acquired value.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Intent mIntent = new Intent(HEAR_RATE_INTENT);
        mIntent.putExtra(HEAR_RATE_INTENT_KEY,(int) sensorEvent.values[0]);
        LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {    }
}
