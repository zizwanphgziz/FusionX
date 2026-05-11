package go;

/**
 * Go Universe — handles Go error type bridging.
 */
public abstract class Universe {
    private Universe() {}

    public static native void _init();

    public interface error {
        String error();
    }

    public interface GoObject {
        int incRefnum();
    }

    public static final class proxyerror implements error, GoObject {
        private final int refnum;

        public proxyerror(int refnum) {
            this.refnum = refnum;
            Seq.incGoRef(refnum);
        }

        @Override
        public int incRefnum() {
            Seq.incGoRef(refnum);
            return refnum;
        }

        @Override
        public native String error();
    }
}
