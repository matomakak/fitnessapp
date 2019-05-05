package com.hlavackamartin.fitnessapp.recognition.data;

/**
 * Enumeration used for selecting which type of data user sees in detection module. Provided types
 * are: reps and average, max and current heart rate.
 */
public enum InfoValueType {
  REPS("Reps"),
  HR("HR"),
  AVG_HR("Avg HR"),
  MAX_HR("Max HR");

  private static InfoValueType[] vals = values();
  private final String name;

  InfoValueType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public InfoValueType next() {
    return vals[(this.ordinal() + 1) % vals.length];
  }
}
