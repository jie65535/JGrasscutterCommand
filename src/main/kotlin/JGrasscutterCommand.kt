package top.jie65535.mirai

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object JGrasscutterCommand : KotlinPlugin(
    JvmPluginDescription(
        id = "top.jie65535.mirai.grasscutter-command",
        name = "J Grasscutter Command",
        version = "0.1.0",
    ) {
        author("jie65535")
        info("""聊天执行GC命令""")
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
    }
}