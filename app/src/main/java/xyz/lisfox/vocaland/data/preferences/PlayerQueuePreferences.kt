package xyz.lisfox.vocaland.data.preferences

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.remote.AppJson

@Serializable
internal data class SavedPlayerQueue(
    val tracks: List<BgmTrack> = emptyList(),
    val currentBvid: String? = null,
    val currentIndex: Int = 0,
)

internal class PlayerQueuePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): SavedPlayerQueue? {
        val json = prefs.getString(KEY_QUEUE, null) ?: return null
        return runCatching { AppJson.decodeFromString<SavedPlayerQueue>(json) }.getOrNull()
    }

    fun save(tracks: List<BgmTrack>, currentBvid: String?, currentIndex: Int) {
        val payload = SavedPlayerQueue(
            tracks = tracks,
            currentBvid = currentBvid,
            currentIndex = currentIndex,
        )
        prefs.edit()
            .putString(KEY_QUEUE, AppJson.encodeToString(payload))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "vocaland_player"
        const val KEY_QUEUE = "queue"
    }
}
