package com.hlavackamartin.fitnessapp.movement;

/**
 * Created by Martin on 25.8.2018.
 */

public class SquatActivity implements ActivityProvider {
	
	private static SquatActivity instance;

	static {
		instance = new SquatActivity();
	}

	public static ActivityProvider getInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "SQUATS";
	}

	@Override
	public String getNavigationIcon() {
		return "ic_s_white_48dp";
	}

	@Override
	public String getImage() {
		return "saturn";
	}

	@Override
	public String getMoons() {
		return "2 activity";
	}

	@Override
	public String getVolume() {
		return "2 kilometer";
	}

	@Override
	public String getSurfaceArea() {
		return "2 hour";
	}

	@Override
	public void startActivity() {
		
	}
}
