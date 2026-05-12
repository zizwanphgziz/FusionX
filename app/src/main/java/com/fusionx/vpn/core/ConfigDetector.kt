package com.fusionx.vpn.core

import android.util.Base64
import android.net.Uri
import com.fusionx.vpn.model.CoreType
import com.fusionx.vpn.model.ServerProfile
import java.net.URLDecoder

object ConfigDetector {

    fun detectAndParse(input: String): List<ServerProfile> {
        val trimmed = input.trim()

        // Try base64 decode first
        val decoded = tryBase64Decode(trimmed)
        if (decoded != null && decoded != trimmed) {
            return decoded.lines()
                .filter { it.isNotBlank() }
                .flatMap { parseSingleLink(it.trim()) }
        }

        // Multi-line input
        if (trimmed.contains("\n")) {
            return trimmed.lines()
                .filter { it.isNotBlank() }
                .flatMap { parseSingleLink(it.trim()) }
        }

        return parseSingleLink(trimmed)
    }

    private fun parseSingleLink(link: String): List<ServerProfile> {
        return try {
            when {
                link.startsWith("vless://") -> listOf(parseVless(link))
                link.startsWith("vmess://") -> listOf(parseVmess(link))
                link.startsWith("trojan://") -> listOf(parseTrojan(link))
                link.startsWith("ss://") -> listOf(parseShadowsocks(link))
                link.startsWith("ssr://") -> listOf(parseSsr(link))
                link.startsWith("hy2://") || link.startsWith("hysteria2://") -> listOf(parseHysteria2(link))
                link.startsWith("tuic://") -> listOf(parseTuic(link))
                link.startsWith("wireguard://") || link.startsWith("wg://") -> listOf(parseWireGuard(link))
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseVless(link: String): ServerProfile {
        val uri = Uri.parse(link)
        val uuid = uri.userInfo ?: ""
        val address = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val name = uri.fragment ?: ""

        val params = parseQueryParams(uri)
        val path = urlDecode(params["path"] ?: "")
        val host = params["host"] ?: ""
        val security = params["security"] ?: "none"
        val network = params["type"] ?: "tcp"
        val sni = params["sni"] ?: ""
        val fp = params["fp"] ?: ""
        val alpn = params["alpn"] ?: ""
        val flow = params["flow"] ?: ""
        val encryption = params["encryption"] ?: "none"

        // STRX detection
        val isStrx = path.contains("[split]", ignoreCase = true) ||
                path.contains("STRX", ignoreCase = true) ||
                host.startsWith("strx-payload://", ignoreCase = true)

        return ServerProfile(
            name = urlDecode(name),
            protocol = "vless",
            address = address,
            port = port,
            uuid = uuid,
            security = encryption,
            network = network,
            host = if (isStrx) host.removePrefix("strx-payload://") else host,
            path = path,
            tls = security,
            sni = sni,
            fingerprint = fp,
            alpn = alpn,
            flow = flow,
            isStrx = isStrx,
            strxPayload = if (isStrx) buildStrxPayload(path) else ""
        )
    }

    private fun parseVmess(link: String): ServerProfile {
        val encoded = link.removePrefix("vmess://")
        val json = tryBase64Decode(encoded) ?: return ServerProfile(protocol = "vmess")

        return try {
            val obj = org.json.JSONObject(json)
            ServerProfile(
                name = obj.optString("ps", ""),
                protocol = "vmess",
                address = obj.optString("add", ""),
                port = obj.optString("port", "443").toIntOrNull() ?: 443,
                uuid = obj.optString("id", ""),
                alterId = obj.optString("aid", "0").toIntOrNull() ?: 0,
                security = obj.optString("scy", "auto"),
                network = obj.optString("net", "tcp"),
                headerType = obj.optString("type", "none"),
                host = obj.optString("host", ""),
                path = obj.optString("path", ""),
                tls = obj.optString("tls", ""),
                sni = obj.optString("sni", ""),
                fingerprint = obj.optString("fp", ""),
                alpn = obj.optString("alpn", "")
            )
        } catch (e: Exception) {
            ServerProfile(protocol = "vmess")
        }
    }

    private fun parseTrojan(link: String): ServerProfile {
        val uri = Uri.parse(link)
        val password = uri.userInfo ?: ""
        val address = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val name = uri.fragment ?: ""
        val params = parseQueryParams(uri)

        return ServerProfile(
            name = urlDecode(name),
            protocol = "trojan",
            address = address,
            port = port,
            uuid = password,
            network = params["type"] ?: "tcp",
            host = params["host"] ?: "",
            path = urlDecode(params["path"] ?: ""),
            tls = params["security"] ?: "tls",
            sni = params["sni"] ?: "",
            fingerprint = params["fp"] ?: "",
            alpn = params["alpn"] ?: ""
        )
    }

    private fun parseShadowsocks(link: String): ServerProfile {
        val cleaned = link.removePrefix("ss://")
        val hashIdx = cleaned.lastIndexOf('#')
        val name = if (hashIdx >= 0) urlDecode(cleaned.substring(hashIdx + 1)) else ""
        val main = if (hashIdx >= 0) cleaned.substring(0, hashIdx) else cleaned

        // ss://base64(method:password)@host:port
        val atIdx = main.indexOf('@')
        return if (atIdx >= 0) {
            val userInfo = tryBase64Decode(main.substring(0, atIdx)) ?: main.substring(0, atIdx)
            val colonIdx = userInfo.indexOf(':')
            val method = if (colonIdx >= 0) userInfo.substring(0, colonIdx) else ""
            val password = if (colonIdx >= 0) userInfo.substring(colonIdx + 1) else userInfo
            val hostPort = main.substring(atIdx + 1)
            val lastColon = hostPort.lastIndexOf(':')
            val host = if (lastColon >= 0) hostPort.substring(0, lastColon) else hostPort
            val port = if (lastColon >= 0) hostPort.substring(lastColon + 1).toIntOrNull() ?: 443 else 443

            ServerProfile(
                name = name,
                protocol = "ss",
                address = host,
                port = port,
                uuid = password,
                security = method
            )
        } else {
            val decoded = tryBase64Decode(main) ?: main
            parseShadowsocks("ss://$decoded${if (name.isNotEmpty()) "#$name" else ""}")
        }
    }

    private fun parseSsr(link: String): ServerProfile {
        val encoded = link.removePrefix("ssr://")
        val decoded = tryBase64Decode(encoded) ?: return ServerProfile(protocol = "ssr")
        val parts = decoded.split(":")
        if (parts.size < 6) return ServerProfile(protocol = "ssr")

        return ServerProfile(
            name = "SSR Server",
            protocol = "ssr",
            address = parts[0],
            port = parts[1].toIntOrNull() ?: 443,
            uuid = parts[5].substringBefore('/').let { tryBase64Decode(it) ?: it },
            security = parts[3]
        )
    }

    private fun parseHysteria2(link: String): ServerProfile {
        val uri = Uri.parse(link.replace("hysteria2://", "hy2://"))
        val password = uri.userInfo ?: ""
        val address = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val name = uri.fragment ?: ""
        val params = parseQueryParams(uri)

        return ServerProfile(
            name = urlDecode(name),
            protocol = "hy2",
            address = address,
            port = port,
            uuid = password,
            sni = params["sni"] ?: "",
            obfsType = params["obfs"] ?: "",
            obfsPassword = params["obfs-password"] ?: "",
            fingerprint = params["pinSHA256"] ?: "",
            upMbps = params["up"]?.replace("mbps", "", true)?.toIntOrNull() ?: 0,
            downMbps = params["down"]?.replace("mbps", "", true)?.toIntOrNull() ?: 0
        )
    }

    private fun parseTuic(link: String): ServerProfile {
        val uri = Uri.parse(link)
        val userInfo = uri.userInfo ?: ""
        val parts = userInfo.split(":")
        val uuid = parts.getOrElse(0) { "" }
        val password = parts.getOrElse(1) { "" }
        val name = uri.fragment ?: ""
        val params = parseQueryParams(uri)

        return ServerProfile(
            name = urlDecode(name),
            protocol = "tuic",
            address = uri.host ?: "",
            port = if (uri.port > 0) uri.port else 443,
            uuid = uuid,
            security = password,
            sni = params["sni"] ?: "",
            alpn = params["alpn"] ?: ""
        )
    }

    private fun parseWireGuard(link: String): ServerProfile {
        val uri = Uri.parse(link.replace("wg://", "wireguard://"))
        val name = uri.fragment ?: ""
        val params = parseQueryParams(uri)

        return ServerProfile(
            name = urlDecode(name),
            protocol = "wireguard",
            address = uri.host ?: "",
            port = if (uri.port > 0) uri.port else 51820,
            uuid = params["privateKey"] ?: "",
            publicKey = params["publicKey"] ?: "",
            security = params["presharedKey"] ?: ""
        )
    }

    fun buildStrxPayload(rawPath: String): String {
        return rawPath
            .replace("[crlf]", "\r\n", ignoreCase = true)
            .replace("[split]", "\u0000SPLIT\u0000", ignoreCase = true)
            .replace("[host]", "{HOST}", ignoreCase = true)
    }

    private fun parseQueryParams(uri: Uri): Map<String, String> {
        val params = mutableMapOf<String, String>()
        uri.queryParameterNames?.forEach { key ->
            params[key] = uri.getQueryParameter(key) ?: ""
        }
        return params
    }

    private fun tryBase64Decode(input: String): String? {
        return try {
            val cleaned = input.trim().replace("-", "+").replace("_", "/")
            val padded = when (cleaned.length % 4) {
                2 -> "$cleaned=="
                3 -> "$cleaned="
                else -> cleaned
            }
            String(Base64.decode(padded, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun urlDecode(s: String): String {
        return try {
            URLDecoder.decode(s, "UTF-8")
        } catch (e: Exception) {
            s
        }
    }
}
