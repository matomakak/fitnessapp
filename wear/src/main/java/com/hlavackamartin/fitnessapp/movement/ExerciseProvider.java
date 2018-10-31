package com.hlavackamartin.fitnessapp.movement;

/**
 * Created by Martin on 25.8.2018.
 */

public interface ActivityProvider {

	String getName();
	String getNavigationIcon();
	String getImage();
	String getMoons();
	String getVolume();
	String getSurfaceArea();
	
	void startActivity();
	
}
