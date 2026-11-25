package com.cookiecamera.model;

import java.util.ArrayList;
import java.util.List;

public class Scene {
  private final String id;
  private String displayName;
  private int defaultSpeedTicks;
  private boolean showMarkers;
  private String world;
  private SmoothMode smoothMode;
  private double tension;
  private double smoothing;
  private final List<CameraPoint> points;

  public Scene(String id) {
    this.id = id;
    this.displayName = id;
    this.defaultSpeedTicks = 40;
    this.showMarkers = true;
    this.smoothMode = SmoothMode.CATMULL_ROM;
    this.tension = 0.0;
    this.smoothing = 0.0;
    this.points = new ArrayList<>();
  }

  public String getId() {
    return this.id;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public int getDefaultSpeedTicks() {
    return this.defaultSpeedTicks;
  }

  public boolean isShowMarkers() {
    return this.showMarkers;
  }

  public String getWorld() {
    return this.world;
  }

  public SmoothMode getSmoothMode() {
    return this.smoothMode;
  }

  public double getTension() {
    return this.tension;
  }

  public double getSmoothing() {
    return this.smoothing;
  }

  public List<CameraPoint> getPoints() {
    return this.points;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setDefaultSpeedTicks(int defaultSpeedTicks) {
    this.defaultSpeedTicks = defaultSpeedTicks;
  }

  public void setShowMarkers(boolean showMarkers) {
    this.showMarkers = showMarkers;
  }

  public void setWorld(String world) {
    this.world = world;
  }

  public void setSmoothMode(SmoothMode smoothMode) {
    this.smoothMode = smoothMode;
  }

  public void setTension(double tension) {
    this.tension = Math.max(0.0, Math.min(1.0, tension));
  }

  public void setSmoothing(double smoothing) {
    this.smoothing = Math.max(0.0, Math.min(0.95, smoothing));
  }

  public void addPoint(CameraPoint point) {
    this.points.add(point);
  }

  public void removePoint(int index) {
    if (index >= 0 && index < this.points.size()) {
      this.points.remove(index);
    }
  }

  public void clearPoints() {
    this.points.clear();
  }

  public int getEffectiveDuration(int pointIndex) {
    if (pointIndex >= 0 && pointIndex < this.points.size()) {
      int dur = this.points.get(pointIndex).getDurationTicks();
      return dur <= 0 ? this.defaultSpeedTicks : dur;
    } else {
      return this.defaultSpeedTicks;
    }
  }

  public int getTotalDurationTicks() {
    int total = 0;

    for (int i = 1; i < this.points.size(); i++) {
      total += this.getEffectiveDuration(i);
    }

    return total;
  }
}

