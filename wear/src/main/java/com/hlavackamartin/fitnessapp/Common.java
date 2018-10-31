/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp;

import java.util.ArrayList;
/**
 * Shared library class providing basic identification for communication and recognition of message.
 * Also provides inner class for storing exercise results.
 */
public class Common {
    public static final String SHARED_DATA_PATH = "/com.hlavackamartin.fitnessapp/";

    public static final String EXERCISE_RUN = "RUN";
    public static final String EXERCISE_BIKE = "BIKE";
    public static final String EXERCISE_JUMPJACK = "JUMP JACKS";
    public static final String EXERCISE_SQUAT= "SQUATS";

    /**
     * Class containing basic data structure for exercise result manipulation.
     */
    public class ExerciseResult{
        public static final String TIME = "time";
        public static final String TYPE = "type";
        public static final String DATA = "data";

        private Long mTime;
        private String mType;
        private ArrayList<String> mText;

        public ExerciseResult(){}

        public ExerciseResult(Long time, String type, ArrayList<String> text){
            mTime = time;
            mType = type;
            mText = text;
        }

        public void setTime(Long time){ mTime = time; }
        public void setType(String type){ mType = type; }
        public void setText(ArrayList<String> text){ mText = text; }

        public Long getTime(){ return mTime; }
        public String getType(){ return mType; }
        public ArrayList<String> getText(){ return mText; }
    }
}
