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
 * to user obtained from location service or statistics over tracking.
 * @see com.hlavackamartin.fitnessapp.extend.DynamicMoveActivity
 */
public class RunningActivity extends DynamicMoveActivity {

    private long mOnGoingTime = 0;
    private float mOnGoingDistance = 0;
    private float mAveragePace = 0;
    private int mNewPaceOccurrence = 0;
    /**
     * Method parsing data from statistics obtained over time of tracking.
     * @return intent with string message containing parsed statistics
     */
    @SuppressLint("DefaultLocale")
    @Override
    protected Intent getResultsMessage() {
        Intent mIntent = new Intent(this, ResultActivity.class);

        double averagePace = mStats.mDistanceTotal > 100 ? ((mStats.mTimeElapsed / mStats.mDistanceTotal) * 1000) : 0;
        ArrayList<String> results = new ArrayList<>();
        mIntent.putExtra(Common.ExerciseResult.TYPE,Common.EXERCISE_RUN);
        results.add("Dist: " + String.format("%.2f km", mStats.mDistanceTotal / 1000));
        results.add("Time: " + String.format("%02d:%02d:%02d",
                mStats.mTimeElapsed / 3600,
                mStats.mTimeElapsed / 60,
                mStats.mTimeElapsed % 60));
        results.add("Pace: " + String.format("%02.0f:%02.0f", averagePace / 60, averagePace % 60));
        mIntent.putExtra(Common.ExerciseResult.DATA,results);
        return mIntent;
    }
    /**
     * Method applying location service data as output to user. Showing average running pace.
     * @param intent message from location service with recorded values
     */
    @SuppressLint("DefaultLocale")
    @Override
    protected void setUnderMapInfo(Intent intent) {
        float mDiff = mStats.mDistanceTotal - mOnGoingDistance;
        if( mDiff > 99){
            mAveragePace = ((mAveragePace * mNewPaceOccurrence) + (((mStats.mTimeElapsed - mOnGoingTime)/mDiff)*1000)) / ++mNewPaceOccurrence;
            mOnGoingDistance = mStats.mDistanceTotal;
            mOnGoingTime = mStats.mTimeElapsed;
            mMapPage.setBottomInfo(String.format(
                    "%02.0f:%02.0f",
                    mAveragePace / 60,
                    mAveragePace % 60));
        }
    }
}
