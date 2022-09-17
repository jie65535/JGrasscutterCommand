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

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.firstIsInstance
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import top.jie65535.mirai.model.User
import top.jie65535.mirai.opencommand.OpenCommandApi
import java.time.LocalDateTime

object JGrasscutterCommand : KotlinPlugin(
    JvmPluginDescription(
        id = "top.jie65535.mirai.grasscutter-command",
        name = "J Grasscutter Command",
        version = "0.3.0",
    ) {
        author("jie65535")
        info("""聊天执行GC命令""")
    }
) {
    override fun onEnable() {
        PluginConfig.reload()
        PluginData.reload()
        PluginCommands.register()

        val eventChannel = GlobalEventChannel.parentScope(this)
        // 监听群消息
        eventChannel.subscribeAlways<MessageEvent> {
            // 忽略被拉黑的用户发送的消息
            if (PluginConfig.blacklist.contains(sender.id))
                return@subscribeAlways

            val server = if (this is GroupMessageEvent) {
                // 若为群消息，忽略未启用的群
                val groupConfig = PluginData.groups.find { it.id == group.id }
                if (groupConfig == null || !groupConfig.isEnabled)
                    return@subscribeAlways
                // 获取群绑定的服务器
                PluginData.servers.find { it.id == groupConfig.serverId }
            } else {
                // 否则为私聊消息，使用默认服务器
                PluginData.servers.find { it.id == PluginConfig.defaultServerId }
            }

            // 忽略未启用的服务器
            if (server == null || !server.isEnabled)
                return@subscribeAlways

            // 解析消息
            val message = this.message.joinToString("") {
                if (it is At) {
                    // 替换@群员为@其绑定的Uid
                    val user = PluginData.users.find { user -> user.id == it.target && user.serverId == server.id }
                    if (user == null) { it.contentToString() } else { "@${user.uid}" }
                } else {
                    it.contentToString()
                }
            }

            // 处理绑定命令
            if (message.startsWith(PluginConfig.bindCommand)) {
                val input = message.removePrefix(PluginConfig.bindCommand).trim()
                val bindUid = input.toIntOrNull()
                if (bindUid == null) {
                    this.subject.sendMessage(this.message.quote() + "请输入正确的UID")
                    return@subscribeAlways
                }
                logger.info("绑定用户 ${sender.nameCardOrNick}(${sender.id}) Uid $bindUid 到服务器 ${server.name} ${server.address}")
                var user = PluginData.users.find { it.id == sender.id && it.serverId == server.id }
                if (user == null) {
                    user = User(sender.id, server.id, bindUid)
                    PluginData.users.add(user)
                } else {
                    if (user.uid != bindUid) {
                        user.uid = bindUid
                        // 更换绑定将重置token
                        user.token = ""
                    }
                }
                // 发送验证码，并提示要求回复验证码
                sendCode(server.address, user)
            }
            // 处理执行游戏命令
            else if (message.startsWith(PluginConfig.commandPrefix)) {
                var command = message.removePrefix(PluginConfig.commandPrefix).trim()
                if (command.isEmpty() || (command[0] == '/' && command.length == 1)) {
                    return@subscribeAlways
                }

                // 执行的用户
                var user: User? = null
                // 获取token
                // 若设置了控制台令牌，则验证是否为管理员执行
                val token = if (server.consoleToken.isNotEmpty() && PluginConfig.administrators.contains(sender.id)) {
                    logger.info("管理员 ${sender.nameCardOrNick}(${sender.id}) 执行命令：$command")
                    // 设置控制台令牌
                    server.consoleToken
                } else {
                    // 普通用户
                    user = PluginData.users.find { it.id == sender.id && it.serverId == server.id }
                    if (user == null || user.token.isEmpty()) {
                        if (server.consoleToken.isNotEmpty() && PluginConfig.publicCommands.contains(command)) {
                            logger.info("游客用户 ${sender.nameCardOrNick}(${sender.id}) 执行公开命令：$command")
                            server.consoleToken
                        } else {
                            return@subscribeAlways
                        }
                    } else {
                        logger.info("用户 ${sender.nameCardOrNick}(${sender.id}) 执行命令：$command")
                        // 使用用户缓存令牌
                        user.token
                    }
                }
                // 检查是否使用别名
                val t = PluginConfig.commandAlias[command]
                if (!t.isNullOrEmpty()) command = t
                // 如果是斜杠开头，则移除斜杠，在控制台执行不需要斜杠
                if (command[0] == '/') command = command.substring(1)

                try {
                    // 调用接口执行命令
                    val response = OpenCommandApi.runCommand(server.address, token, command)
                    if (response.isNullOrEmpty()) {
                        subject.sendMessage(this.message.quote() + "OK")
                    } else {
                        subject.sendMessage(this.message.quote() + response)
                    }
                    if (user != null) {
                        // 计数并更新最后运行时间
                        ++user.runCount
                        user.lastRunTime = LocalDateTime.now()
                    }
                } catch (e: OpenCommandApi.InvokeException) {
                    when (e.code) {
                        404 -> {
                            subject.sendMessage(this.message.quote() + "玩家不存在或未上线")
                        }
                        403 -> {
                            logger.warning("${sender.nameCardOrNick}(${sender.id}) 的命令执行失败，服务器已收到命令，但不做处理，可能是未验证通过")
                            // 403不理会用户
                        }
                        401 -> {
                            logger.warning("${sender.nameCardOrNick}(${sender.id}) 的命令执行失败，未授权或已过期的令牌，可以修改插件配置以延长令牌过期时间")
                            subject.sendMessage(this.message.quote() + "令牌未授权或已过期，请重新绑定账号以更新令牌")
                            // TODO 此处可以重新发送验证码要求验证，但目前直接报错并要求重新绑定
                        }
                        500 -> {
                            logger.warning("${sender.nameCardOrNick}(${sender.id}) 的命令执行失败，服务器内部错误：${e.message}")
                            subject.sendMessage(this.message.quote() + "服务器内部发生错误，命令执行失败")
                        }
                        else -> {
                            logger.warning("${sender.nameCardOrNick}(${sender.id}) 的命令执行失败，发生预期外异常：${e.message}")
                        }
                    }
                } catch (e: Throwable) {
                    logger.warning("${sender.nameCardOrNick}(${sender.id}) 在执行命令时发生异常", e)
                }
            }
            // 否则如果启用了同步消息，且控制台令牌不为空，且为群消息时
            else if (server.consoleToken.isNotEmpty() && server.syncMessage && this is GroupMessageEvent) {
                try {
                    OpenCommandApi.runCommand(
                        server.address,
                        server.consoleToken,
                        "say <color=green>${sender.nameCardOrNick}</color>:\n${this.message.contentToString()}")
                } catch (e: Throwable) {
                    server.syncMessage = false
                    logger.warning("同步发送聊天消息失败，自动禁用同步消息，请手动重新启用", e)
                }
            }
        }

        logger.info("Plugin loaded")
    }

    /**
     * 发送验证码，并监听用户回复
     * @param host 服务器地址
     * @param user 用户实例
     */
    private suspend fun MessageEvent.sendCode(host: String, user: User) {
        try {
            logger.info("${sender.nameCardOrNick}(${sender.id}) 正在请求向服务器[${user.serverId}] @${user.uid} 发送验证码")
            // 请求发送验证码
            user.token = OpenCommandApi.sendCode(host, user.uid)
            // 提示用户
            subject.sendMessage(message.quote() + "验证码已发送，请在一分钟内将游戏中收到的验证码发送以验证身份。")

            // 最多等待1分钟
            withTimeout(60 * 1000) {
                // 当验证失败时循环重试，最多重试3次
                var retry = 3
                while (isActive && retry --> 0) {
                    // 监听消息事件
                    val nextEvent = GlobalEventChannel.nextEvent<MessageEvent>(priority = EventPriority.HIGH) { event ->
                        // 仅监听该用户的消息，并且消息内容为4位数字
                        event.sender.id == user.id && event.message.firstIsInstanceOrNull<PlainText>()
                            ?.content?.trim()
                            ?.toIntOrNull()
                            ?.let { it in 1000..9999 } == true
                    }

                    // 得到消息中的4位数字代码
                    val code = nextEvent.message.firstIsInstance<PlainText>().content.trim().toInt()
                    try {
                        // 请求验证
                        OpenCommandApi.verify(host, user.token, code)
                        logger.info("${nextEvent.sender.nameCardOrNick}(${nextEvent.sender.id}) 在服务器[${user.serverId}]验证通过，其游戏Uid为 ${user.uid}")
                        // 若无异常则验证通过
                        nextEvent.subject.sendMessage(nextEvent.message.quote() + "验证通过")
                        // 停止监听
                        return@withTimeout
                    } catch (e: OpenCommandApi.InvokeException) {
                        logger.warning("${nextEvent.sender.nameCardOrNick}(${nextEvent.sender.id}) 在服务器[${user.serverId}] @${user.uid} 验证失败，信息：${e.message}")
                        // 400为验证失败
                        if (e.code == 400) {
                            nextEvent.subject.sendMessage(nextEvent.message.quote() + "验证失败，请重试")
                        } else {
                            nextEvent.subject.sendMessage(nextEvent.message.quote() + e.message!!)
                        }
                        // 不返回，继续监听
                    } catch (e: Throwable) {
                        // 预期外异常，停止监听器
                        logger.warning("${nextEvent.sender.nameCardOrNick}(${nextEvent.sender.id}) 在向服务器[${user.serverId}] @${user.uid} 发起验证时发生了预期外异常", e)
                        user.token = ""
                        nextEvent.subject.sendMessage(nextEvent.message.quote() + "发生内部错误，已取消验证，请重试")
                        return@withTimeout
                    }
                }
            }
        } catch (e: OpenCommandApi.InvokeException) {
            subject.sendMessage(message.quote() +
                    when (e.code) {
                        404 ->  "目标玩家不存在或不在线"
                        403 -> "请求太频繁，请稍后再试"
                        else -> e.message!!
                    })
        } catch (e: TimeoutCancellationException) {
            subject.sendMessage(message.quote() + "等待验证超时，请重试")
        } catch (e: Throwable) {
            logger.warning("发送验证码出现预期外的异常", e)
            if (e.message != null) subject.sendMessage(message.quote() + e.message!!)
        }
    }
}