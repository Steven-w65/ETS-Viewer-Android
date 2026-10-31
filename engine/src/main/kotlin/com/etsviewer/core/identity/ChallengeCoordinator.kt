package com.etsviewer.core.identity

import com.etsviewer.core.service.string
import com.etsviewer.core.feedback.*
import kotlinx.serialization.json.*

data class ChallengeProof(val uid: String, val captchaResult: String)
sealed interface ChallengeEvent {
    data class Verified(val proof: ChallengeProof) : ChallengeEvent
    data class Rejected(val message: String, val expired: Boolean = false,
        val failure: AppFailure = AppFailure(FeedbackOperation.VERIFICATION)) : ChallengeEvent
}

object ChallengeCoordinator {
    // Public interactive ETS100 login page used by the Windows reference; no service proof is generated here.
    const val PAGE_URL = "https://pass.changyan.com/login?nextpage=aHR0cHM6Ly93d3cuZXRzMTAwLmNvbS9sb2dpbkNoZWNrLmh0bWw%3D&customConfig=e2hpZGRlbl9tb2R1bGU6ICJoZWFkZXIsdGFpbCxsb2dpbkJ5VmVyaWZ5Q29kZSxyZWdpc3Rlcixsb2dpbkJ5VGhpcmRMb2dpbiIscHJvZHVjdF9hcHBrZXk6InFpbmdkYW9fZXRzIiwibmVlZFRpY2tldCI6InRydWUiLCJsb2dpbl9ub3RBdXRvIjoidHJ1ZSJ9&from=ew&appId=pass6port18"
    fun expired(message: String) = Regex("过期|失效|超时|expired|timeout", RegexOption.IGNORE_CASE).containsMatchIn(message)
    fun decodeCheckLogin(text: String): ChallengeProof? = (decodeEvent(text) as? ChallengeEvent.Verified)?.proof
    fun decodeEvent(text: String): ChallengeEvent? = runCatching {
        val outer = Json.parseToJsonElement(text).jsonObject
        val transported = "status" in outer && "responseBody" in outer
        val response = if (transported) {
            val status = outer.string("status").toIntOrNull() ?: return null
            if (status != 200) return ChallengeEvent.Rejected("验证服务 HTTP $status，请重试",
                failure = AppFailure(FeedbackOperation.VERIFICATION, httpStatus = status.takeIf { it in 100..599 }))
            runCatching { Json.parseToJsonElement(outer.string("responseBody")).jsonObject }.getOrNull()
                ?: return ChallengeEvent.Rejected("验证服务响应无法解析，请重试")
        } else outer
        val code = (response["Code"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
            ?: return if (transported) ChallengeEvent.Rejected("验证服务返回无效结果，请重试") else null
        if (code != 0) {
            val message = listOf("Message", "Msg", "message", "msg", "ErrorMsg").firstNotNullOfOrNull { response.string(it).takeIf(String::isNotBlank) }
                ?: "验证失败 (Code $code)"
            return ChallengeEvent.Rejected(message, expired(message))
        }
        val data = runCatching { response["Data"].let { if (it is JsonPrimitive && it.isString) Json.parseToJsonElement(it.content) else it } }.getOrNull() as? JsonObject
            ?: return ChallengeEvent.Rejected("未获取到完整验证凭证，请重试")
        val uid = data.string("uid"); val proof = data.string("captchaResult")
        if (uid.isBlank() || proof.isBlank()) ChallengeEvent.Rejected("未获取到完整验证凭证，请重试")
        else ChallengeEvent.Verified(ChallengeProof(uid, proof))
    }.getOrNull()
}
