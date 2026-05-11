package libv2ray;

/**
 * JNI bridge to Xray CoreController — controls the Xray-core lifecycle.
 * Signatures match the gomobile-generated native code from v2rayNG STRX.
 */
public class CoreController implements go.Universe.GoObject {
    private final int refnum;

    public CoreController(int refnum) {
        this.refnum = refnum;
        go.Seq.incGoRef(refnum);
    }

    public CoreController(CoreCallbackHandler handler) {
        this.refnum = __NewCoreController(handler);
        go.Seq.incGoRef(refnum);
    }

    private static native int __NewCoreController(CoreCallbackHandler handler);

    @Override
    public int incRefnum() {
        go.Seq.incGoRef(refnum);
        return refnum;
    }

    public native void startLoop(String config, int fd);
    public native void stopLoop();
    public native boolean getIsRunning();
    public native void setIsRunning(boolean running);
    public native long measureDelay(String url);
    public native long queryStats(String tag, String direct);
    public native CoreCallbackHandler getCallbackHandler();
    public native void setCallbackHandler(CoreCallbackHandler handler);
}
