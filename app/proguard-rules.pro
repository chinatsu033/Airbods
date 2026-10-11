-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn org.lsposed.hiddenapibypass.**
-keep class org.lsposed.hiddenapibypass.** { *; }
-keepclassmembers class android.bluetooth.BluetoothDevice {
    public *** createL2capSocket(...);
    public *** createInsecureL2capSocket(...);
}
-keepclassmembers class android.bluetooth.BluetoothSocket { *; }
