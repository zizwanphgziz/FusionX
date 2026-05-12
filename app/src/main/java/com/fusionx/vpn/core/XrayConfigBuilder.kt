package com.fusionx.vpn.core

import com.fusionx.vpn.model.ServerProfile
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun buildConfig(profile: ServerProfile, socksPort: Int = 10808, httpPort: Int = 10809): String {
        val config = JSONObject()

        // Log
        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // Inbounds
        val inbounds = JSONArray()
        inbounds.put(JSONObject().apply {
            put("tag", "socks")
            put("port", socksPort)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("destOverride", JSONArray().apply {
                    put("http")
                    put("tls")
                    put("quic")
                })
                put("routeOnly", true)
            })
        })
        inbounds.put(JSONObject().apply {
            put("tag", "http")
            put("port", httpPort)
            put("listen", "127.0.0.1")
            put("protocol", "http")
        })
        config.put("inbounds", inbounds)

        // Outbounds
        val outbounds = JSONArray()
        outbounds.put(buildOutbound(profile))
        outbounds.put(JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
        })
        outbounds.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
        })
        config.put("outbounds", outbounds)

        // Routing
        config.put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("domain", JSONArray().put("geosite:private"))
                })
                put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray().apply {
                        put("geoip:private")
                        put("geoip:cn")
                    })
                })
            })
        })

        // DNS
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put("1.1.1.1")
                put("8.8.8.8")
            })
        })

        // Stats
        config.put("stats", JSONObject())
        config.put("policy", JSONObject().apply {
            put("system", JSONObject().apply {
                put("statsOutboundUplink", true)
                put("statsOutboundDownlink", true)
            })
        })

        return config.toString(2)
    }

    private fun buildOutbound(profile: ServerProfile): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")

        when (profile.protocol.lowercase()) {
            "vless" -> buildVlessOutbound(outbound, profile)
            "vmess" -> buildVmessOutbound(outbound, profile)
            "trojan" -> buildTrojanOutbound(outbound, profile)
            "ss", "shadowsocks" -> buildShadowsocksOutbound(outbound, profile)
            "ssr" -> buildSsrOutbound(outbound, profile)
        }

        return outbound
    }

    private fun buildVlessOutbound(outbound: JSONObject, profile: ServerProfile) {
        outbound.put("protocol", "vless")

        val vnext = JSONObject().apply {
            put("address", profile.address)
            put("port", profile.port)
            put("users", JSONArray().put(JSONObject().apply {
                put("id", profile.uuid)
                put("encryption", "none")
                put("flow", profile.flow)
            }))
        }

        outbound.put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(vnext))
        })

        outbound.put("streamSettings", buildStreamSettings(profile))
    }

    private fun buildVmessOutbound(outbound: JSONObject, profile: ServerProfile) {
        outbound.put("protocol", "vmess")

        val vnext = JSONObject().apply {
            put("address", profile.address)
            put("port", profile.port)
            put("users", JSONArray().put(JSONObject().apply {
                put("id", profile.uuid)
                put("alterId", profile.alterId)
                put("security", profile.security.ifEmpty { "auto" })
            }))
        }

        outbound.put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(vnext))
        })

        outbound.put("streamSettings", buildStreamSettings(profile))
    }

    private fun buildTrojanOutbound(outbound: JSONObject, profile: ServerProfile) {
        outbound.put("protocol", "trojan")

        val server = JSONObject().apply {
            put("address", profile.address)
            put("port", profile.port)
            put("password", profile.uuid)
        }

        outbound.put("settings", JSONObject().apply {
            put("servers", JSONArray().put(server))
        })

        outbound.put("streamSettings", buildStreamSettings(profile))
    }

    private fun buildShadowsocksOutbound(outbound: JSONObject, profile: ServerProfile) {
        outbound.put("protocol", "shadowsocks")

        val server = JSONObject().apply {
            put("address", profile.address)
            put("port", profile.port)
            put("method", profile.security)
            put("password", profile.uuid)
        }

        outbound.put("settings", JSONObject().apply {
            put("servers", JSONArray().put(server))
        })
    }

    private fun buildSsrOutbound(outbound: JSONObject, profile: ServerProfile) {
        outbound.put("protocol", "shadowsocks")

        val server = JSONObject().apply {
            put("address", profile.address)
            put("port", profile.port)
            put("method", profile.security)
            put("password", profile.uuid)
        }

        outbound.put("settings", JSONObject().apply {
            put("servers", JSONArray().put(server))
        })
    }

    private fun buildStreamSettings(profile: ServerProfile): JSONObject {
        val stream = JSONObject()

        val network = if (profile.isStrx) "ws" else profile.network.ifEmpty { "tcp" }
        stream.put("network", network)

        // TLS settings
        when (profile.tls.lowercase()) {
            "tls" -> {
                stream.put("security", "tls")
                stream.put("tlsSettings", JSONObject().apply {
                    if (profile.sni.isNotEmpty()) put("serverName", profile.sni)
                    if (profile.fingerprint.isNotEmpty()) put("fingerprint", profile.fingerprint)
                    if (profile.alpn.isNotEmpty()) {
                        put("alpn", JSONArray().apply {
                            profile.alpn.split(",").forEach { put(it.trim()) }
                        })
                    }
                    put("allowInsecure", false)
                })
            }
            "reality" -> {
                stream.put("security", "reality")
                stream.put("realitySettings", JSONObject().apply {
                    if (profile.sni.isNotEmpty()) put("serverName", profile.sni)
                    if (profile.fingerprint.isNotEmpty()) put("fingerprint", profile.fingerprint)
                    if (profile.publicKey.isNotEmpty()) put("publicKey", profile.publicKey)
                    if (profile.shortId.isNotEmpty()) put("shortId", profile.shortId)
                    if (profile.spiderX.isNotEmpty()) put("spiderX", profile.spiderX)
                })
            }
            else -> stream.put("security", "none")
        }

        // Transport settings
        when (network) {
            "ws" -> {
                stream.put("wsSettings", JSONObject().apply {
                    val wsPath = if (profile.isStrx) {
                        profile.path
                    } else {
                        profile.path.ifEmpty { "/" }
                    }
                    put("path", wsPath)
                    put("headers", JSONObject().apply {
                        val wsHost = if (profile.isStrx) {
                            profile.host.removePrefix("strx-payload://")
                        } else {
                            profile.host
                        }
                        if (wsHost.isNotEmpty()) put("Host", wsHost)
                    })
                })
            }
            "grpc" -> {
                stream.put("grpcSettings", JSONObject().apply {
                    put("serviceName", profile.path)
                    put("multiMode", false)
                })
            }
            "h2", "http" -> {
                stream.put("httpSettings", JSONObject().apply {
                    put("path", profile.path.ifEmpty { "/" })
                    if (profile.host.isNotEmpty()) {
                        put("host", JSONArray().apply {
                            profile.host.split(",").forEach { put(it.trim()) }
                        })
                    }
                })
            }
            "tcp" -> {
                if (profile.headerType == "http") {
                    stream.put("tcpSettings", JSONObject().apply {
                        put("header", JSONObject().apply {
                            put("type", "http")
                            put("request", JSONObject().apply {
                                put("path", JSONArray().put(profile.path.ifEmpty { "/" }))
                                if (profile.host.isNotEmpty()) {
                                    put("headers", JSONObject().apply {
                                        put("Host", JSONArray().put(profile.host))
                                    })
                                }
                            })
                        })
                    })
                }
            }
        }

        return stream
    }
}
