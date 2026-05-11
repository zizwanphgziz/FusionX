package libv2ray;

public class CoreController implements go.Seq.GoObject {
    private final int refnum;

    CoreController(int refnum) {
        this.refnum = refnum;
        go.Seq.trackGoRef(refnum, this);
    }

    public CoreController(CoreCallbackHandler handler) {
        this.refnum = __NewCoreController(handler);
        go.Seq.trackGoRef(refnum, this);
    }

    private static native int __NewCoreController(CoreCallbackHandler handler);

    @Override
    public final int incRefnum() {
        go.Seq.incGoRef(refnum, this);
        return refnum;
    }

    public native void startLoop(String config, int fd);
    public native void stopLoop();
    public final native boolean getIsRunning();
    public final native void setIsRunning(boolean running);
    public native long measureDelay(String url);
    public native long queryStats(String tag, String direct);
    public final native CoreCallbackHandler getCallbackHandler();
    public final native void setCallbackHandler(CoreCallbackHandler handler);

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CoreController)) return false;
        CoreController that = (CoreController) o;
        return this.refnum == that.refnum;
    }

    @Override
    public int hashCode() {
        return refnum;
    }

    @Override
    public String toString() {
        return "CoreController{refnum=" + refnum + "}";
    }
}
