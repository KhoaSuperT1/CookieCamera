package com.cookiecamera.math;

public final class CatmullRom {
  private CatmullRom() {}

  public static double interpolate(double p0, double p1, double p2, double p3, double t) {
    double t2 = t * t;
    double t3 = t2 * t;
    return 0.5
        * (2.0 * p1
            + (-p0 + p2) * t
            + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
            + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
  }

  public static double[] interpolate3D(
      double[] p0, double[] p1, double[] p2, double[] p3, double t) {
    return new double[] {
      interpolate(p0[0], p1[0], p2[0], p3[0], t),
      interpolate(p0[1], p1[1], p2[1], p3[1], t),
      interpolate(p0[2], p1[2], p2[2], p3[2], t)
    };
  }

  public static double bspline(double p0, double p1, double p2, double p3, double t) {
    double t2 = t * t;
    double t3 = t2 * t;
    return 0.16666666666666666
        * ((-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
            + (3.0 * p0 - 6.0 * p1 + 3.0 * p2) * t2
            + (-3.0 * p0 + 3.0 * p2) * t
            + (p0 + 4.0 * p1 + p2));
  }

  public static double[] bspline3D(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
    return new double[] {
      bspline(p0[0], p1[0], p2[0], p3[0], t),
      bspline(p0[1], p1[1], p2[1], p3[1], t),
      bspline(p0[2], p1[2], p2[2], p3[2], t)
    };
  }

  public static double hermite(
      double p0, double p1, double p2, double p3, double t, double tension) {
    double t2 = t * t;
    double t3 = t2 * t;
    double m0 = (1.0 - tension) * (p2 - p0) * 0.5;
    double m1 = (1.0 - tension) * (p3 - p1) * 0.5;
    return (2.0 * t3 - 3.0 * t2 + 1.0) * p1
        + (t3 - 2.0 * t2 + t) * m0
        + (-2.0 * t3 + 3.0 * t2) * p2
        + (t3 - t2) * m1;
  }

  public static double[] hermite3D(
      double[] p0, double[] p1, double[] p2, double[] p3, double t, double tension) {
    return new double[] {
      hermite(p0[0], p1[0], p2[0], p3[0], t, tension),
      hermite(p0[1], p1[1], p2[1], p3[1], t, tension),
      hermite(p0[2], p1[2], p2[2], p3[2], t, tension)
    };
  }

  private static float normAngle(float a) {
    a %= 360.0F;
    if (a > 180.0F) {
      a -= 360.0F;
    }

    if (a < -180.0F) {
      a += 360.0F;
    }

    return a;
  }

  public static float lerpAngle(float a, float b, double t) {
    float diff = normAngle(b - a);
    return a + diff * (float) t;
  }

  public static float splineAngle(float a0, float a1, float a2, float a3, double t) {
    float u0 = a1 + normAngle(a0 - a1);
    float u2 = a1 + normAngle(a2 - a1);
    float u3 = u2 + normAngle(a3 - a2);
    return (float) interpolate(u0, a1, u2, u3, t);
  }

  public static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }
}

