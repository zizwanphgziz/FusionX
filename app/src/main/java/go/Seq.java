package go;

import android.content.Context;
import android.util.Log;

/**
 * Go mobile Seq bridge — required by gomobile-generated native libraries.
 * This class provides the serialization layer between Java and Go.
 * Loading is deferred until the VPN core is actually needed.
 */
public class Seq {
    private static boolean loaded = false;
    private static final String TAG = "GoSeq";

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized boolean loadXray() {
        if (loaded) return true;
        try {
            System.loadLibrary("gojni");
            loaded = true;
            Log.i(TAG, "Loaded libgojni.so (xray)");
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load libgojni.so: " + e.getMessage());
            return false;
        }
    }

    public static void initIfLoaded(Context ctx) {
        if (!loaded) return;
        try {
            init(ctx);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Seq.init() not available: " + e.getMessage());
        } catch (Throwable t) {
            Log.w(TAG, "Seq.init() failed: " + t.getMessage());
        }
    }

    public static native void init(Context ctx);
    public static native void destroyRef(int refnum);
    public static native void incGoRef(int refnum);
    public static native void setContext(Object ctx);

    public static int nullRef = 41;

    public static final class Ref {
        public int refnum;
        public Ref(int refnum) {
            this.refnum = refnum;
        }
        @Override
        protected void finalize() throws Throwable {
            if (loaded && refnum != nullRef) {
                try {
                    destroyRef(refnum);
                } catch (UnsatisfiedLinkError e) {
                    // ignore
                }
            }
            super.finalize();
        }
    }

    public static Object getRef(int refnum) {
        return tracker.get(refnum);
    }

    private static final RefTracker tracker = new RefTracker();

    public static int incRef(Object o) {
        return tracker.inc(o);
    }

    private static class RefTracker {
        private final java.util.concurrent.ConcurrentHashMap<Integer, Object> refs =
            new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.AtomicInteger nextNum =
            new java.util.concurrent.atomic.AtomicInteger(42);

        int inc(Object o) {
            int num = nextNum.getAndIncrement();
            refs.put(num, o);
            return num;
        }

        Object get(int refnum) {
            return refs.get(refnum);
        }
    }
}
