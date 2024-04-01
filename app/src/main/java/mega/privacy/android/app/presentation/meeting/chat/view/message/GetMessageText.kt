package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.format.Format
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.format.FormatTag
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.format.FormatType
import mega.privacy.android.core.ui.controls.text.megaSpanStyle
import mega.privacy.android.core.ui.theme.robotoMono

/**
 * Get message text
 *
 * @param message
 * @param isEdited
 */
@Composable
internal fun getMessageText(
    message: String,
    isEdited: Boolean,
) = buildAnnotatedString {
    append(message.toFormattedText())
    if (isEdited) {
        append(" ")
        withStyle(
            style = megaSpanStyle(
                fontStyle = FontStyle.Italic,
                fontSize = 8.sp,
            )
        ) {
            append(stringResource(id = R.string.edited_message_text))
        }
    }
}

internal fun String.toFormattedText(): AnnotatedString {
    val text = this
    val formatList = buildList {
        text.forEachIndexed { index, char ->
            when (char) {
                FormatTag.Quote.tag -> {
                    if (text.getOrNull(index + 1) == FormatTag.Quote.tag
                        && text.getOrNull(index + 2) == FormatTag.Quote.tag
                    ) {
                        FormatType.MultiQuote
                    } else {
                        FormatType.Quote
                    }.let { formatType ->
                        text.getFormat(this, index, formatType)
                            ?.let { add(it) }
                    }
                }

                FormatTag.Bold.tag -> text.getFormat(this, index, FormatType.Bold)
                    ?.let { add(it) }

                FormatTag.Italic.tag -> text.getFormat(this, index, FormatType.Italic)
                    ?.let { add(it) }

                FormatTag.Strikethrough.tag -> text.getFormat(this, index, FormatType.Strikethrough)
                    ?.let { add(it) }
            }
        }
    }.compactInnerFormats()

    return buildAnnotatedString {
        if (formatList.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }

        if (formatList.first().formatStart != 0) {
            append(text.substring(0, formatList.first().formatStart))
        }

        formatList.forEachIndexed { index, format ->
            append(text.applyFormat(format = format))

            val isMultiQuote = format.type.first() == FormatType.MultiQuote
            val isEndOfText = format.formatEnd == text.length - (if (isMultiQuote) 3 else 1)
            val isStartOfNextFormat = index < formatList.size - 1
                    && format.formatEnd == formatList[index + 1].formatStart

            if (!isEndOfText && !isStartOfNextFormat) {
                // Append the text between the current format and the next one
                val startIndex =
                    if (isMultiQuote) format.formatEnd + 3
                    else format.formatEnd + 1
                val endIndex =
                    if (index == formatList.size - 1) text.length
                    else formatList[index + 1].formatStart
                append(text.substring(startIndex, endIndex))
            }
        }
    }
}

private fun List<Format>.compactInnerFormats(): List<Format> {
    var updatedList = findInnerFormat()
    var newUpdatedList = updatedList.findInnerFormat()

    while (updatedList.size < newUpdatedList.size) {
        updatedList = newUpdatedList
        newUpdatedList = newUpdatedList.findInnerFormat()
    }

    return updatedList
}

private fun List<Format>.findInnerFormat(): List<Format> {
    forEachIndexed { index, innerFormat ->
        runCatching {
            first { innerFormat.formatStart > it.sentenceStart && innerFormat.formatStart < it.sentenceEnd }
        }.onSuccess { parentFormat ->
            return if (innerFormat.formatStart > parentFormat.sentenceStart && innerFormat.formatEnd < parentFormat.sentenceEnd) {
                compactFormats(this, index, innerFormat, parentFormat)
            } else {
                this.toMutableList().also { it.removeAt(index) }
            }
        }
    }

    return this
}

private fun compactFormats(
    initialList: List<Format>,
    innerFormatIndex: Int,
    innerFormat: Format,
    parentFormat: Format,
): List<Format> {
    val updatedList = initialList.toMutableList()

    with(updatedList) {
        removeAt(innerFormatIndex)
        remove(parentFormat)
        if (innerFormat.formatStart > parentFormat.sentenceStart) {
            add(
                Format(
                    formatStart = parentFormat.formatStart,
                    formatEnd = innerFormat.formatStart,
                    type = parentFormat.type,
                    sentenceStart = parentFormat.sentenceStart,
                    sentenceEnd = innerFormat.formatStart,
                )
            )
        }
        add(
            Format(
                formatStart = innerFormat.formatStart,
                formatEnd = innerFormat.formatEnd,
                type = parentFormat.type.toMutableList().apply { addAll(innerFormat.type) },
                sentenceStart = innerFormat.formatStart + innerFormat.type.size,
                sentenceEnd = innerFormat.formatEnd - (innerFormat.type.size - 1),
            )
        )
        if (innerFormat.formatEnd < parentFormat.sentenceEnd) {
            add(
                Format(
                    formatStart = innerFormat.formatEnd,
                    formatEnd = parentFormat.formatEnd,
                    type = parentFormat.type,
                    sentenceStart = innerFormat.formatEnd + 1,
                    sentenceEnd = parentFormat.sentenceEnd
                ),
            )
        }
    }

    return updatedList.sortedBy { it.formatStart }
}

private fun String.getFormat(formatList: List<Format>, index: Int, formatType: FormatType) =
    when (formatType) {
        FormatType.Bold -> FormatTag.Bold.tag.toString()
        FormatType.Italic -> FormatTag.Italic.tag.toString()
        FormatType.Strikethrough -> FormatTag.Strikethrough.tag.toString()
        FormatType.Quote -> FormatTag.Quote.tag.toString()
        FormatType.MultiQuote -> FormatTag.Quote.tag.toString().repeat(3)
        FormatType.None -> null
    }?.let { tag ->
        if (formatList.isAlreadyPartOfAFormat(index)
            || (index > 0 && this[index - 1] != ' ' && this[index - 1] != '\n')
        ) {
            return@let null
        }

        val isMultiQuote = formatType == FormatType.MultiQuote
        val endIndex = getEndFormatIndex(tag, isMultiQuote, index) ?: return@let null

        val formatTypeList = buildList {
            add(formatType)

            if (!isMultiQuote) {
                var newStartIndex = index + 1
                var newEndIndex = endIndex - 1
                var newFormat = findExtraFormatTypes(this@getFormat, newStartIndex, newEndIndex)

                while (newFormat != null) {
                    add(newFormat)
                    newStartIndex += 1
                    newEndIndex -= 1
                    newFormat = findExtraFormatTypes(this@getFormat, newStartIndex, newEndIndex)
                }
            }
        }

        val indexAfterFormat = index + formatTypeList.size + if (isMultiQuote) 2 else 0
        val indexBeforeFormat = endIndex - formatTypeList.size

        if (this[indexAfterFormat] != ' ' && this[indexAfterFormat] != '\n'
            && this[indexBeforeFormat] != ' ' && this[indexBeforeFormat] != '\n'
        ) {
            Format(
                formatStart = index,
                formatEnd = endIndex,
                type = formatTypeList,
                sentenceStart = if (isMultiQuote) index + 3 else index + formatTypeList.size,
                sentenceEnd = if (isMultiQuote) endIndex else endIndex - (formatTypeList.size - 1),
            )
        } else {
            null
        }
    }

private fun String.getEndFormatIndex(tag: String, isMultiQuote: Boolean, startIndex: Int): Int? {
    val formatCharsSize = if (isMultiQuote) 3 else 1
    var endIndex = indexOf(tag, startIndex + formatCharsSize)
    var newStartIndex = -1

    while (endIndex != -1) {
        val breaksSimpleFormat = !isMultiQuote && substring(startIndex, endIndex).contains('\n')
        val isEndOfText = endIndex == this.length - formatCharsSize
        val indexBeforeFormat = endIndex - 1
        val indexAfterFormat = endIndex + formatCharsSize
        if (newStartIndex == -1 && this[indexBeforeFormat] == ' ' || this[indexBeforeFormat] == '\n') {
            newStartIndex = endIndex
        }

        if (!breaksSimpleFormat
            && this[indexBeforeFormat] != ' '
            && this[indexBeforeFormat] != '\n'
            && (isEndOfText
                    || this[indexAfterFormat] == ' '
                    || this[indexAfterFormat] == '\n')
        ) {
            if (newStartIndex == -1) {
                return endIndex
            } else {
                newStartIndex = -1
            }
        }

        endIndex = indexOf(tag, endIndex + formatCharsSize)
    }

    // No final format char found
    return null
}

private fun findExtraFormatTypes(
    text: String,
    startIndex: Int,
    endIndex: Int,
): FormatType? {
    val nextStartChar = text[startIndex]
    val previousEndChar = text[endIndex]

    return if (nextStartChar == previousEndChar
        && FormatTag.entries.any { it.tag == nextStartChar }
        && FormatTag.entries.any { it.tag == previousEndChar }
    ) {
        FormatTag.entries.find { it.tag == nextStartChar }?.type
    } else {
        null
    }
}

private fun List<Format>.isAlreadyPartOfAFormat(index: Int) =
    find { format ->
        with(format) {
            when {
                type.first() == FormatType.MultiQuote -> index in formatStart + 1 until formatEnd + 1
                type.size > 1 -> index in formatStart + 1 until formatStart + type.size
                        || index in formatEnd - (type.size - 1) until formatEnd + 1

                else -> formatEnd == index
            }
        }
    }?.let { true } ?: false

private fun String.applyFormat(format: Format) = buildAnnotatedString {
    with(format) {
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold.takeIf { type.contains(FormatType.Bold) },
                fontStyle = FontStyle.Italic.takeIf { type.contains(FormatType.Italic) },
                textDecoration = TextDecoration.LineThrough.takeIf { type.contains(FormatType.Strikethrough) },
                fontFamily = robotoMono.takeIf {
                    type.contains(FormatType.Quote) || type.contains(FormatType.MultiQuote)
                }
            )
        ) {
            val substring = substring(startIndex = sentenceStart, endIndex = sentenceEnd)
            append(substring)
        }
    }
}
