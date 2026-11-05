package xyz.lisfox.vocaland.data.songactions

enum class SongListenStatus(
    val apiValue: String,
    val label: String,
) {
    Want("want", "想听"),
    Listened("listened", "听过"),
    ;

    companion object {
        fun fromApiValue(value: String?): SongListenStatus? =
            entries.firstOrNull { it.apiValue == value }
    }
}
