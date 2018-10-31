package com.hlavackamartin.fitnessapp.recognition.fragment;

public class Exercise {

	private String name;
	private String image;
	private Integer reps = 0;
	private Float avgHR = 0f;
	private Integer maxHR = 0;
	private Long hrCount = 0L;

	public Exercise(String name, String image) {
		this.name = name;
		this.image = image;
	}
	
	public void updateHR(int currentHR) {
		avgHR = ((avgHR * hrCount) + currentHR) / ++hrCount;
		if( currentHR > maxHR )
			maxHR = currentHR;
	}

	public String getName() {
		return name;
	}

	public String getImage() {
		return image;
	}

	public Integer getReps() {
		return reps;
	}

	public void setReps(Integer reps) {
		this.reps = reps;
	}

	public Float getAvgHR() {
		return avgHR;
	}

	public Integer getMaxHR() {
		return maxHR;
	}

	public void setMaxHR(Integer maxHR) {
		this.maxHR = maxHR;
	}
}
