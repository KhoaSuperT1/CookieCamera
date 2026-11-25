package com.cookiecamera.model;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum EaseType {
  LINEAR,
  SMOOTH,
  EASE_IN,
  EASE_OUT,
  EASE_IN_OUT,
  HOLD;

  @Nullable
  public static EaseType fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    try {
      return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  public double apply(double t) {
    return switch (this) {
      case LINEAR -> t;
      case SMOOTH -> t * t * (3.0 - 2.0 * t);
      case EASE_IN -> t * t;
      case EASE_OUT -> 1.0 - (1.0 - t) * (1.0 - t);
      case EASE_IN_OUT -> t < 0.5 ? 2.0 * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
      case HOLD -> 0.0;
    };
  }
}

