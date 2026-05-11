package libv2ray;

/**
 * JNI bridge to Xray CoreController — controls the Xray-core lifecycle.
 * Matches exported JNI functions from libxrayjni.so (formerly libgojni.so from v2rayNG STRX).
 */
public class CoreController implements go.Universe.GoObject {
    private final int refnum;

    CoreController(int refnum) {
        this.refnum = refnum;
        go.Seq.incGoRef(refnum);
    }

    @Override
    public int incRefnum() {
        go.Seq.incGoRef(refnum);
        return refnum;
    }

    public native void startLoop(String config);
    public native boolean stopLoop();
    public native boolean getIsRunning();
    public native void setIsRunning(boolean running);
    public native long measureDelay(String url);
    public native long queryStats(String tag, String direct);
    public native CoreCallbackHandler getCallbackHandler();
    public native void setCallbackHandler(CoreCallbackHandler handler);
}
