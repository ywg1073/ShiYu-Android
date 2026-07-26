package com.example.shiyu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (markdown.isBlank()) return

    val lines = markdown.lines()
    val blocks = rememberMarkdownBlocks(lines)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Quote -> {
                    QuoteBlockCard(text = block.content)
                }
                is MarkdownBlock.Header1 -> {
                    Text(
                        text = parseInlineMarkdown(block.content),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                is MarkdownBlock.Header2 -> {
                    Text(
                        text = parseInlineMarkdown(block.content),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.Header3 -> {
                    Text(
                        text = parseInlineMarkdown(block.content),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 4 + 4).dp, top = 1.dp, bottom = 1.dp)
                    ) {
                        Text(
                            text = "• ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = parseInlineMarkdown(block.content),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = textColor
                            )
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp)
                    ) {
                        Text(
                            text = "${block.num}. ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = parseInlineMarkdown(block.content),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = textColor
                            )
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.content),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textColor
                        )
                    )
                }
                is MarkdownBlock.Blank -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun QuoteBlockCard(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parseInlineMarkdown(text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private sealed class MarkdownBlock {
    data class Header1(val content: String) : MarkdownBlock()
    data class Header2(val content: String) : MarkdownBlock()
    data class Header3(val content: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    data class ListItem(val indent: Int, val content: String) : MarkdownBlock()
    data class NumberedItem(val num: String, val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
    object Blank : MarkdownBlock()
}

private fun rememberMarkdownBlocks(lines: List<String>): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val quoteBuffer = mutableListOf<String>()

    fun flushQuote() {
        if (quoteBuffer.isNotEmpty()) {
            result.add(MarkdownBlock.Quote(quoteBuffer.joinToString("\n")))
            quoteBuffer.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith(">")) {
            quoteBuffer.add(trimmed.removePrefix(">").trim())
            continue
        } else {
            flushQuote()
        }

        if (trimmed.isBlank()) {
            result.add(MarkdownBlock.Blank)
            continue
        }

        if (trimmed.startsWith("# ")) {
            result.add(MarkdownBlock.Header1(trimmed.removePrefix("# ").trim()))
        } else if (trimmed.startsWith("## ")) {
            result.add(MarkdownBlock.Header2(trimmed.removePrefix("## ").trim()))
        } else if (trimmed.startsWith("### ")) {
            result.add(MarkdownBlock.Header3(trimmed.removePrefix("### ").trim()))
        } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
            val indent = line.takeWhile { it.isWhitespace() }.length
            result.add(MarkdownBlock.ListItem(indent, trimmed.substring(2).trim()))
        } else if (trimmed.matches(Regex("^\\d+\\.\\s+.*"))) {
            val numStr = trimmed.substringBefore(".")
            val rest = trimmed.substringAfter(".").trim()
            result.add(MarkdownBlock.NumberedItem(numStr, rest))
        } else {
            result.add(MarkdownBlock.Paragraph(trimmed))
        }
    }
    flushQuote()
    return result
}

/**
 * Inline markdown parser supporting **bold**, `code`, and normal text
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            val boldStart = text.indexOf("**", cursor)
            val codeStart = text.indexOf("`", cursor)

            val nextPos = listOf(boldStart, codeStart)
                .filter { it != -1 }
                .minOrNull() ?: -1

            if (nextPos == -1) {
                append(text.substring(cursor))
                break
            }

            if (nextPos > cursor) {
                append(text.substring(cursor, nextPos))
                cursor = nextPos
            }

            if (nextPos == boldStart) {
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    cursor = boldEnd + 2
                } else {
                    append("**")
                    cursor = boldStart + 2
                }
            } else if (nextPos == codeStart) {
                val codeEnd = text.indexOf("`", codeStart + 1)
                if (codeEnd != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    ) {
                        append(text.substring(codeStart + 1, codeEnd))
                    }
                    cursor = codeEnd + 1
                } else {
                    append("`")
                    cursor = codeStart + 1
                }
            }
        }
    }
}
