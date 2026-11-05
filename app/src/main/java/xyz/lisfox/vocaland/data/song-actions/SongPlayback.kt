package xyz.lisfox.vocaland.data.songactions

import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.VideoDto
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.songDisplayName

private val originalCopyrightValues = setOf(1, 100)

fun sortSongVideos(videos: List<VideoDto>): List<VideoDto> =
    videos.sortedWith(
        compareByDescending<VideoDto> { it.copyright in originalCopyrightValues }
            .thenByDescending { it.latestSnapshot?.view ?: 0L },
    )

fun songVideoTrack(
    song: SongDto,
    video: VideoDto,
): BgmTrack? {
    if (video.disabled || video.bvid.isBlank()) return null
    return BgmTrack(
        songId = song.id,
        bvid = video.bvid,
        title = songDisplayName(song),
        artist = song.artistLine(),
        cover = video.thumbnail?.takeIf { it.isNotBlank() } ?: song.coverUrl(),
        durationSeconds = video.duration ?: 0,
    )
}
