# Keep models used by Moshi
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}

# Room schema
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

