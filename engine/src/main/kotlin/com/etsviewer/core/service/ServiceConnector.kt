package com.etsviewer.core.service

import com.etsviewer.core.accounts.*
import com.etsviewer.core.homework.*
import kotlinx.serialization.json.*

interface CloudApi {
    suspend fun login(phone: String, password: String, deviceCode: String): EtsReply
    suspend fun bindDevice(phone: String, password: String, deviceCode: String): EtsReply
    suspend fun renew(token: String): EtsReply
    suspend fun continueLogin(uid: String, captchaResult: String, deviceCode: String): EtsReply
    suspend fun accounts(token: String): List<LearnerAccount>
    suspend fun homework(token: String, parentId: String, period: HomeworkPeriod): HomeworkBatch
}

class ServiceConnector(private val transport: CloudTransport, private val serial: String = "test") : CloudApi {
    private fun JsonObjectBuilder.common(version: String = "3", globalVersion: String = "5.4.5") {
        put("version", version); put("system", "4"); put("global_client_version", globalVersion); put("sign_response", 1)
    }
    override suspend fun login(phone: String, password: String, deviceCode: String) =
        transport.post("user/login", buildJsonObject {
            put("sn", serial); put("phone", phone); put("password", password); put("device_code", deviceCode)
            put("device_name", "DESKTOP"); put("version", "3"); put("local_ip", "127.0.0.1")
            put("system", "4"); put("global_client_version", "5.4.5"); put("sign_response", 1)
        })
    override suspend fun bindDevice(phone: String, password: String, deviceCode: String) =
        transport.post("user/rebind-code", buildJsonObject {
            put("sn", serial); put("phone", phone); put("email", ""); put("password", password)
            put("code", "0"); put("version", "2"); put("device_name", "1337"); put("device_code", deviceCode)
            put("local_ip", "127.0.0.1"); put("system", "4"); put("global_client_version", "5.4.5"); put("sign_response", 1)
        })
    override suspend fun renew(token: String) = transport.post("user/rebind-code", buildJsonObject {
        put("token", token); put("version", "2"); put("system", "4")
    })
    override suspend fun continueLogin(uid: String, captchaResult: String, deviceCode: String) =
        transport.post("user/login-zt", buildJsonObject {
            put("sn", serial); put("login", uid); put("user_id", uid); put("suc_user_token", captchaResult)
            put("device_code", deviceCode); put("device_name", "Android"); put("suc_device_name", "Other"); put("os_type", "android")
            put("system", "4"); put("global_client_version", ""); put("sign_response", 1)
        })
    override suspend fun accounts(token: String) = LearnerAccountSource.decode(transport.post("m/ecard/list", buildJsonObject {
        put("sn", serial); put("token", token); common()
    }))
    override suspend fun homework(token: String, parentId: String, period: HomeworkPeriod) = HomeworkSource.decode(transport.post("g/homework/list", buildJsonObject {
        put("sn", serial); put("token", token); put("parent_account_id", parentId); put("limit", "0"); put("status", period.wireValue); put("offset", "0")
        put("max_end_time", ""); put("max_homework_id", ""); put("min_end_time", ""); put("min_homework_id", "")
        put("get_to_do_count", 1); put("show_old_homework", 1); put("parent_homework_id", ""); put("get_all_count", 1); put("check_pass", 1)
        put("get_to_overtime_count", 1); common()
    }))
}
