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

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.contact.*
import top.jie65535.mirai.JGrasscutterCommand.reload
import top.jie65535.mirai.model.GroupConfig
import top.jie65535.mirai.model.Server
import top.jie65535.mirai.opencommand.OpenCommandApi

object PluginCommands : CompositeCommand(
    JGrasscutterCommand, "jgc",
    description = "管理插件设置"
) {
    private val logger = JGrasscutterCommand.logger


    @SubCommand
    @Description("插件命令用法")
    suspend fun CommandSender.help() {
        sendMessage(usage)
    }

    @SubCommand
    @Description("重载插件配置")
    suspend fun CommandSender.reload() {
        logger.info("重载插件配置")
        PluginConfig.reload()
        sendMessage("OK")
    }

    @SubCommand
    @Description("设置执行GC命令前缀")
    suspend fun CommandSender.setCommandPrefix(prefix: String) {
        if (prefix.isEmpty()) {
            sendMessage("前缀不能为空，这会导致每条消息都作为命令处理")
        } else {
            logger.info("设置执行GC命令前缀为 $prefix")
            PluginConfig.commandPrefix = prefix
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("设置绑定命令前缀")
    suspend fun CommandSender.setBindCommand(prefix: String) {
        if (prefix.isEmpty()) {
            sendMessage("不能为空")
        } else {
            logger.info("设置绑定命令为 $prefix")
            PluginConfig.bindCommand = prefix
            sendMessage("OK")
        }
    }

    @SubCommand("setAdmin", "op")
    @Description("设置管理员")
    suspend fun CommandSender.setAdmin(user: User) {
        logger.info("添加管理员 ${user.nameCardOrNick}(${user.id})")
        PluginConfig.administrators.add(user.id)
        sendMessage("OK")
    }

    @SubCommand("removeAdmin", "deop")
    @Description("解除管理员")
    suspend fun CommandSender.removeAdmin(user: User) {
        PluginConfig.administrators.remove(user.id)
        logger.info("解除管理员 ${user.nameCardOrNick}(${user.id})")
        sendMessage("OK")
    }

    @SubCommand
    @Description("禁止指定QQ使用插件")
    suspend fun CommandSender.ban(qq: Long) {
        logger.info("禁止${qq}使用插件")
        PluginConfig.blacklist.add(qq)
        sendMessage("OK")
    }

    @SubCommand
    @Description("解除禁止指定QQ使用插件")
    suspend fun CommandSender.unban(qq: Long) {
        logger.info("解除禁止${qq}使用插件")
        PluginConfig.blacklist.remove(qq)
        sendMessage("OK")
    }


    // region 服务器相关命令

    @SubCommand
    @Description("测试指定服务器是否安装插件")
    suspend fun CommandSender.ping(address: String) {
        val id = address.toIntOrNull()
        val serverAddress = if (id != null) {
            val server = PluginData.servers.find { it.id == id }
            if (server == null) {
                sendMessage("未找到指定服务器")
                return
            } else {
                server.address
            }
        } else {
            address
        }
        if (tryPing(serverAddress)) {
            sendMessage("OK")
        } else {
            sendMessage("Error")
        }
    }

    private suspend fun tryPing(address: String): Boolean {
        return try {
            logger.info("正在 ping $address")
            OpenCommandApi.ping(address)
            true
        } catch (e: Throwable) {
            logger.warning("ping $address 异常", e)
            false
        }
    }

    @SubCommand
    @Description("添加服务器")
    suspend fun CommandSender.addServer(address: String, name: String = "", vararg description: String = arrayOf()) {
        if (address.isEmpty()) {
            sendMessage("服务器地址不能为空！")
            return
        }
        val descriptionStr = description.joinToString(" ")

        if (tryPing(address)) {
            logger.info("添加服务器：$address\tname=$name\tdescription=$descriptionStr")
            val serverId = ++PluginData.lastServerId
            PluginData.servers.add(Server(serverId, address, name, descriptionStr))
            sendMessage("服务器已添加，ID为[$serverId]，使用servers子命令查看服务器列表")
        } else {
            sendMessage("只能设置装有 [OpenCommand](https://github.com/jie65535/gc-opencommand-plugin) 插件的服务器")
        }
    }

    @SubCommand
    @Description("列出服务器")
    suspend fun CommandSender.servers() {
        if (PluginData.servers.isEmpty()) {
            sendMessage("服务器列表为空，使用addServer子命令来添加服务器")
        } else {
            sendMessage(PluginData.servers.joinToString("\n") {
                if (it.description.isNotEmpty())
                    "[${it.id}] ${it.name} ${it.address}\n${it.description}"
                else
                    "[${it.id}] ${it.name} ${it.address}"
            })
        }
    }

    @SubCommand
    @Description("设置服务器启用")
    suspend fun CommandSender.setServerIsEnabled(id: Int, isEnabled: Boolean) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            logger.info("${if (isEnabled) "启用" else "禁用"}服务器[$id]")
            server.isEnabled = isEnabled
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("修改服务器地址")
    suspend fun CommandSender.setServerAddress(id: Int, address: String) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            if (tryPing(address)) {
                logger.info("修改服务器地址为：$address")
                server.address = address
                sendMessage("OK")
            } else {
                sendMessage("只能设置装有 [OpenCommand](https://github.com/jie65535/gc-opencommand-plugin) 插件的服务器")
            }
        }
    }

    @SubCommand
    @Description("设置服务器信息")
    suspend fun CommandSender.setServerInfo(id: Int, name: String, vararg description: String) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            val descriptionStr = description.joinToString(" ")
            logger.info("设置服务器信息为：name=$name\tdescription=$descriptionStr")
            server.name = name
            server.description = descriptionStr
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("设置服务器控制台令牌")
    suspend fun CommandSender.setServerConsoleToken(id: Int, consoleToken: String) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            logger.info("设置服务器控制台令牌为：$consoleToken")
            server.consoleToken = consoleToken
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("设置是否同步群消息到服务器")
    suspend fun CommandSender.setServerSyncMessage(id: Int, sync: Boolean) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            logger.info("服务器[$id]${if (sync) "启用" else "禁用"}消息同步")
            server.syncMessage = sync
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("设置默认服务器（初始值为首个创建的服务器）")
    suspend fun CommandSender.setDefaultServer(id: Int) {
        val server = PluginData.servers.find { it.id == id }
        if (server == null) {
            sendMessage("未找到指定服务器")
        } else {
            PluginConfig.defaultServerId = id
            logger.info("已将 [$id] ${server.name} 设置为默认服务器")
            sendMessage("OK")
        }
    }

    // endregion 服务器相关命令

    // region 群相关命令

    @SubCommand("linkGroup", "bindGroup", "addGroup")
    @Description("绑定服务器到群，若未指定服务器则使用默认服务器ID")
    suspend fun CommandSender.linkGroup(serverId: Int = PluginConfig.defaultServerId, group: Group? = getGroupOrNull()) {
        if (group == null) {
            sendMessage("必须指定群")
            return
        }

        val server = PluginData.servers.find { it.id == serverId }
        if (server == null) {
            sendMessage("指定服务器不存在，请先添加服务器(使用addServer子命令)")
            return
        }

        logger.info("将服务器[$serverId] ${server.name} ${server.address} 绑定到群 ${group.name}(${group.id})")
        val g = PluginData.groups.find { it.id == group.id }
        if (g == null) {
            PluginData.groups.add(GroupConfig(group.id, serverId))
            sendMessage("OK，默认已启用")
        } else {
            g.serverId = serverId
            sendMessage("OK")
        }
    }

    @SubCommand
    @Description("启用指定群执行，若未绑定，则自动绑定到默认服务器")
    suspend fun CommandSender.enable(group: Group? = getGroupOrNull()) {
        if (group == null) {
            sendMessage("必须指定群")
        } else {
            val g = PluginData.groups.find { it.id == group.id }
            if (g == null) {
                // 当启用的群是未初始化的群时，使用默认服务器进行初始化
                val server = PluginData.servers.find { it.id == PluginConfig.defaultServerId }
                if (server != null) {
                    // 绑定到默认服务器
                    logger.info("将默认服务器[${server.id}] ${server.name} ${server.address} 绑定到群 ${group.name}(${group.id})")
                    PluginData.groups.add(GroupConfig(group.id, server.id))
                    sendMessage("OK，已绑定到默认服务器")
                } else {
                    sendMessage("请先绑定群到服务器(使用linkGroup子命令)")
                }
            } else {
                logger.info("启用插件在群 ${group.name}(${group.id})")
                g.isEnabled = true
                sendMessage("OK")
            }
        }
    }

    @SubCommand
    @Description("禁用指定群执行")
    suspend fun CommandSender.disable(group: Group? = getGroupOrNull()) {
        if (group == null) {
            sendMessage("必须指定群")
        } else {
            val g = PluginData.groups.find { it.id == group.id }
            if (g == null) {
                sendMessage("请先绑定群到服务器(使用linkGroup子命令)")
            } else {
                logger.info("禁用插件在群 ${group.name}(${group.id})")
                g.isEnabled = false
                sendMessage("OK")
            }
        }
    }

    // endregion 群相关命令

    // region 命令别名部分

    @SubCommand
    @Description("列出所有别名")
    suspend fun CommandSender.listCommands() {
        sendMessage(PluginConfig.commandAlias.map { "[${it.key}] ${it.value}" }.joinToString())
    }

    @SubCommand
    @Description("添加命令别名，多条命令用|隔开")
    suspend fun CommandSender.setCommand(alias: String, vararg command: String) {
        if (alias.isEmpty() || command.isEmpty() || command[0].isEmpty()) {
            sendMessage("参数不能为空")
        }
        PluginConfig.commandAlias[alias] = command.joinToString(" ")
        sendMessage("OK")
    }

    @SubCommand
    @Description("删除命令别名")
    suspend fun CommandSender.removeCommand(alias: String) {
        PluginConfig.commandAlias.remove(alias)
        sendMessage("OK")
    }

    @SubCommand
    @Description("添加公开命令（游客可执行）（可用别名）")
    suspend fun CommandSender.addPublicCommand(command: String) {
        PluginConfig.publicCommands.add(command)
        sendMessage("OK")
    }
    @SubCommand
    @Description("删除公开命令")
    suspend fun CommandSender.removePublicCommand(alias: String) {
        PluginConfig.publicCommands.remove(alias)
        sendMessage("OK")
    }

    // endregion
}