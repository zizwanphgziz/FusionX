package com.fusionx.vpn.core

import com.fusionx.vpn.model.ServerProfile
import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigBuilder {

    fun buildConfig(profile: ServerProfile, tunEnabled: Boolean = false): String {
        val config = JSONObject()

        // Log
        config.put("log", JSONObject().apply {
            put("level", "warn")
        })

        // DNS
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "remote")
                    put("address", "tls://1.1.1.1")
                    put("detour", "proxy")
                })
                put(JSONObject().apply {
                    put("tag", "local")
                    put("address", "local")
                    put("detour", "direct")
                })
            })
        })

        // Inbounds
        val inbounds = JSONArray()
        if (tunEnabled) {
            inbounds.put(JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                put("interface_name", "tun0")
                put("inet4_address", "172.19.0.1/30")
                put("auto_route", true)
                put("strict_route", true)
                put("sniff", true)
            })
        } else {
            inbounds.put(JSONObject().apply {
                put("type", "socks")
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("listen_port", 10808)
            })
            inbounds.put(JSONObject().apply {
                put("type", "http")
                put("tag", "http-in")
                put("listen", "127.0.0.1")
                put("listen_port", 10809)
            })
        }
        config.put("inbounds", inbounds)

        // Outbounds
        val outbounds = JSONArray()
        outbounds.put(buildOutbound(profile))
        outbounds.put(JSONObject().apply {
            put("type", "direct")
            put("tag", "direct")
        })
        outbounds.put(JSONObject().apply {
            put("type", "block")
            put("tag", "block")
        })
        outbounds.put(JSONObject().apply {
            put("type", "dns")
            put("tag", "dns-out")
        })
        config.put("outbounds", outbounds)

        // Route
        config.put("route", JSONObject().apply {
            put("auto_detect_interface", true)
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("protocol", "dns")
                    put("outbound", "dns-out")
                })
                put(JSONObject().apply {
                    put("geoip", JSONArray().put("private"))
                    put("outbound", "direct")
                })
            })
        })

        return config.toString(2)
    }

    private fun buildOutbound(profile: ServerProfile): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")

        when (profile.protocol.lowercase()) {
            "hy2", "hysteria2" -> {
                outbound.put("type", "hysteria2")
                outbound.put("server", profile.address)
                outbound.put("server_port", profile.port)
                outbound.put("password", profile.uuid)
                if (profile.sni.isNotEmpty()) {
                    outbound.put("tls", JSONObject().apply {
                        put("enabled", true)
                        put("server_name", profile.sni)
                    })
                }
                if (profile.obfsType.isNotEmpty()) {
                    outbound.put("obfs", JSONObject().apply {
                        put("type", profile.obfsType)
                        put("password", profile.obfsPassword)
                    })
                }
                if (profile.upMbps > 0) outbound.put("up_mbps", profile.upMbps)
                if (profile.downMbps > 0) outbound.put("down_mbps", profile.downMbps)
            }
            "tuic" -> {
                outbound.put("type", "tuic")
                outbound.put("server", profile.address)
                outbound.put("server_port", profile.port)
                outbound.put("uuid", profile.uuid)
                outbound.put("password", profile.security)
                if (profile.sni.isNotEmpty()) {
                    outbound.put("tls", JSONObject().apply {
                        put("enabled", true)
                        put("server_name", profile.sni)
                        if (profile.alpn.isNotEmpty()) {
                            put("alpn", JSONArray().apply {
                                profile.alpn.split(",").forEach { put(it.trim()) }
                            })
                        }
                    })
                }
            }
            "wireguard" -> {
                outbound.put("type", "wireguard")
                outbound.put("server", profile.address)
                outbound.put("server_port", profile.port)
                outbound.put("private_key", profile.uuid)
                outbound.put("peer_public_key", profile.publicKey)
                if (profile.security.isNotEmpty()) {
                    outbound.put("pre_shared_key", profile.security)
                }
                outbound.put("local_address", JSONArray().apply {
                    put("10.0.0.2/32")
                })
            }
        }

        return outbound
    }
}
