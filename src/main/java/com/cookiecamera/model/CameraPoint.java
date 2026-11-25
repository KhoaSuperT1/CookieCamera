package com.cookiecamera.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

public class CameraPoint implements ConfigurationSerializable {
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int durationTicks;
  private float fov;
  private EaseType easeType;
  private final List<PointAction> actions = new ArrayList<>();

  public CameraPoint(Location loc, int durationTicks, float fov, EaseType easeType) {
    this.x = loc.getX();
    this.y = loc.getY();
    this.z = loc.getZ();
    this.yaw = loc.getYaw();
    this.pitch = loc.getPitch();
    this.durationTicks = durationTicks;
    this.fov = fov;
    this.easeType = easeType;
  }

  public CameraPoint(
      double x,
      double y,
      double z,
      float yaw,
      float pitch,
      int durationTicks,
      float fov,
      EaseType easeType) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.durationTicks = durationTicks;
    this.fov = fov;
    this.easeType = easeType;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public int getDurationTicks() {
    return this.durationTicks;
  }

  public float getFov() {
    return this.fov;
  }

  public EaseType getEaseType() {
    return this.easeType;
  }

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }

  public void setZ(double z) {
    this.z = z;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public void setDurationTicks(int durationTicks) {
    this.durationTicks = durationTicks;
  }

  public void setFov(float fov) {
    this.fov = fov;
  }

  public void setEaseType(EaseType easeType) {
    this.easeType = easeType;
  }

  public List<PointAction> getActions() {
    return this.actions;
  }

  public Location toLocation(World world) {
    return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
  }

  @NotNull
  public Map<String, Object> serialize() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("x", this.x);
    map.put("y", this.y);
    map.put("z", this.z);
    map.put("yaw", this.yaw);
    map.put("pitch", this.pitch);
    map.put("durationTicks", this.durationTicks);
    map.put("fov", this.fov);
    map.put("easeType", this.easeType.name());
    if (!this.actions.isEmpty()) {
      List<Map<String, Object>> actionsList = new ArrayList<>();

      for (PointAction a : this.actions) {
        actionsList.add(a.serialize());
      }

      map.put("actions", actionsList);
    }

    return map;
  }

  public static CameraPoint deserialize(Map<String, Object> map) {
    double x = ((Number) map.get("x")).doubleValue();
    double y = ((Number) map.get("y")).doubleValue();
    double z = ((Number) map.get("z")).doubleValue();
    float yaw = ((Number) map.get("yaw")).floatValue();
    float pitch = ((Number) map.get("pitch")).floatValue();
    int dur = ((Number) map.get("durationTicks")).intValue();
    float fov = ((Number) map.getOrDefault("fov", -1.0F)).floatValue();
    EaseType ease =
        EaseType.fromString((String) map.getOrDefault("easeType", "LINEAR"));
    if (ease == null) {
      ease = EaseType.LINEAR;
    }
    CameraPoint point = new CameraPoint(x, y, z, yaw, pitch, dur, fov, ease);
    Object actionsRaw = map.get("actions");
    if (actionsRaw instanceof List<?> actionList) {
      for (Object item : actionList) {
        if (item instanceof Map<?, ?> rawAction) {
          try {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionMap = (Map<String, Object>) rawAction;
            point.getActions().add(PointAction.deserialize(actionMap));
          } catch (Exception ignored) {
            // Ignore malformed action entries to keep scene loading resilient.
          }
        }
      }
    }

    return point;
  }
}

