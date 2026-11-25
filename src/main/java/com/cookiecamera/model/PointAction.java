package com.cookiecamera.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PointAction {
  private PointAction.ActionType type;
  private String value;
  private String extra;
  private String extra2;

  public PointAction(PointAction.ActionType type, String value) {
    this(type, value, null, null);
  }

  public PointAction(PointAction.ActionType type, String value, String extra) {
    this(type, value, extra, null);
  }

  public PointAction(PointAction.ActionType type, String value, String extra, String extra2) {
    this.type = type;
    this.value = value;
    this.extra = extra;
    this.extra2 = extra2;
  }

  public PointAction.ActionType getType() {
    return this.type;
  }

  public String getValue() {
    return this.value;
  }

  public String getExtra() {
    return this.extra;
  }

  public String getExtra2() {
    return this.extra2;
  }

  public void setType(PointAction.ActionType type) {
    this.type = type;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setExtra(String extra) {
    this.extra = extra;
  }

  public void setExtra2(String extra2) {
    this.extra2 = extra2;
  }

  public Map<String, Object> serialize() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", this.type.name());
    map.put("value", this.value);
    if (this.extra != null) {
      map.put("extra", this.extra);
    }

    if (this.extra2 != null) {
      map.put("extra2", this.extra2);
    }

    return map;
  }

  public static PointAction deserialize(Map<String, Object> map) {
    PointAction.ActionType type =
        PointAction.ActionType.valueOf(((String) map.get("type")).toUpperCase());
    String value = (String) map.get("value");
    String extra = (String) map.get("extra");
    String extra2 = (String) map.get("extra2");
    return new PointAction(type, value, extra, extra2);
  }

  public String toShortString() {
    return switch (this.type) {
      case TITLE -> "TITLE: " + truncate(this.value, 20);
      case ACTIONBAR -> "ACTIONBAR: " + truncate(this.value, 20);
      case SOUND -> "SOUND: " + this.value;
      case CMD -> "CMD: " + truncate(this.value, 20);
      case PLAYERCMD -> "PLAYERCMD: " + truncate(this.value, 20);
      case SCRIPT -> "SCRIPT: " + this.value;
      case MSG -> "MSG: " + truncate(this.value, 20);
    };
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    } else {
      return s.length() > max ? s.substring(0, max) + "..." : s;
    }
  }

  public static enum ActionType {
    TITLE,
    ACTIONBAR,
    SOUND,
    CMD,
    PLAYERCMD,
    SCRIPT,
    MSG;
  }
}

