package libcore;

/**
 * JNI bridge to sing-box BoxInstance — controls the sing-box lifecycle.
 * Matches exported JNI functions from libsingboxjni.so (from NekoBox 1.4.2).
 */
public class BoxInstance implements go.Universe.GoObject {
    private final int refnum;

    BoxInstance(int refnum) {
        this.refnum = refnum;
        go.Seq.incGoRef(refnum);
    }

    @Override
    public int incRefnum() {
        go.Seq.incGoRef(refnum);
        return refnum;
    }

    public native void preStart();
    public native void start();
    public native void close();
    public native long queryStats(String tag, String direct);
    public native void selectOutbound(String tag);
    public native void setAsMain();
    public native void setV2rayStats(String tag);
    public native void sleep();
    public native void wake();
}
