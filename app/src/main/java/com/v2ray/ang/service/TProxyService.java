package com.v2ray.ang.service;

import android.util.Log;

/**
 * JNI bridge to hev-socks5-tunnel (tun2socks).
 * Must match the class name the .so was compiled against.
 */
public class TProxyService {
    private static final String TAG = "TProxyService";
    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("hev-socks5-tunnel");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "hev-socks5-tunnel not available: " + e.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static native void TProxyStartService(String configPath, int fd);
    public static native void TProxyStopService();
}
