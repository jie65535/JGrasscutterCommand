package top.jie65535.mirai

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val id: Int,
    var address: String,
    var consoleToken: String,
)