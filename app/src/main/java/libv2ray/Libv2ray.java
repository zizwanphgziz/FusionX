package libv2ray;

import go.Seq;

public abstract class Libv2ray {
    private static native void _init();

    public static native String checkVersionX();
    public static native void initCoreEnv(String str, String str2);
    public static native long measureOutboundDelay(String str, String str2) throws Exception;
    public static native CoreController newCoreController(CoreCallbackHandler coreCallbackHandler);

    public static void touch() {
    }

    static {
        Seq.touch();
        _init();
    }

    private Libv2ray() {
    }

    private static final class proxyCoreCallbackHandler implements Seq.Proxy, CoreCallbackHandler {
        private final int refnum;

        @Override
        public native long onEmitStatus(long j, String str);

        @Override
        public native long shutdown();

        @Override
        public native long startup();

        @Override
        public final int incRefnum() {
            Seq.incGoRef(this.refnum, this);
            return this.refnum;
        }

        proxyCoreCallbackHandler(int i) {
            this.refnum = i;
            Seq.trackGoRef(i, this);
        }
    }
}
