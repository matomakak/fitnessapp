package com.hlavackamartin.fitnessapp.recognition.data;

public class Exercise {

  private String name;
  private Integer reps = 0;
  private Float confidence = 0f;

  public Exercise() {
  }

  public Exercise(String name) {
    this.name = name;
  }

  public void clearStats() {
    this.reps = 0;
  }

  public void addRep() {
    this.reps++;
  }

  public void setConfidence(Float confidence) {
    this.confidence = confidence;
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
}
