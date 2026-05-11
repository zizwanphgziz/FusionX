package libv2ray;

public abstract class Libv2ray {
    private Libv2ray() {}

    static {
        go.Seq.touch();
    }

    private static native void _init();
    public static native String checkVersionX();
    public static native void initCoreEnv(String assetsPath, String tempPath);
    public static native CoreController newCoreController(CoreCallbackHandler handler);
    public static native long measureOutboundDelay(String config, String url);

    public static void touch() {
        // Force class loading
    }

    static final class proxyCoreCallbackHandler implements CoreCallbackHandler, go.Seq.GoObject {
        private final int refnum;

        proxyCoreCallbackHandler(int refnum) {
            this.refnum = refnum;
            go.Seq.trackGoRef(refnum, this);
        }

        @Override
        public final int incRefnum() {
            go.Seq.incGoRef(refnum, this);
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
