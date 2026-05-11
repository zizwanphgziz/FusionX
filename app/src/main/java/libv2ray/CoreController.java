package libv2ray;

import go.Seq;
import java.util.Arrays;

public final class CoreController implements Seq.Proxy {
    private final int refnum;

    private static native int __NewCoreController(CoreCallbackHandler coreCallbackHandler);

    public final native CoreCallbackHandler getCallbackHandler();
    public final native boolean getIsRunning();
    public native long measureDelay(String str) throws Exception;
    public native long queryStats(String str, String str2);
    public final native void setCallbackHandler(CoreCallbackHandler coreCallbackHandler);
    public final native void setIsRunning(boolean z);
    public native void startLoop(String str, int i) throws Exception;
    public native void stopLoop() throws Exception;

    static {
        Libv2ray.touch();
    }

    @Override
    public final int incRefnum() {
        Seq.incGoRef(this.refnum, this);
        return this.refnum;
    }

    public CoreController(CoreCallbackHandler coreCallbackHandler) {
        int ref = __NewCoreController(coreCallbackHandler);
        this.refnum = ref;
        Seq.trackGoRef(ref, this);
    }

    CoreController(int i) {
        this.refnum = i;
        Seq.trackGoRef(i, this);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CoreController)) {
            return false;
        }
        CoreController coreController = (CoreController) obj;
        CoreCallbackHandler callbackHandler = getCallbackHandler();
        CoreCallbackHandler callbackHandler2 = coreController.getCallbackHandler();
        if (callbackHandler == null) {
            if (callbackHandler2 != null) {
                return false;
            }
        } else if (!callbackHandler.equals(callbackHandler2)) {
            return false;
        }
        return getIsRunning() == coreController.getIsRunning();
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{getCallbackHandler(), Boolean.valueOf(getIsRunning())});
    }

    public String toString() {
        return "CoreController{CallbackHandler:" + getCallbackHandler() + ",IsRunning:" + getIsRunning() + ",}";
    }
}
