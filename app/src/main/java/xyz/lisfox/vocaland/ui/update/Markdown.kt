package xyz.lisfox.vocaland.ui.update

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            MarkdownBlockView(
                block = block,
                linkColor = scheme.primary,
                codeBackground = scheme.surfaceContainerHighest,
            )
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Ordered(val number: String, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val language: String?, val code: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    linkColor: Color,
    codeBackground: Color,
) {
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = inlineMarkdown(block.text, linkColor, codeBackground),
            style = when (block.level) {
                1 -> MaterialTheme.typography.titleMedium
                2 -> MaterialTheme.typography.titleSmall
                else -> MaterialTheme.typography.labelLarge
            },
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        is MarkdownBlock.Paragraph -> Text(
            text = inlineMarkdown(block.text, linkColor, codeBackground),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        is MarkdownBlock.Bullet -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "•",
                modifier = Modifier.width(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = linkColor,
            )
            Text(
                text = inlineMarkdown(block.text, linkColor, codeBackground),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is MarkdownBlock.Ordered -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "${block.number}.",
                modifier = Modifier.width(26.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = linkColor,
                textAlign = TextAlign.End,
            )
            Text(
                text = inlineMarkdown(block.text, linkColor, codeBackground),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is MarkdownBlock.Quote -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = inlineMarkdown(block.text, linkColor, codeBackground),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        is MarkdownBlock.Code -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = codeBackground,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                block.language?.let { language ->
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = block.code,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }

        MarkdownBlock.Rule -> HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

private fun inlineMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", index + 2)
                if (end > index) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(index + 2, end))
                    }
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }

            text.startsWith("~~", index) -> {
                val end = text.indexOf("~~", index + 2)
                if (end > index) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(index + 2, end))
                    }
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }

            text[index] == '`' -> {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        ),
                    ) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }

            text.startsWith("![", index) -> {
                val link = parseLink(text, index + 1)
                if (link != null) {
                    append(link.label)
                    index = link.end
                } else {
                    append(text[index])
                    index++
                }
            }

            text[index] == '[' -> {
                val link = parseLink(text, index)
                if (link != null) {
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(link.label)
                    }
                    index = link.end
                } else {
                    append(text[index])
                    index++
                }
            }

            text[index] == '*' -> {
                val end = text.indexOf('*', index + 1)
                if (end > index + 1) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }

            else -> {
                append(text[index])
                index++
            }
        }
    }
}

private data class InlineLink(
    val label: String,
    val end: Int,
)

private fun parseLink(text: String, start: Int): InlineLink? {
    if (text.getOrNull(start) != '[') return null
    val labelEnd = text.indexOf(']', start + 1)
    if (labelEnd < 0 || text.getOrNull(labelEnd + 1) != '(') return null
    val urlEnd = text.indexOf(')', labelEnd + 2)
    if (urlEnd < 0) return null
    return InlineLink(
        label = text.substring(start + 1, labelEnd),
        end = urlEnd + 1,
    )
}

private val orderedPattern = Regex("""^(\d+)[.)]\s+(.*)$""")

private fun parseMarkdown(source: String): List<MarkdownBlock> {
    val lines = source
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index].trim()
        when {
            line.isEmpty() -> index++

            line.startsWith("```") -> {
                val language = line.removePrefix("```").trim().ifBlank { null }
                val code = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code += lines[index]
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(language, code.joinToString("\n"))
            }

            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
                blocks += MarkdownBlock.Heading(level, line.drop(level).trim())
                index++
            }

            line == "---" || line == "***" || line == "___" -> {
                blocks += MarkdownBlock.Rule
                index++
            }

            line.startsWith(">") -> {
                val quote = mutableListOf<String>()
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    quote += lines[index].trim().removePrefix(">").trimStart()
                    index++
                }
                blocks += MarkdownBlock.Quote(quote.joinToString(" "))
            }

            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                blocks += MarkdownBlock.Bullet(line.drop(2).trim())
                index++
            }

            else -> {
                val ordered = orderedPattern.matchEntire(line)
                if (ordered != null) {
                    blocks += MarkdownBlock.Ordered(
                        number = ordered.groupValues[1],
                        text = ordered.groupValues[2],
                    )
                    index++
                } else {
                    val paragraph = mutableListOf<String>()
                    while (index < lines.size) {
                        val current = lines[index].trim()
                        if (current.isEmpty() || isBlockStart(current)) break
                        paragraph += current
                        index++
                    }
                    if (paragraph.isEmpty()) {
                        index++
                    } else {
                        blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
                    }
                }
            }
        }
    }
    return blocks
}

private fun isBlockStart(line: String): Boolean =
    line.startsWith("```") ||
        line.startsWith("#") ||
        line.startsWith(">") ||
        line.startsWith("- ") ||
        line.startsWith("* ") ||
        line.startsWith("+ ") ||
        line == "---" ||
        line == "***" ||
        line == "___" ||
        orderedPattern.matches(line)
