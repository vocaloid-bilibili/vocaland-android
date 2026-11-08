package xyz.lisfox.vocaland.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import xyz.lisfox.vocaland.data.community.CommentImageRefDto
import xyz.lisfox.vocaland.data.community.CommentMentionDto
import xyz.lisfox.vocaland.util.CommentToken
import xyz.lisfox.vocaland.util.SiteTarget
import xyz.lisfox.vocaland.util.parseSitePath
import xyz.lisfox.vocaland.util.resolveCommentLinkHref
import xyz.lisfox.vocaland.util.toHttps
import xyz.lisfox.vocaland.util.tokenizeCommentContent

/**
 * 评论正文渲染：只处理网页版支持的四种 token（插图、封面引用、站内链接、@提及），
 * 其余 Markdown 语法保持原样当文本显示。
 *
 * 图片按块渲染，因此 token 先按图片切成「行内段 / 图片」交替的块；
 * 行内段统一用 AnnotatedString 拼接，保证换行与文字排版连续。
 */
@Composable
internal fun CommentContent(
    content: String,
    mentions: List<CommentMentionDto>,
    onOpenUser: ((Int) -> Unit)?,
    onOpenTarget: ((SiteTarget) -> Unit)?,
    resolveImageRef: suspend (String) -> CommentImageRefDto?,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(content, mentions) { tokenizeCommentContent(content, mentions) }
    val blocks = remember(tokens) { splitIntoBlocks(tokens) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is CommentBlock.Inline -> InlineCommentText(
                    tokens = block.tokens,
                    onOpenUser = onOpenUser,
                    onOpenTarget = onOpenTarget,
                )

                is CommentBlock.Image -> CommentImage(
                    url = block.url,
                    alt = block.alt,
                )

                is CommentBlock.ImageRef -> CommentImageRef(
                    query = block.query,
                    alt = block.alt,
                    resolveImageRef = resolveImageRef,
                )
            }
        }
    }
}

private sealed interface CommentBlock {
    data class Inline(val tokens: List<CommentToken>) : CommentBlock

    data class Image(val alt: String, val url: String) : CommentBlock

    data class ImageRef(val query: String, val alt: String) : CommentBlock
}

private fun splitIntoBlocks(tokens: List<CommentToken>): List<CommentBlock> {
    val blocks = mutableListOf<CommentBlock>()
    val inline = mutableListOf<CommentToken>()

    fun flushInline() {
        if (inline.isNotEmpty()) {
            blocks += CommentBlock.Inline(inline.toList())
            inline.clear()
        }
    }

    tokens.forEach { token ->
        when (token) {
            is CommentToken.Image -> {
                flushInline()
                blocks += CommentBlock.Image(alt = token.alt, url = token.url)
            }

            is CommentToken.ImageRef -> {
                flushInline()
                blocks += CommentBlock.ImageRef(query = token.query, alt = token.alt)
            }

            else -> inline += token
        }
    }
    flushInline()
    return blocks
}

@Composable
private fun InlineCommentText(
    tokens: List<CommentToken>,
    onOpenUser: ((Int) -> Unit)?,
    onOpenTarget: ((SiteTarget) -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val text = remember(tokens, scheme.primary, onOpenUser, onOpenTarget) {
        buildAnnotatedString {
            tokens.forEach { token ->
                when (token) {
                    is CommentToken.Text -> append(token.text)

                    is CommentToken.Mention -> {
                        val mention = token.mention
                        val label = mentionLabel(mention)
                        val start = length
                        withStyle(linkStyle(scheme.primary)) { append(label) }
                        if (mention.kind == "user" && mention.userId > 0 && onOpenUser != null) {
                            addLink(
                                LinkAnnotation.Clickable(
                                    tag = "mention-${mention.userId}",
                                    linkInteractionListener = { onOpenUser(mention.userId) },
                                ),
                                start = start,
                                end = length,
                            )
                        }
                    }

                    is CommentToken.InternalLink -> {
                        val target = parseSitePath(resolveCommentLinkHref(token.path))
                        val label = token.label.trimStart('/').ifEmpty { token.label }
                        val start = length
                        withStyle(linkStyle(scheme.primary)) { append(label) }
                        if (target != null && onOpenTarget != null) {
                            addLink(
                                LinkAnnotation.Clickable(
                                    tag = "link-$target",
                                    linkInteractionListener = { onOpenTarget(target) },
                                ),
                                start = start,
                                end = length,
                            )
                        }
                    }

                    // 图片已在上层拆成独立块，这里不会出现
                    is CommentToken.Image, is CommentToken.ImageRef -> Unit
                }
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurface,
    )
}

private fun mentionLabel(mention: CommentMentionDto): String {
    val display = mention.display.trim()
    return when {
        display.isEmpty() -> "@用户"
        display.startsWith("@") -> display
        else -> "@$display"
    }
}

private fun linkStyle(primary: Color): SpanStyle =
    SpanStyle(color = primary, fontWeight = FontWeight.Medium)

@Composable
private fun CommentImage(
    url: String,
    alt: String,
    modifier: Modifier = Modifier,
) {
    val resolved = toHttps(url)
    if (resolved.isNullOrBlank()) {
        CommentFallbackChip("图片不可用")
        return
    }

    var failed by remember(resolved) { mutableStateOf(false) }
    if (failed) {
        CommentFallbackChip("图片不可用")
        return
    }

    AsyncImage(
        model = resolved,
        contentDescription = alt.ifBlank { "评论图片" },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentScale = ContentScale.Fit,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) {
                failed = true
            }
        },
    )
}

@Composable
private fun CommentImageRef(
    query: String,
    alt: String,
    resolveImageRef: suspend (String) -> CommentImageRefDto?,
) {
    var state by remember(query) { mutableStateOf<ImageRefState>(ImageRefState.Loading) }

    LaunchedEffect(query) {
        state = if (query.isBlank()) {
            ImageRefState.Failed
        } else {
            resolveImageRef(query)
                ?.takeIf { it.url.isNotBlank() }
                ?.let(ImageRefState::Loaded)
                ?: ImageRefState.Failed
        }
    }

    when (val current = state) {
        ImageRefState.Loading -> CommentFallbackChip("封面加载中…")
        ImageRefState.Failed -> CommentFallbackChip("封面不可用")
        is ImageRefState.Loaded -> CommentImage(
            url = current.data.url,
            alt = current.data.alt.ifBlank { alt },
        )
    }
}

private sealed interface ImageRefState {
    data object Loading : ImageRefState

    data object Failed : ImageRefState

    data class Loaded(val data: CommentImageRefDto) : ImageRefState
}

@Composable
private fun CommentFallbackChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
