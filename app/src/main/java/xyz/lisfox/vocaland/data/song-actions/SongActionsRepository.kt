package xyz.lisfox.vocaland.data.songactions

class SongActionsRepository(
    private val api: SongActionsApi,
) {
    suspend fun loadListenState(songId: Int): Result<SongListenStateDto> = runCatching {
        api.songListenState(songId)
    }

    suspend fun loadLitCount(songId: Int): Result<Int> = runCatching {
        api.litCounts(
            entityType = "song",
            ids = songId.toString(),
        ).data
            .firstOrNull { it.entityId == songId }
            ?.litCount
            ?: 0
    }

    suspend fun setListenStatus(
        songId: Int,
        status: SongListenStatus,
    ): Result<SongListenStateDto> = runCatching {
        api.setSongListenStatus(
            songId = songId,
            request = SetSongListenStatusRequest(status.apiValue),
        )
    }

    suspend fun clearListenStatus(songId: Int): Result<SongListenStateDto> = runCatching {
        api.clearSongListenStatus(songId)
    }
}
