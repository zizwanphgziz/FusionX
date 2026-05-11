package libv2ray;

/**
 * JNI bridge to Xray/V2Ray core library.
 * Signatures match the gomobile-generated native code from v2rayNG STRX.
 */
public abstract class Libv2ray {
    private Libv2ray() {}

    static {
        go.Seq.loadXray();
        try {
            _init();
        } catch (Throwable t) {
            // Defer init failure
        }
    }

    private static native void _init();

    public static native String checkVersionX();

    public static native void initCoreEnv(String assetsPath, String tempPath);

    public static native CoreController newCoreController(CoreCallbackHandler handler);

    public static native long measureOutboundDelay(String config, String url);

    public static native void touch();

    static final class proxyCoreCallbackHandler implements CoreCallbackHandler {
        private final int refnum;

        proxyCoreCallbackHandler(int refnum) {
            this.refnum = refnum;
            go.Seq.incGoRef(refnum);
        }

        public int incRefnum() {
            go.Seq.incGoRef(refnum);
            return refnum;
        }

        @Override
        public native long onEmitStatus(long code, String msg);

        @Override
        public native long shutdown();

        @Override
        public native long startup();
    }
}
