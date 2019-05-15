/*
 * Copyright (c) 2019. Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.smartphone.data;

public class Exercise {

  private final String name;
  private Integer reps = 0;
  private Float confidence = 0f;

  public Exercise(String name) {
    this.name = name;
  }

  public void clearStats() {
    this.reps = 0;
  }

  public void addRep() {
    this.reps++;
  }

  public String getName() {
    return name;
  }

  public Integer getReps() {
    return reps;
  }

  public Float getConfidence() {
    return confidence;
  }

  public void setConfidence(Float confidence) {
    this.confidence = confidence;
  }
}
