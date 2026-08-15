# Keep the gomobile-generated Go entry points and callback interfaces.
-keep class go.** { *; }
-keep class getsauce.** { *; }
-keep class mobile.** { *; }
-keep interface mobile.** { *; }
-keep class app.sahal.getsauce.bridge.** { *; }

# ffmpeg-kit uses reflection for its Config bridge.
-keep class com.arthenica.** { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class app.sahal.getsauce.**$$serializer { *; }
-keepclassmembers class app.sahal.getsauce.** {
    *** Companion;
}
-keepclasseswithmembers class app.sahal.getsauce.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room needs to keep entity/DAO constructors accessible via reflection.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
