package libv2ray;

/**
 * JNI bridge to Xray/V2Ray core library.
 * Matches exported JNI functions from libxrayjni.so (v2rayNG STRX mod).
 */
public abstract class Libv2ray {
    private Libv2ray() {}

    static {
        go.Seq.loadXray();
        _init();
    }

    private static native void _init();

    public static native String checkVersionX();

    public static native void initCoreEnv(String assetsPath);

    public static native CoreController newCoreController(CoreCallbackHandler handler);

    public static native long measureOutboundDelay(String config);

    static final class proxyCoreCallbackHandler implements CoreCallbackHandler {
        private final int refnum;

        proxyCoreCallbackHandler(int refnum) {
            this.refnum = refnum;
            go.Seq.incGoRef(refnum);
        }

        @Override
        public int incRefnum() {
            go.Seq.incGoRef(refnum);
            return refnum;
        }

        @Override
        public native void onEmitStatus(long code, String msg);

        @Override
        public native void shutdown();

        @Override
        public native long startup();
    }
}
