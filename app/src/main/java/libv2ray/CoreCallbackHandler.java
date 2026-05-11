package libv2ray;

/**
 * Callback interface for Xray core status events.
 */
public interface CoreCallbackHandler extends go.Universe.GoObject {
    void onEmitStatus(long code, String msg);
    void shutdown();
    long startup();
}
