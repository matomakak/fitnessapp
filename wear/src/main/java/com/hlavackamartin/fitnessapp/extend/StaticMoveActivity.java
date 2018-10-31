/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.extend;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.Stats;
import com.hlavackamartin.fitnessapp.activity.ResultActivity;
import com.hlavackamartin.fitnessapp.adapter.FragmentGridAdapter;
import com.hlavackamartin.fitnessapp.fragment.ActionFragment;
import com.hlavackamartin.fitnessapp.fragment.SingleValueFragment;
import com.hlavackamartin.fitnessapp.service.HeartRateService;

import java.util.ArrayList;

/**
 * Class providing base methods and functionality for tracking static type of movement.
 * Extending WearableActivityExt class, which defines methods for action tasks.
 * @see com.hlavackamartin.fitnessapp.extend.WearableActivityExt
 */
public class StaticMoveActivity extends WearableActivityExt {

    private GridViewPager mPager;
    private boolean serviceListenerRegistered;

    private SingleValueFragment mValuePageHR;
    private SingleValueFragment mValuePageReps;
    private ActionFragment mPausePage;

    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;


    private Stats.StaticMove mStats = new Stats.StaticMove();
    /**
     * Method definition for extending classes.
     * @return intent name for intent filter, according used service
     */
    protected String getRepsIntent(){
        return "";
    }
    /**
     * Method definition for extending classes.
     * @return service class for repetition counting.
     */
    protected Class getRepsService(){
        return null;
    }
    /**
     * Method using GridView to create simple UI containing multiple pages with single values for
     * heart rate,, repetition count value and action buttons allowing to pause, reset and stop
     * tracking of exercise.
     */
    private void setupViews(){
        mPager = (GridViewPager) findViewById(R.id.pager);

        FragmentGridAdapter mAdapter = new FragmentGridAdapter(this, getFragmentManager());
        mValuePageHR = SingleValueFragment.newInstance("HeartRate");
        mPausePage = ActionFragment.newInstance(ActionFragment.type.PAUSE_ACTION);
        mAdapter.addFragmentRow(
                mValuePageHR,
                mPausePage,
                ActionFragment.newInstance(ActionFragment.type.END_ACTION)
        );
        mValuePageReps = SingleValueFragment.newInstance("Reps");
        mAdapter.addFragmentRow(
                mValuePageReps,
                ActionFragment.newInstance(ActionFragment.type.RESET_ACTION),
                ActionFragment.newInstance(ActionFragment.type.END_ACTION)
        );

        mPager.setOffscreenPageCount(3);
        mPager.setAdapter(mAdapter);
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(mPager);
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

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(HeartRateService.HEAR_RATE_INTENT);
        mIntentFilter.addAction(getRepsIntent());
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver, mIntentFilter);
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)){
            startService(new Intent(this, HeartRateService.class));
        }else{
            Toast.makeText(this, "Heart rate Sensor is not present.", Toast.LENGTH_SHORT).show();
        }
        startService(new Intent(this, getRepsService()));
        serviceListenerRegistered = true;

        mDismissOverlayView= (DismissOverlayView)findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent event) {
                mDismissOverlayView.show();
            }
        });
    }
    /**
     * Broadcast receiver which registers to receive messages from heart rate monitoring and
     * repetition counting service.
     * After receiving message from services, updates value in UI and saves it to statistics.
     * @see com.hlavackamartin.fitnessapp.service.HeartRateService
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equalsIgnoreCase(HeartRateService.HEAR_RATE_INTENT)){
                int tHearRate = intent.getIntExtra(HeartRateService.HEAR_RATE_INTENT_KEY, 0);
                if( tHearRate > 30 ){
                    mValuePageHR.setValue(tHearRate);
                    mStats.averageHR = ((mStats.averageHR * mStats.HRCount) + tHearRate) / ++mStats.HRCount;
                    if( tHearRate > mStats.maxHR )
                        mStats.maxHR = tHearRate;
                    if( tHearRate < mStats.minHR)
                        mStats.minHR = tHearRate;
                }else
                    mValuePageHR.setValue("...");
            }
            if(action.equalsIgnoreCase(getRepsIntent()))
                mValuePageReps.setValue(++mStats.moveTotal);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        this.finish();
    }
    @Override
    protected void onDestroy() {
        endTask(false);
        super.onDestroy();
    }
    /**
     * Method definition for extending classes.
     * @return String as name of exercise
     */
    protected String getExerciseName(){ return ""; }
    /**
     * When activity is exited, stops running services and displays results.
     * @param shouldCallDestroy indicates whether is activity ending normally
     */
    private void endTask(boolean shouldCallDestroy){
        stopService(new Intent(this, HeartRateService.class));
        stopService(new Intent(this, getRepsService()));
        serviceListenerRegistered = false;
        if(shouldCallDestroy){
            Intent intent = new Intent(this, ResultActivity.class);
            ArrayList<String> results = new ArrayList<>();
            intent.putExtra("RESULT", getExerciseName());
            results.add("Avg HR: " + (int) mStats.averageHR);
            results.add("Max HR: " + mStats.maxHR);
            results.add("Min HR: " + (mStats.minHR < 300 ? mStats.minHR : 0));
            results.add("Reps: " + mStats.moveTotal);
            intent.putExtra("DATA", results);
            startActivity(intent);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    StaticMoveActivity.this.finish();
                }
            }, 1000);
        }
    }
    /**
     * Action method which is triggered when pause button is pressed.
     * Pauses or resumes heart rate monitoring service and changes button appearance accordingly.
     */
    @Override
    public void pauseActionTask() {
        if(serviceListenerRegistered){
            stopService(new Intent(this, HeartRateService.class));
            stopService(new Intent(this, getRepsService()));
            mValuePageHR.setValue("...");
            mPausePage.setButtonBackground(ActionFragment.type.START_ACTION);
        }else{
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE))
                startService(new Intent(this, HeartRateService.class));
            startService(new Intent(this, getRepsService()));
            mPausePage.setButtonBackground(ActionFragment.type.PAUSE_ACTION);
        }
        serviceListenerRegistered = !serviceListenerRegistered;
    }
    /**
     * Action method which is triggered when reset button is pressed.
     * Resets repetition counter to 0.
     */
    @Override
    public void resetActionTask() {
        mStats.moveTotal = 0;
        mValuePageReps.setValue(mStats.moveTotal);
    }
    /**
     * Action method which is triggered when stop button is pressed.
     * Stops running services and display animation for finishing.
     */
    @Override
    public void endActionTask() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                StaticMoveActivity.this.endTask(true);
            }
        }, 1000);
    }

    /**
     * @see StaticMoveActivity#updateDisplay()
     */
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    /**
     * @see StaticMoveActivity#updateDisplay()
     */
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }
    /**
     * @see StaticMoveActivity#updateDisplay()
     */
    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }
    /**
     * Method changes appearance of visible fragment according to whether the ambient mode is
     * on or off.
     */
    private void updateDisplay() {
        if(mValuePageHR.getUserVisibleHint())
            mValuePageHR.updateAmbient(isAmbient());
        else if(mValuePageReps.getUserVisibleHint())
            mValuePageReps.updateAmbient(isAmbient());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }
}
