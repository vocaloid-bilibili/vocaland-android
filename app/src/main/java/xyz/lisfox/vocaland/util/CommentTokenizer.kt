package xyz.lisfox.vocaland.util

import xyz.lisfox.vocaland.data.community.CommentMentionDto

/**
 * 评论正文 token。与网页版 `packages/shared/src/comment/tokenizer.ts` 逐条对应，
 * 只认这四种语法，其余 Markdown（粗体、斜体、标题、代码等）一律按纯文本处理。
 *
 * 支持的类型（优先级从上到下，同一起点只取第一个命中的）：
 *   ![alt](url)          插图
 *   [[image:歌名|alt]]   封面引用（image / img / 封面 三个前缀等价）
 *   [[path|label]]       站内链接
 *   @某人                由服务端 mentions 的 start/end 定位
 */
sealed interface CommentToken {
    data class Text(val text: String) : CommentToken

    data class Mention(val mention: CommentMentionDto) : CommentToken

    data class Image(val alt: String, val url: String) : CommentToken

    data class ImageRef(val query: String, val alt: String) : CommentToken

    data class InternalLink(val path: String, val label: String) : CommentToken
}

private data class ParsedToken<T : CommentToken>(
    val token: T,
    val length: Int,
)

private val IMAGE_REF_NAMESPACES = setOf("image", "img", "封面")
private val ED_LINK_PATTERN = Regex("^ed:(\\d+)$", RegexOption.IGNORE_CASE)

fun tokenizeCommentContent(
    content: String,
    mentions: List<CommentMentionDto> = emptyList(),
): List<CommentToken> {
    val tokens = mutableListOf<CommentToken>()
    val sortedMentions = mentions.sortedBy { it.start }
    var mentionIdx = 0
    var cursor = 0
    var textStart = 0

    fun flushText(end: Int) {
        if (end > textStart) {
            tokens += CommentToken.Text(content.substring(textStart, end))
        }
    }

    while (cursor < content.length) {
        while (mentionIdx < sortedMentions.size && sortedMentions[mentionIdx].end <= cursor) {
            mentionIdx += 1
        }

        val mention = sortedMentions.getOrNull(mentionIdx)
        if (mention != null && mention.start == cursor && mention.end > mention.start) {
            flushText(cursor)
            tokens += CommentToken.Mention(mention)
            cursor = mention.end
            textStart = cursor
            mentionIdx += 1
            continue
        }

        val image = parseImageToken(content, cursor)
        if (image != null) {
            flushText(cursor)
            tokens += image.token
            cursor += image.length
            textStart = cursor
            continue
        }

        val imageRef = parseImageRefToken(content, cursor)
        if (imageRef != null) {
            flushText(cursor)
            tokens += imageRef.token
            cursor += imageRef.length
            textStart = cursor
            continue
        }

        val internalLink = parseInternalLinkToken(content, cursor)
        if (internalLink != null) {
            flushText(cursor)
            tokens += internalLink.token
            cursor += internalLink.length
            textStart = cursor
            continue
        }

        cursor += 1
    }

    flushText(content.length)
    return tokens
}

/**
 * 站内链接的 path → 站内地址，与网页版 `resolveInternalLinkHref` 相同：
 * `ed:123` 展开成 `/ed/campaign/123`，其余没有前导 `/` 的补上。
 */
fun resolveCommentLinkHref(path: String): String {
    val normalized = path.trim()
    ED_LINK_PATTERN.find(normalized)?.let { match ->
        return "/ed/campaign/${match.groupValues[1]}"
    }

    if (normalized == "interaction:pickup") return "/interaction/pickup"
    if (normalized == "interaction:backfill") return "/interaction/backfill"

    return if (normalized.startsWith("/")) normalized else "/$normalized"
}

private fun parseImageToken(content: String, start: Int): ParsedToken<CommentToken.Image>? {
    if (start + 1 >= content.length) return null
    if (content[start] != '!' || content[start + 1] != '[') return null

    val altEnd = content.indexOf(']', start + 2)
    if (altEnd == -1 || altEnd + 1 >= content.length || content[altEnd + 1] != '(') return null

    val urlEnd = content.indexOf(')', altEnd + 2)
    if (urlEnd == -1) return null

    val alt = content.substring(start + 2, altEnd)
    val url = content.substring(altEnd + 2, urlEnd)

    return ParsedToken(CommentToken.Image(alt, url), urlEnd - start + 1)
}

private fun parseImageRefToken(content: String, start: Int): ParsedToken<CommentToken.ImageRef>? {
    if (start + 1 >= content.length) return null
    if (content[start] != '[' || content[start + 1] != '[') return null

    val close = content.indexOf("]]", start + 2)
    if (close == -1) return null

    val inner = content.substring(start + 2, close)
    val pipeIdx = inner.indexOf('|')
    val refPart = (if (pipeIdx >= 0) inner.substring(0, pipeIdx) else inner).trim()
    val altPart = if (pipeIdx >= 0) inner.substring(pipeIdx + 1).trim() else ""

    val colonIdx = refPart.indexOf(':')
    if (colonIdx == -1) return null

    val namespace = refPart.substring(0, colonIdx).trim().lowercase()
    if (namespace !in IMAGE_REF_NAMESPACES) return null

    val query = refPart.substring(colonIdx + 1).trim()

    return ParsedToken(
        CommentToken.ImageRef(
            query = query,
            alt = altPart.ifEmpty { query.ifEmpty { "封面" } },
        ),
        close - start + 2,
    )
}

private fun parseInternalLinkToken(
    content: String,
    start: Int,
): ParsedToken<CommentToken.InternalLink>? {
    if (start + 1 >= content.length) return null
    if (content[start] != '[' || content[start + 1] != '[') return null

    val close = content.indexOf("]]", start + 2)
    if (close == -1) return null

    val inner = content.substring(start + 2, close)
    val pipeIdx = inner.indexOf('|')
    val path = (if (pipeIdx >= 0) inner.substring(0, pipeIdx) else inner).trim()
    val rawLabel = (if (pipeIdx >= 0) inner.substring(pipeIdx + 1) else inner).trim()
    val label = rawLabel.ifEmpty { path }

    return ParsedToken(CommentToken.InternalLink(path, label), close - start + 2)
}
