/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.extend;

import android.support.wearable.activity.WearableActivity;

/**
 * Class providing method definition for action activities.
 * Defines 4 base methods which are essential for action handling.
 */
public class WearableActivityExt extends WearableActivity{
    /**
     * When start button is pressed this method is called.
     */
    public void startActionTask(){
    }
    /**
     * When pause button is pressed this method is called.
     */
    public void pauseActionTask(){
    }
    /**
     * When reset button is pressed this method is called.
     */
    public void resetActionTask(){
    }
    /**
     * When stop button is pressed this method is called.
     */
    public void endActionTask(){
    }
    /**
     * When activity stopped, finishing it properly.
     */
    @Override
    protected void onStop() {
        activityFinish();
        super.onStop();
    }
    /**
     * Helper method, which allows to override onStop behaviour when needed.
     * @see #onStop()
     */
    protected void activityFinish(){
        this.finish();
    }
}
