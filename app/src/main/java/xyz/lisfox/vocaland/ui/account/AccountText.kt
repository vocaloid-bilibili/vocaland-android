package xyz.lisfox.vocaland.ui.account

import xyz.lisfox.vocaland.data.shop.CheckinResultDto

fun roleLabel(role: String): String = when (role) {
    "guest" -> "游客"
    "user" -> "用户"
    "sponsor" -> "赞助者"
    "worker" -> "工作人员"
    "admin" -> "管理员"
    "superuser" -> "超级管理员"
    else -> role
}

fun describeCheckin(result: CheckinResultDto): String {
    if (result.already) return "今天已经签过了"
    val parts = mutableListOf("签到成功 +${result.expGained} 经验")
    if (result.streakBonus > 0) parts += "连签奖励 +${result.streakBonus}"
    if (result.streakBroken) parts += "连签已中断"
    if (result.ticketGranted) parts += "获得补签卡 ×1"
    return parts.joinToString("，")
}
