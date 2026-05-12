package libcore;

/**
 * JNI bridge to sing-box core library.
 * Matches exported JNI functions from libsingboxjni.so (NekoBox 1.4.2).
 * Note: Cannot be loaded simultaneously with Xray core due to Go runtime conflicts.
 */
public abstract class Libcore {
    private Libcore() {}

    private static boolean initialized = false;

    public static synchronized void ensureInitialized() {
        if (!initialized) {
            System.loadLibrary("singboxjni");
            _init();
            initialized = true;
        }
    }

    private static native void _init();

    public static native void initCore(String basePath, String tempPath, long statusPort, String statusAddr);

    public static native BoxInstance newSingBoxInstance(String config);

    public static native void forceGc();

    public static native void resetAllConnections();

    public static native String sha1(String data);

    public static native void nekoLogClear();

    public static native void nekoLogPrintln(String msg);
}
