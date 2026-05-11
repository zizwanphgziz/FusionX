package go;

import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Seq {
    private static final Logger log = Logger.getLogger("GoSeq");
    private static boolean loaded = false;

    private Seq() {}

    public interface GoObject {
        int incRefnum();
    }

    public interface Proxy extends GoObject {}

    public static final class Ref {
        public int refnum;
        private Object obj;

        Ref(int refnum, Object obj) {
            this.refnum = refnum;
            this.obj = obj;
        }
    }

    private static final RefTracker tracker = new RefTracker();
    private static final GoRefQueue goRefQueue = new GoRefQueue();
    private static final ConcurrentHashMap<Integer, GoObject> goObjs = new ConcurrentHashMap<>();

    static {
        try {
            System.loadLibrary("gojni");
            loaded = true;
            init();
            Universe._init();
        } catch (Throwable t) {
            log.warning("Failed to load gojni: " + t.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized boolean loadXray() {
        return loaded;
    }

    public static void touch() {
        // Force class loading and static initializer
    }

    // Native methods — signatures must match gomobile JNI_OnLoad registration exactly
    private static native void init();
    static native void destroyRef(int refnum);
    public static native void incGoRef(int refnum, GoObject o);
    static native void setContext(Object ctx);

    public static void setContext(Context ctx) {
        setContext((Object) ctx);
    }

    public static void trackGoRef(int refnum, GoObject o) {
        goObjs.put(refnum, o);
        goRefQueue.track(refnum, o);
    }

    public static void incRefnum(int refnum) {
        tracker.incRefnum(refnum);
    }

    public static int incGoObjectRef(GoObject o) {
        int refnum = o.incRefnum();
        return refnum;
    }

    public static Ref getRef(int refnum) {
        return tracker.get(refnum);
    }

    public static int incRef(Object o) {
        return tracker.inc(o);
    }

    static void decRef(int refnum) {
        tracker.dec(refnum);
    }

    private static class RefTracker {
        private final ConcurrentHashMap<Integer, Ref> refs = new ConcurrentHashMap<>();
        private final AtomicInteger nextNum = new AtomicInteger(42);

        int inc(Object o) {
            int num = nextNum.getAndIncrement();
            refs.put(num, new Ref(num, o));
            return num;
        }

        Ref get(int refnum) {
            return refs.get(refnum);
        }

        synchronized void dec(int refnum) {
            refs.remove(refnum);
        }

        synchronized void incRefnum(int refnum) {
            // increment count
        }
    }

    private static class GoRefQueue {
        void track(int refnum, GoObject o) {
            // Track go reference for GC
        }
    }
}
