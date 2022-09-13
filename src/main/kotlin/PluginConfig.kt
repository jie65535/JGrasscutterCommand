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
package top.jie65535.mirai

import net.mamoe.mirai.console.data.*

object PluginConfig : AutoSavePluginConfig("config") {
    @ValueDescription("管理员列表，仅管理员可以执行控制台命令")
    val administrators: MutableSet<Long> by value()

    @ValueDescription("用户黑名单")
    val blacklist: MutableSet<Long> by value()

    @ValueDescription("绑定命令：绑定 <UID> 示例：绑定 10001")
    var bindCommand: String by value("绑定")

    @ValueDescription("聊天中执行GC命令前缀：!<命令|别名>\n" +
            "示例1：!give 1096 lv90\n" +
            "示例2：!位置\n")
    var commandPrefix: String by value("!")

    @ValueDescription("命令别名")
    val commandAlias: MutableMap<String, String> by value(mutableMapOf(
        "无敌" to "prop god on",
        "关闭无敌" to "prop god off",
        "无限体力" to "prop ns on",
        "关闭无限体力" to "prop ns off",
        "无限能量" to "prop ue on",
        "关闭无限能量" to "prop ue off",
        "点亮地图" to "prop unlockmap 1",
        "解锁地图" to "prop unlockmap 1",
        "位置" to "pos",
        "坐标" to "pos",

        // TODO ...
    ))

    @ValueDescription("公开命令，无需绑定账号也可以执行（可用别名）（必须绑定了控制台令牌才可使用）")
    val publicCommands: MutableSet<String> by value(mutableSetOf(
        "list", "list uid"
    ))
}