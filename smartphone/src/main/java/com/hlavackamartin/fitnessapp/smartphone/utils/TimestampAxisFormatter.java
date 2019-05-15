/*
 * Copyright (c) 2019. Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.smartphone.utils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

public class TimestampAxisFormatter implements IAxisValueFormatter {

  public TimestampAxisFormatter() {
  }

  public static String formatValue(float value) {
    return String.format("%.3f", value / 1000f); // convert microseconds to seconds
  }

  @Override
  public String getFormattedValue(float value, AxisBase axis) {
    // "value" represents the position of the label on the axis (x or y)
    return formatValue(value);
  }
}