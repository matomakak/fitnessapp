/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.extend;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.GridViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.maps.model.LatLng;
import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.Stats;
import com.hlavackamartin.fitnessapp.Utilities;
import com.hlavackamartin.fitnessapp.adapter.FragmentGridAdapter;
import com.hlavackamartin.fitnessapp.fragment.ActionFragment;
import com.hlavackamartin.fitnessapp.fragment.SimpleMapFragment;
import com.hlavackamartin.fitnessapp.fragment.SingleValueFragment;
import com.hlavackamartin.fitnessapp.service.LocationService;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class providing base methods and functionality for tracking dynamic type of movement.
 * Extending WearableActivityExt class, which defines methods for action tasks.
 * @see com.hlavackamartin.fitnessapp.extend.WearableActivityExt
 */
public class DynamicMoveActivity extends WearableActivityExt {

    protected SimpleMapFragment mMapPage;
    protected SingleValueFragment mDistancePage;
    private SingleValueFragment mTimePage;
    private ActionFragment mPausePage;

    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;

    private boolean mLocationUpdateRegistered;
    private boolean locationFirstStart = true;
    private LatLng mLastKnownLocation = null;
    private boolean mAwaitGPS = true;

    private Timer mTimer;

    protected Stats.DynamicMove mStats = new Stats.DynamicMove();
    /**
     * Method using GridView to create simple UI containing multiple pages with Google map and
     * location, single values and action buttons allowing to pause and stop tracking.
     */
    private void setupViews(){
        GridViewPager mPager = (GridViewPager) findViewById(R.id.pager);

        FragmentGridAdapter mAdapter = new FragmentGridAdapter(this, getFragmentManager());
        mMapPage = new SimpleMapFragment();
        mDistancePage = SingleValueFragment.newInstance("Distance");
        mTimePage = SingleValueFragment.newInstance("Time");
        mPausePage = ActionFragment.newInstance(ActionFragment.type.PAUSE_ACTION);
        mAdapter.addFragmentRow(
                mMapPage,
                mDistancePage,
                mTimePage,
                mPausePage,
                ActionFragment.newInstance(ActionFragment.type.END_ACTION)
        );

        mPager.setOffscreenPageCount(5);
        mPager.setAdapter(mAdapter);
        findViewById(R.id.page_indicator).setVisibility(View.GONE);
    }
    /**
     * Method creating basic UI with dismiss overlay availability and registering for receiving
     * intents from services
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.grid_pager_layout);
        setupViews();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setAmbientEnabled();

        mDismissOverlayView= (DismissOverlayView)findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent event) {
                mDismissOverlayView.show();
            }
        });
        //LOCATION SERVICE STARTING
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(LocationService.LOCATION_INTENT);
        mIntentFilter.addAction(LocationService.GPS_WAIT_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
        locationFirstStart = true;

        mStats.mDistanceTotal = 0.0f;
    }
    /**
     * Broadcast receiver which registers to receive messages from location service.
     * @see com.hlavackamartin.fitnessapp.service.LocationService
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equalsIgnoreCase(LocationService.GPS_WAIT_INTENT) && mLocationUpdateRegistered){
            	mAwaitGPS = true;
                mMapPage.setAwaitGPS(mAwaitGPS);
                locationFirstStart = true;
                return;
            }
            double latitude = intent.getDoubleExtra("latitude", 200.0);
            double longitude = intent.getDoubleExtra("longitude", 200.0);
            if( latitude > 180 || longitude > 180 )
                return;

            //indication if zoom need to be reset
            boolean resetZoom = false;
            //first occurrence of successful location
            if(locationFirstStart){
                locationFirstStart = false;
                startActionTask();
                resetZoom = true;
            }
            //current location from intent message
            LatLng mCurrentLocation = new LatLng(
                    latitude,
                    longitude
            );

            mMapPage.moveCameraTo(mCurrentLocation, intent.getFloatExtra("bearing", 0),resetZoom);

            float mCalculatedDistance = calculateDistance(mCurrentLocation);
            //if there was movement, set total distance and draw polyline from last position
            if( mCalculatedDistance > 0 ){
                mStats.mDistanceTotal += mCalculatedDistance;
                if(mStats.mDistanceTotal > 1000)
                    mDistancePage.setValue(String.format("%.2f km", mStats.mDistanceTotal /1000));
                else
                    mDistancePage.setValue(String.format("%.0f m", mStats.mDistanceTotal));

                mMapPage.drawPolyline(
                        mLastKnownLocation,
                        mCurrentLocation
                );
                mLastKnownLocation = mCurrentLocation;
            }
            setUnderMapInfo(intent);
        }
    };
    /**
     * Method definition for extending classes, which sets text under map fragment.
     * @param intent received message from service
     */
    protected void setUnderMapInfo(Intent intent){
    }
    /**
     * @param mCurrentLocation current position
     * @return distance between last known position and current position
     */
    private float calculateDistance(LatLng mCurrentLocation){
        if(mLastKnownLocation == null){
            mLastKnownLocation = mCurrentLocation;
            return 0;
        }

        float[] results = new float[1];
        try{
            Location.distanceBetween(
                    mLastKnownLocation.latitude,
                    mLastKnownLocation.longitude,
                    mCurrentLocation.latitude,
                    mCurrentLocation.longitude,
                    results);
            return results[0];
        }catch (IllegalArgumentException e){
            return 0;
        }
    }
    /**
     * Method definition for extending classes.
     * @return intent with desired string text message
     */
    protected Intent getResultsMessage(){
        return null;
    }
    /**
     * When activity is exited, stops running services and displays results.
     @param shouldCallDestroy indicates whether is activity ending normally
     */
    private void endTask(boolean shouldCallDestroy){
        if( mTimer != null ) mTimer.cancel();
        mMapPage.stopLocationService();
        if(shouldCallDestroy){
            startActivity(getResultsMessage());
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    DynamicMoveActivity.this.finish();
                }
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        endTask(false);
        super.onDestroy();
    }
    /**
     * Action method which is triggered when location service successfully acquired position.
     * Method starts timer for counting elapsed time and send request for device vibration.
     * @see com.hlavackamartin.fitnessapp.Utilities
     */
    @Override
    public void startActionTask() {
    	mAwaitGPS = false;
        mMapPage.setAwaitGPS(mAwaitGPS);
        Utilities.vibrate(this, 200);
        mLocationUpdateRegistered = true;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(mLocationUpdateRegistered) {
                    ++mStats.mTimeElapsed;
                    final String mFormat = String.format("%02d:%02d:%02d",
                                    mStats.mTimeElapsed / 3600,
                                    mStats.mTimeElapsed / 60,
                                    mStats.mTimeElapsed % 60);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                mTimePage.setValue(mFormat);
                        }
                    });
                }
            }
        },1000,1000);
    }
    /**
     * Action method which is triggered when pause button is pressed.
     * Pauses or resumes location service and changes button appearance accordingly.
     */
    @Override
    public void pauseActionTask() {
    	if(mAwaitGPS)
    		return;
        if(mLocationUpdateRegistered){
            mMapPage.stopLocationService();
            mPausePage.setButtonBackground(ActionFragment.type.START_ACTION);
            mMapPage.setStartPause(true);
        }else{
            mMapPage.startLocationService();
            mPausePage.setButtonBackground(ActionFragment.type.PAUSE_ACTION);
            mMapPage.setStartPause(false);
        }
        mLocationUpdateRegistered = !mLocationUpdateRegistered;
    }
    /**
     * Action method which is triggered when stop button is pressed.
     * Stops location service, finishes activity and displays results.
     */
    @Override
    public void endActionTask() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                DynamicMoveActivity.this.endTask(true);
            }
        }, 1000);
    }
    /**
     * When ambient mode is entered and map fragment is visible, set map style to ambient.
     */
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        if(mMapPage.getUserVisibleHint())
            mMapPage.onEnterAmbient(ambientDetails);
    }
    /**
     * When ambient mode is exited, method sets map style back to default.
     */
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mMapPage.onExitAmbient();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }
}
