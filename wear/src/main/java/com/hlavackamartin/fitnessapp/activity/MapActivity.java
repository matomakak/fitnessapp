/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.gms.maps.model.LatLng;
import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.extend.WearableActivityExt;
import com.hlavackamartin.fitnessapp.fragment.SimpleMapFragment;

/**
 * Class providing interactive map interface with current position.
 * @see SimpleMapFragment
 */
public class MapActivity extends WearableActivityExt {

    private SimpleMapFragment mMapFragment;

    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;

    private static final String LOCATION_INTENT = "LOCATION";
    /**
     * Method creating basic layout for showing map with ability to exit it trough long press
     * dismiss overlay.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_activity_layout);

        mMapFragment = new SimpleMapFragment();
        getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment, "Map interface").commit();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setAmbientEnabled();

        mDismissOverlayView= (DismissOverlayView)findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent event) {
                mDismissOverlayView.show();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,new IntentFilter(LOCATION_INTENT));
    }
    /**
     * Broadcast receiver which registers to receive messages from location service.
     * @see com.hlavackamartin.fitnessapp.service.LocationService
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //first occurrence of successful location
            LocalBroadcastManager.getInstance(MapActivity.this).unregisterReceiver(mBroadcastReceiver);
            mMapFragment.hideBottomInfo();
            mMapFragment.setAwaitGPS(false);
            //current location from intent message
            LatLng mCurrentLocation = new LatLng(
                    intent.getDoubleExtra("latitude", 0.0),
                    intent.getDoubleExtra("longitude", 0.0)
            );
            mMapFragment.moveCameraTo(mCurrentLocation, intent.getFloatExtra("bearing", 0),true);
        }
    };
    /**
     * When finishing unregisters local broadcast receiver.
     */
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }
}
