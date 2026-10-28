package com.etsviewer.core.accounts

import com.etsviewer.core.service.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class LearnerAccount(
    val key: String, val id: String, val parentId: String, val name: String,
    val status: String = "", val outOfDate: String = "", val classId: String = "",
    val mobileStatus: String = "", val grade: String = "", val className: String = "",
    val userAccountId: String = "", val machineCodeStatus: String = "",
) {
    val eligible: Boolean get() = status == "0" && outOfDate == "0" && classId.isNotEmpty() && mobileStatus == "1"
}

object LearnerAccountSource {
    fun decode(reply: EtsReply): List<LearnerAccount> {
        reply.requireSuccess()
        return reply.body.entries.mapNotNull { (key, value) ->
            val a = value as? JsonObject ?: return@mapNotNull null
            if (a.isEmpty()) return@mapNotNull null
            LearnerAccount(key, a.string("id"), a.string("parent_id"), a.string("name"),
                a.string("status"), a.string("out_of_date"), a.string("class_id"), a.string("mobile_status"),
                a.string("grade"), a.string("class_name"), a.string("user_account_id"), a.string("machine_code_status"))
        }
    }
    fun preferred(accounts: List<LearnerAccount>): List<LearnerAccount> {
        val usable = accounts.filter { it.parentId.isNotBlank() }
        return usable.filter { it.eligible }.ifEmpty {
            usable.filter { it.status.isBlank() && it.outOfDate.isBlank() && it.classId.isBlank() && it.mobileStatus.isBlank() }.take(1)
        }
    }
}
