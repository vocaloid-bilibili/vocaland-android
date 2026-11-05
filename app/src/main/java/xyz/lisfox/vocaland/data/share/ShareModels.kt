package xyz.lisfox.vocaland.data.share

/** 卡片 CSS/DP 尺寸（与网站 540×960 对齐）。 */
const val SHARE_CARD_WIDTH_DP = 540
const val SHARE_CARD_HEIGHT_DP = 960

/** 2 倍导出尺寸。 */
const val SHARE_CARD_EXPORT_SCALE = 2
const val SHARE_CARD_WIDTH_PX = SHARE_CARD_WIDTH_DP * SHARE_CARD_EXPORT_SCALE
const val SHARE_CARD_HEIGHT_PX = SHARE_CARD_HEIGHT_DP * SHARE_CARD_EXPORT_SCALE
const val SHARE_CARD_ASPECT_RATIO = SHARE_CARD_WIDTH_DP.toFloat() / SHARE_CARD_HEIGHT_DP

/** 条目区：歌曲 8 行 × 6 列，艺人 5 行 × 6 列。 */
const val SHARE_CARD_GRID_COLUMNS = 6
const val SHARE_CARD_SONG_COUNT = 48
const val SHARE_CARD_ARTIST_COUNT = 30

data class ShareCardStat(
    val label: String,
    val value: String,
)

data class ShareCardSong(
    val title: String,
    val subtitle: String = "",
    val thumbnail: String? = null,
    val badge: String? = null,
    val lit: Boolean = true,
)

/**
 * 收藏册分享卡的展示数据。
 *
 * 收藏册页面无需把自身模型带进来：按此结构映射即可（歌曲标题 / 副标题 / 封面）。
 * `progress` 为空时不画进度条。
 */
data class CollectionShareCardData(
    val title: String,
    val subtitle: String = "",
    val ownerName: String? = null,
    val ownerAvatar: String? = null,
    val stats: List<ShareCardStat> = emptyList(),
    val progress: Float? = null,
    val squareThumbnails: Boolean = false,
    val songs: List<ShareCardSong> = emptyList(),
    val footer: String = "",
    val watermark: String = "VocaLand",
)
