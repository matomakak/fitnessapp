package com.hlavackamartin.fitnessapp.recognition.data;

/**
 * Provides functionality for storing information about available modules in app. Current state
 * allows recognition, learning and synchronization module
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
    RECOGNITION("recognition", 0),
    LEARNING("learning", 1),
    SYNC("sync", 2);

    private final String name;
    private final int pos;

    MenuType(String name, int pos) {
      this.name = name;
      this.pos = pos;
    }

    public String getName() {
      return this.name;
    }

    public int getPos() {
      return pos;
    }
  }
}
