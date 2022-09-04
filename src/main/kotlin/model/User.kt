/*
 * JGrasscutterCommand
 * Copyright (C) 2022 jie65535
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.jie65535.mirai.model

import kotlinx.serialization.Serializable
import top.jie65535.mirai.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class User(
    /**
     * 用户ID（QQ帐号）
     */
    val id: Long,

    /**
     * 服务器ID
     */
    val serverId: Int,

    /**
     * 游戏UID
     */
    var uid: Int,
) {
    /**
     * 令牌，失效时清空
     */
    var token: String = ""

    /**
     * 用户添加时间
     */
    @Serializable(LocalDateTimeSerializer::class)
    val createTime: LocalDateTime = LocalDateTime.now()

    /**
     * 运行命令计数
     */
    var runCount: Int = 0

    /**
     * 最后运行时间
     */
    @Serializable(LocalDateTimeSerializer::class)
    var lastRunTime: LocalDateTime? = null
}