package com.serv.common;

/**
 * Settings the admins can change without a redeployment, with their default value and allowed range.
 * Stored by name: never rename a value (the stored row would be ignored and the default used again).
 */
public enum SettingKey {

    /** How long a certification is valid once approved, in months. Applies to certifications approved afterwards. */
    CERTIFICATION_VALIDITY_MONTHS(8, 1, 36),
    /** How many months before its end a certification can be renewed. Must be shorter than the validity. */
    CERTIFICATION_RENEWAL_MONTHS(2, 0, 12),
    /** Storage for photos and videos of a free account, in MB. */
    STORAGE_LIMIT_MB(50, 10, 10_240);

    private final int defaultValue;
    private final int min;
    private final int max;

    SettingKey(int defaultValue, int min, int max) {
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public int defaultValue() { return defaultValue; }
    public int min() { return min; }
    public int max() { return max; }
}
