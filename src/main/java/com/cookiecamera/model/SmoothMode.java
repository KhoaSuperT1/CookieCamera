package com.cookiecamera.model;

import java.util.Locale;

public enum SmoothMode {
  CATMULL_ROM,
  BSPLINE,
  HERMITE,
  LINEAR;

  public static SmoothMode fromString(String s) {
    if (s == null || s.isBlank()) {
      return CATMULL_ROM;
    }

    try {
      return valueOf(s.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return CATMULL_ROM;
    }
  }
}

