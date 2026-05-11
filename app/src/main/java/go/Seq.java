package go;

import android.content.Context;

/**
 * Go mobile Seq bridge — required by gomobile-generated native libraries.
 * This class provides the serialization layer between Java and Go.
 */
public class Seq {
    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("xrayjni");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            // Will be loaded later when core is selected
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void loadXray() {
        if (!loaded) {
            System.loadLibrary("xrayjni");
            loaded = true;
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
            if (refnum != nullRef) {
                destroyRef(refnum);
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
