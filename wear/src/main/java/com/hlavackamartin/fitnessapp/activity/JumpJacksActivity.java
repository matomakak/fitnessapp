/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.os.Bundle;

import com.hlavackamartin.fitnessapp.Common;
import com.hlavackamartin.fitnessapp.extend.StaticMoveActivity;
import com.hlavackamartin.fitnessapp.service.JumpJackService;

/**
 * Class extending StaticMoveActivity with specification of which service providing data
 * is being used and what intent filter should be applied for Jumping Jack exercise.
 * @see com.hlavackamartin.fitnessapp.extend.StaticMoveActivity
 */
public class JumpJacksActivity extends StaticMoveActivity {
    /**
     * @return name of intent filter for registering broadcast receiver
     * @see StaticMoveActivity#onCreate(Bundle)
     */
    @Override
    protected String getRepsIntent() {
        return JumpJackService.JUMP_JACK_INTENT;
    }
    /**
     * @return name of exercise
     * @see StaticMoveActivity#endTask(boolean)
     */
    @Override
    protected String getExerciseName() {
        return Common.EXERCISE_JUMPJACK;
    }
    /**
     * @return service class providing needed data
     * @see StaticMoveActivity#onCreate(Bundle)
     */
    @Override
    protected Class getRepsService() {
        return JumpJackService.class;
    }
}
