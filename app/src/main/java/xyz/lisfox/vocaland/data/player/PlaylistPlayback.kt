package xyz.lisfox.vocaland.data.player

import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.songDisplayName

fun playlistTracks(detail: PlaylistDetailDto): List<BgmTrack> =
    detail.songs
        .mapNotNull { item ->
            val song = item.song ?: return@mapNotNull null
            val video = song.videos.firstOrNull {
                it.bvid.isNotBlank() && !it.disabled
            } ?: return@mapNotNull null
            BgmTrack(
                songId = song.id,
                bvid = video.bvid,
                title = songDisplayName(song),
                artist = song.artistLine(),
                cover = video.thumbnail?.takeIf { it.isNotBlank() } ?: song.coverUrl(),
                durationSeconds = video.duration ?: 0,
            )
        }
        .distinctBy { it.bvid }
