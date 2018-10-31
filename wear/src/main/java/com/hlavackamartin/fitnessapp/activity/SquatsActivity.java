/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.os.Bundle;

import com.hlavackamartin.fitnessapp.Common;
import com.hlavackamartin.fitnessapp.extend.StaticMoveActivity;
import com.hlavackamartin.fitnessapp.service.SquatService;

/**
 * Class extending StaticMoveActivity with specification of which service providing data
 * is being used and what intent filter should be applied for Squat exercise.
 * @see com.hlavackamartin.fitnessapp.extend.StaticMoveActivity
 */
public class SquatsActivity extends StaticMoveActivity {
    /**
     * @return name of intent filter for registering broadcast receiver
     * @see com.hlavackamartin.fitnessapp.extend.StaticMoveActivity#onCreate(Bundle)
     */
    @Override
    protected String getRepsIntent() {
        return SquatService.SQUAT_INTENT;
    }
    /**
     * @return name of exercise
     * @see StaticMoveActivity#endTask(boolean)
     */
    @Override
    protected String getExerciseName() {
        return Common.EXERCISE_SQUAT;
    }
    /**
     * @return service class providing needed data
     * @see com.hlavackamartin.fitnessapp.extend.StaticMoveActivity#onCreate(Bundle)
     */
    @Override
    protected Class getRepsService() {
        return SquatService.class;
    }
}
