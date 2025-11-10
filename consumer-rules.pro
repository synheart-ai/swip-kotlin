# Consumer ProGuard rules for library users
# These rules are applied when the library is consumed by other projects

# Keep SWIP SDK public API
-keep class ai.synheart.swip.SwipSdkManager { *; }
-keep class ai.synheart.swip.models.** { *; }
-keep class ai.synheart.swip.errors.** { *; }

# Keep data classes
-keepclassmembers class ai.synheart.swip.models.** {
    <fields>;
    <methods>;
}

# Keep Kotlin data classes
-keepclassmembers class ai.synheart.swip.** {
    <fields>;
}

# Keep enum classes
-keepclassmembers enum ai.synheart.swip.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

