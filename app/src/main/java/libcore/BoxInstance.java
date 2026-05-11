package libcore;

/**
 * JNI bridge to sing-box BoxInstance — controls the sing-box lifecycle.
 * Note: sing-box has its own Go runtime, separate from Xray's libgojni.so.
 */
public class BoxInstance implements go.Seq.GoObject {
    private final int refnum;

    BoxInstance(int refnum) {
        this.refnum = refnum;
        go.Seq.trackGoRef(refnum, this);
    }

    @Override
    public int incRefnum() {
        go.Seq.incGoRef(refnum, this);
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
