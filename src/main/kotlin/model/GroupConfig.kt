package top.jie65535.mirai.model

import kotlinx.serialization.Serializable

/**
 * 群类型
 */
@Serializable
data class Group(
    /**
     * 群ID（QQ群号）
     */
    val id: Long,

    /**
     * 服务器ID
     */
    var serverId: Int,

    /**
     * 是否启用（用于临时关闭）
     */
    var enabled: Boolean = true,
)