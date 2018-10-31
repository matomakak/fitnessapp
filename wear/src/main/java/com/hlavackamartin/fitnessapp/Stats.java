/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp;

/**
 * Class providing structuralized access to statistics data of activity tracking.
 */
public class Stats {

    public static class DynamicMove{
        public float mDistanceTotal = 0;
        public long mTimeElapsed = 0;
    }

    public static class StaticMove{
        public int moveTotal = 0;
        public double averageHR = 0;
        public long HRCount = 0;
        public double maxHR = 0;
        public double minHR = 999;
    }
}
