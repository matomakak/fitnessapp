package com.hlavackamartin.fitnessapp.recognition.data;

public class HeartRateData {

	private Integer currentHR = 0;
	private Float avgHR = 0f;
	private Integer maxHR = 0;
	private Long hrCount = 0L;
	
	public void updateHR(int currentHR) {
		if (currentHR > 30) {
			this.currentHR = currentHR;
			avgHR = ((avgHR * hrCount) + this.currentHR) / ++hrCount;
			if (this.currentHR > maxHR)
				maxHR = this.currentHR;
		}
	}

	public Integer getCurrentHR() {
		return currentHR;
	}

	public Float getAvgHR() {
		return avgHR;
	}

	public Integer getMaxHR() {
		return maxHR;
	}
}
