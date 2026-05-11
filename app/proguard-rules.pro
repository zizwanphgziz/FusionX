-keep class libv2ray.** { *; }
-keep class libcore.** { *; }
-keep class go.** { *; }
-keep class com.fusionx.vpn.service.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
