package com.hlavackamartin.fitnessapp.recognition.data;

public class Exercise {

	private String name;
	private Integer reps = 0;
	private Float avgHR = 0f;
	private Integer maxHR = 0;
	private Long hrCount = 0L;

	public Exercise(String name) {
		this.name = name;
	}
	
	public void clearStats() {
		this.reps = 0;
		this.avgHR = 0f;
		this.maxHR = 0;
		this.hrCount = 0L;
	}

	public void addRep() {
		this.reps++;
	}
	
	public void updateHR(int currentHR) {
		avgHR = ((avgHR * hrCount) + currentHR) / ++hrCount;
		if( currentHR > maxHR )
			maxHR = currentHR;
	}

	public String getName() {
		return name;
	}

	public Integer getReps() {
		return reps;
	}

	public Float getAvgHR() {
		return avgHR;
	}

	public Integer getMaxHR() {
		return maxHR;
	}
}
