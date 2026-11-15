package com.rommelguerra.prototipomusichaptic.haptics;

/** Logical amplitude multiplier; firmware retains its own calibrated gain. */
public final class HapticIntensity {
    private HapticIntensity() {}
    public static boolean valid(String value) {
        return "BAJA".equals(value) || "MEDIA".equals(value) || "ALTA".equals(value);
    }
    public static int percent(String value) {
        if ("BAJA".equals(value)) return 25;
        if ("MEDIA".equals(value)) return 60;
        return 100;
    }
}
