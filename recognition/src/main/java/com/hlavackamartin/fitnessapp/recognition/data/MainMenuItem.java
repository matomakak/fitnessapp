package com.hlavackamartin.fitnessapp.recognition.data;

/**
 * Created by Martin on 18.10.2018.
 */

public class MainMenuItem {
	private MenuType type;
	private String name;
	private String image; 

	public MainMenuItem(MenuType type, String name, String image) {
		this.type = type;
		this.name = name;
		this.image = image;
	}
	
	public MenuType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getImage() {
		return image;
	}

	public enum MenuType {
		RECOGNITION("recognition"),
		LEARNING("learning"),
		SYNC("learning");
		
		private final String name;

		MenuType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
		/*public static MenuType getByName(String name) {
			for (MenuType type : values()) {
				if (type.name.equals(name)) {
					return type;
				}
			}
			return null;
		}*/
	}
}
