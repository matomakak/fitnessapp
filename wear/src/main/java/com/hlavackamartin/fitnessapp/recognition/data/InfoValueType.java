package com.hlavackamartin.fitnessapp.recognition.data;


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
