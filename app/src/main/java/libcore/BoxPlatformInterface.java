package libcore;

/**
 * Platform interface for sing-box — provides Android-specific callbacks.
 */
public interface BoxPlatformInterface extends go.Seq.GoObject {
    int autoDetectInterfaceControl(int fd);
    int findConnectionOwner(int ipProto, String srcAddr, int srcPort, String dstAddr, int dstPort);
    int openTun(String tunOptions);
    String packageNameByUid(int uid);
    int uidByPackageName(String packageName);
    boolean useProcFS();
    String wifiState();
}
