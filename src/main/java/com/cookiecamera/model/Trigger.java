package com.cookiecamera.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public class Trigger {
  private final String id;
  private Trigger.TriggerType type;
  private String sceneId;
  private String commandPattern;
  private String world;
  private double x1;
  private double y1;
  private double z1;
  private double x2;
  private double y2;
  private double z2;
  private int blockX;
  private int blockY;
  private int blockZ;
  private String requiredItemInHand;
  private String requiredItemInInv;
  private String requiredPermission;
  private int cooldownTicks;
  private boolean oneTime;
  private final List<String> commands = new ArrayList<>();

  public Trigger(String id) {
    this.id = id;
    this.type = Trigger.TriggerType.REGION_ENTER;
    this.cooldownTicks = 0;
    this.oneTime = false;
  }

  public String getId() {
    return this.id;
  }

  public Trigger.TriggerType getType() {
    return this.type;
  }

  public String getSceneId() {
    return this.sceneId;
  }

  public String getWorld() {
    return this.world;
  }

  public String getCommandPattern() {
    return this.commandPattern;
  }

  public double getX1() {
    return this.x1;
  }

  public double getY1() {
    return this.y1;
  }

  public double getZ1() {
    return this.z1;
  }

  public double getX2() {
    return this.x2;
  }

  public double getY2() {
    return this.y2;
  }

  public double getZ2() {
    return this.z2;
  }

  public int getBlockX() {
    return this.blockX;
  }

  public int getBlockY() {
    return this.blockY;
  }

  public int getBlockZ() {
    return this.blockZ;
  }

  public String getRequiredItemInHand() {
    return this.requiredItemInHand;
  }

  public String getRequiredItemInInv() {
    return this.requiredItemInInv;
  }

  public String getRequiredPermission() {
    return this.requiredPermission;
  }

  public int getCooldownTicks() {
    return this.cooldownTicks;
  }

  public boolean isOneTime() {
    return this.oneTime;
  }

  public List<String> getCommands() {
    return this.commands;
  }

  public void setType(Trigger.TriggerType type) {
    this.type = type;
  }

  public void setSceneId(String sceneId) {
    this.sceneId = sceneId;
  }

  public void setWorld(String world) {
    this.world = world;
  }

  public void setCommandPattern(String commandPattern) {
    if (commandPattern == null) {
      this.commandPattern = null;
      return;
    }

    String normalized = commandPattern.trim();
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }

    this.commandPattern = normalized.isEmpty() ? null : normalized;
  }

  public void setRegion(double x1, double y1, double z1, double x2, double y2, double z2) {
    this.x1 = Math.min(x1, x2);
    this.y1 = Math.min(y1, y2);
    this.z1 = Math.min(z1, z2);
    this.x2 = Math.max(x1, x2);
    this.y2 = Math.max(y1, y2);
    this.z2 = Math.max(z1, z2);
  }

  public void setBlock(int x, int y, int z) {
    this.blockX = x;
    this.blockY = y;
    this.blockZ = z;
  }

  public void setRequiredItemInHand(String requiredItemInHand) {
    this.requiredItemInHand = requiredItemInHand;
  }

  public void setRequiredItemInInv(String requiredItemInInv) {
    this.requiredItemInInv = requiredItemInInv;
  }

  public void setRequiredPermission(String requiredPermission) {
    this.requiredPermission = requiredPermission;
  }

  public void setCooldownTicks(int cooldownTicks) {
    this.cooldownTicks = cooldownTicks;
  }

  public void setOneTime(boolean oneTime) {
    this.oneTime = oneTime;
  }

  public boolean isInRegion(String worldName, double px, double py, double pz) {
    if (this.type != Trigger.TriggerType.REGION_ENTER) {
      return false;
    } else {
      return this.world != null && !this.world.equals(worldName)
          ? false
          : px >= this.x1
              && px <= this.x2
              && py >= this.y1
              && py <= this.y2
              && pz >= this.z1
              && pz <= this.z2;
    }
  }

  public boolean isBlock(String worldName, int bx, int by, int bz) {
    if (this.type != Trigger.TriggerType.RIGHT_CLICK_BLOCK) {
      return false;
    } else {
      return this.world != null && !this.world.equals(worldName)
          ? false
          : bx == this.blockX && by == this.blockY && bz == this.blockZ;
    }
  }

  public boolean matchesCommand(String rawCommand) {
    if (this.type != Trigger.TriggerType.COMMAND || this.commandPattern == null) {
      return false;
    }

    String normalized = rawCommand == null ? "" : rawCommand.trim();
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }

    normalized = normalized.toLowerCase(Locale.ROOT);
    String pattern = this.commandPattern.toLowerCase(Locale.ROOT);
    return normalized.equals(pattern) || normalized.startsWith(pattern + " ");
  }

  public static enum TriggerType {
    REGION_ENTER,
    RIGHT_CLICK_BLOCK,
    COMMAND;

    @Nullable
    public static TriggerType fromString(String raw) {
      if (raw == null || raw.isBlank()) {
        return null;
      }

      try {
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
        return null;
      }
    }
  }
}

