/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.hlavackamartin.fitnessapp.Common;
import com.hlavackamartin.fitnessapp.extend.DynamicMoveActivity;

import java.util.ArrayList;

/**
 * Class extending DynamicMoveActivity with specification of which values should be shown
 * to user obtained from location service or statistics over tracking
 * @see DynamicMoveActivity
 */
public class CyclingActivity extends DynamicMoveActivity {
    /**
     * Method parsing data from statistics obtained over time of tracking.
     * @return intent with string message containing parsed statistics
     */
    @SuppressLint("DefaultLocale")
    @Override
    protected Intent getResultsMessage() {
        Intent mIntent = new Intent(this, ResultActivity.class);
        ArrayList<String> results = new ArrayList<>();
        mIntent.putExtra(Common.ExerciseResult.TYPE,Common.EXERCISE_BIKE);
        results.add("Dist: " + String.format("%.2f km", mStats.mDistanceTotal / 1000));
        results.add("Time: " + String.format("%02d:%02d:%02d",
                mStats.mTimeElapsed / 3600,
                mStats.mTimeElapsed / 60,
                mStats.mTimeElapsed % 60));
        results.add("Speed: " + String.format("%.1f km/h", (mStats.mDistanceTotal / mStats.mTimeElapsed) * 3.6));
        mIntent.putExtra(Common.ExerciseResult.DATA,results);
        return mIntent;
    }
    /**
     * Method applying location service data as output to user. Showing current speed.
     * @param intent message from location service with recorded values
     */
    @SuppressLint("DefaultLocale")
    @Override
    protected void setUnderMapInfo(Intent intent) {
        mMapPage.setBottomInfo(String.format("%.1f km/h",intent.getFloatExtra("speed",0)*3.6f));
    }
}
