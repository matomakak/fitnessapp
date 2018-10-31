package com.hlavackamartin.fitnessapp.movement;

/**
 * Created by Martin on 25.8.2018.
 */

public class JumpingJacksActivity implements ActivityProvider {

	private static JumpingJacksActivity instance;

	private JumpingJacksActivity() {}

	static {
		instance = new JumpingJacksActivity();
	}

	public static ActivityProvider getInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "RUNNING";
	}

	@Override
	public String getNavigationIcon() {
		return "ic_n_white_48dp";
	}

	@Override
	public String getImage() {
		return "neptune";
	}

	@Override
	public String getMoons() {
		return "4 activity";
	}

	@Override
	public String getVolume() {
		return "4 kilometer";
	}

	@Override
	public String getSurfaceArea() {
		return "4 hour";
	}

	@Override
	public void startActivity() {
		
	}
}
