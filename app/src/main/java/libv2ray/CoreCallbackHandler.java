package libv2ray;

/**
 * Callback interface for Xray core status events.
 * Signatures must match the gomobile-generated native code exactly.
 */
public interface CoreCallbackHandler {
    long onEmitStatus(long code, String msg);
    long shutdown();
    long startup();
}
