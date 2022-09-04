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

/**
 * 服务器类型
 */
@Serializable
data class Server(
    /**
     * 服务器ID
     * 自动递增
     */
    val id: Int,

    /**
     * 服务器地址
     */
    var address: String,

    /**
     * 服务器名称
     */
    var name: String = "",

    /**
     * 服务器说明
     */
    var description: String = "",

    /**
     * 控制台令牌
     */
    var consoleToken: String = "",

    /**
     * 服务器是否已启用
     */
    var isEnabled: Boolean = true,

    /**
     * 同步群消息到服务器，必须设置了控制台令牌
     */
    var syncMessage: Boolean = false,
)