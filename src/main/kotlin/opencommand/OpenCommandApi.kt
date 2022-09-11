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
package top.jie65535.mirai.opencommand

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import top.jie65535.mirai.utils.UnsafeOkHttpClient

@OptIn(ExperimentalSerializationApi::class)
object OpenCommandApi {
    private val httpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().build()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    private data class PingRequest(
        val action: String = "ping"
    )
    @Serializable
    private data class SendCodeRequest(
        @SerialName("data")
        val uid: Int,
        val action: String = "sendCode"
    )
    @Serializable
    private data class VerifyRequest(
        val token: String,
        @SerialName("data")
        val code: Int,
        val action: String = "verify"
    )
    @Serializable
    private data class CommandRequest(
        val token: String,
        @SerialName("data")
        val command: String,
        val action: String = "command",
    )


    @Serializable
    private data class StringResponse(
        val retcode: Int,
        val message: String,
        val data: String?
    )

    class HttpException(val code: Int, message: String) : Exception(message)
    class InvokeException(val code: Int, message: String) : Exception(message)

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * 向服务器发起opencommand请求
     * @param host 服务器地址
     * @param jsonBody 请求正文（Json）
     * @return 如果一切正常，返回相应数据，可能为空
     * @exception InvokeException HTTP请求执行成功，但opencommand插件调用失败
     * @exception HttpException HTTP请求完成，但HTTP响应错误代码（例如404、500等）
     * @exception IOException 如果由于取消、连接问题或超时而无法执行请求。由于网络可能在交换期间发生故障，因此远程服务器可能在故障之前接受了请求
     */
    private suspend fun doRequest(host: String, jsonBody: String): String? {
        val api = "$host/opencommand/api"
        val request = Request.Builder()
            .url(api)
            .post(jsonBody.toRequestBody(JSON))
            .build()
//        JGrasscutterCommand.logger.debug("POST to $api Body $jsonBody")
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { httpResponse ->
                val responseBody = httpResponse.body
                if (httpResponse.code == 200 && responseBody != null) {
                    val response = json.decodeFromStream<StringResponse>(responseBody.byteStream())
                    if (response.retcode != 200) {
                        throw InvokeException(response.retcode, response.data ?: response.message)
                    } else {
                        return@use response.data
                    }
                } else {
                    throw HttpException(httpResponse.code, httpResponse.message)
                }
            }
        }
    }

    /**
     * 测试链接
     * @param host 服务器地址
     */
    suspend fun ping(host: String) {
        doRequest(host, json.encodeToString(PingRequest()))
    }

    /**
     * 发送验证码
     * @param host 服务器地址
     * @param uid 目标玩家UID
     * @return 返回临时Token用于验证
     */
    suspend fun sendCode(host: String, uid: Int): String {
        return doRequest(host, json.encodeToString(SendCodeRequest(uid)))!!
    }

    /**
     * 验证身份，验证失败时将抛出异常，异常详情参考doRequest描述
     * @param host 服务器地址
     * @param token 发送验证码时返回的临时令牌
     * @param code 用户输入的验证代码
     * @see doRequest
     */
    suspend fun verify(host: String, token: String, code: Int) {
        doRequest(host, json.encodeToString(VerifyRequest(token, code)))
    }

    /**
     * 运行命令，成功时返回命令执行结果，失败时抛出异常，异常详情参考doRequest描述
     * @param host 服务器地址
     * @param token 持久令牌
     * @param command 命令行
     * @return 命令执行结果
     * @see doRequest
     */
    suspend fun runCommand(host: String, token: String, command: String): String? {
        return doRequest(host, json.encodeToString(CommandRequest(token, command)))
    }
}