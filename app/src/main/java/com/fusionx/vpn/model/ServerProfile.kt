package com.fusionx.vpn.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "server_profiles")
@Serializable
data class ServerProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val protocol: String = "",       // vless, vmess, trojan, ss, ssr, hy2, tuic, wireguard
    val address: String = "",
    val port: Int = 443,
    val uuid: String = "",           // user id / password
    val alterId: Int = 0,
    val security: String = "auto",   // encryption method
    val network: String = "tcp",     // tcp, ws, grpc, h2, quic, kcp
    val headerType: String = "none",
    val host: String = "",
    val path: String = "",
    val tls: String = "",            // none, tls, reality, xtls
    val sni: String = "",
    val fingerprint: String = "",
    val alpn: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val flow: String = "",

    // STRX-specific fields
    val isStrx: Boolean = false,
    val strxPayload: String = "",    // raw STRX payload with [crlf], [split] markers

    // sing-box specific
    val obfsType: String = "",
    val obfsPassword: String = "",
    val upMbps: Int = 0,
    val downMbps: Int = 0,

    // Metadata
    val group: String = "Default",
    val subscriptionUrl: String = "",
    val latency: Long = -1,
    val isSelected: Boolean = false,

    // Full config JSON (for raw xray/sing-box configs)
    val rawConfig: String = ""
) {
    val coreType: CoreType
        get() = when (protocol.lowercase()) {
            "hy2", "hysteria2", "tuic", "wireguard" -> CoreType.SING_BOX
            else -> CoreType.XRAY
        }

    val displayName: String
        get() = name.ifEmpty { "$protocol://$address:$port" }
}

enum class CoreType {
    XRAY,
    SING_BOX
}
