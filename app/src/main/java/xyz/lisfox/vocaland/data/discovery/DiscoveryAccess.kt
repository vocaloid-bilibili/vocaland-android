package xyz.lisfox.vocaland.data.discovery

private val ROLE_LEVEL = mapOf(
    "guest" to 0,
    "user" to 1,
    "sponsor" to 2,
    "worker" to 3,
    "admin" to 4,
    "superuser" to 5,
)

fun roleAtLeast(current: String?, required: String): Boolean {
    val currentLevel = ROLE_LEVEL[current ?: "guest"] ?: 0
    val requiredLevel = ROLE_LEVEL[required] ?: Int.MAX_VALUE
    return currentLevel >= requiredLevel
}
