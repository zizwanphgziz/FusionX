package go;

public abstract class Universe {
    private Universe() {}

    static native void _init();

    public static void touch() {
        // Force class loading
    }

    public static final class proxyerror extends Exception implements Seq.GoObject {
        private final int refnum;

        public proxyerror(int refnum) {
            this.refnum = refnum;
            Seq.trackGoRef(refnum, this);
        }

        @Override
        public final int incRefnum() {
            Seq.incGoRef(refnum, this);
            return refnum;
        }

        public native String error();

        @Override
        public String getMessage() {
            return error();
        }
    }
}
