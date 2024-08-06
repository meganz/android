package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.format.Format
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.format.FormatTag
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.format.FormatType
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.controls.text.megaSpanStyle
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.robotoMono
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Message text view.
 * Display the message text with the correct format.
 * If the message is edited, it will display a text indicating that the message was edited.
 */
@Composable
fun MessageText(
    message: String,
    isEdited: Boolean,
    links: List<String>,
    interactionEnabled: Boolean,
    onLinkClicked: (String) -> String?,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    var clickedLink by rememberSaveable { mutableStateOf<String?>(null) }

    clickedLink?.let {
        OpenMessageLink(link = it)
        clickedLink = null
    }

    with(message.getMessageText(isEdited = isEdited, links = links)) {
        val onClick: (Int) -> Unit = {
            getStringAnnotations(URL_TAG, it, it).firstOrNull()
                ?.let { link -> clickedLink = onLinkClicked(link.item) }
        }
        val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
        val pressIndicator = Modifier.pointerInput(onClick) {
            detectTapGestures(
                onLongPress = { onLongClick() },
            ) { pos ->
                layoutResult.value?.let { layoutResult ->
                    onClick(layoutResult.getOffsetForPosition(pos))
                }
            }
        }

        Text(
            text = this,
            modifier = modifier.conditional(interactionEnabled) {
                then(pressIndicator)
            },
            style = style,
            onTextLayout = { layoutResult.value = it }
        )
    }
}

/**
 * Get message text
 *
 * @param isEdited
 */
@Composable
fun String.getMessageText(
    isEdited: Boolean,
) = buildAnnotatedString {
    append(this@getMessageText)
    getEditedIndicator(isEdited = isEdited)?.let { append(it) }
}


/**
 * Get message text
 *
 * @param isEdited
 */
@Composable
fun String.getMessageText(
    isEdited: Boolean,
    links: List<String> = emptyList(),
) = buildAnnotatedString {
    runCatching { toFormattedText(links) }
        .onSuccess { append(it) }
        .onFailure { append(this@getMessageText) }
    getEditedIndicator(isEdited = isEdited)?.let { append(it) }
}

@Composable
private fun getEditedIndicator(isEdited: Boolean): AnnotatedString? =
    if (isEdited) {
        buildAnnotatedString {
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
    } else {
        null
    }

/**
 * Apply the correct format to the text
 */
fun String.toFormattedText(
    links: List<String> = emptyList(),
): AnnotatedString {
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
        addAll(getFormatsFromLinks(links))
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
            val isOnlyLink = format.type.size == 1 && format.type.first() == FormatType.Link
            val isEndOfText = format.formatEnd == text.length - (when {
                isMultiQuote -> 3
                isOnlyLink -> 0
                else -> 1
            })
            val isStartOfNextFormat = index < formatList.size - 1
                    && format.formatEnd == formatList[index + 1].formatStart

            if (!isEndOfText && !isStartOfNextFormat) {
                // Append the text between the current format and the next one
                val startIndex = format.formatEnd + when {
                    isMultiQuote -> 3
                    isOnlyLink -> 0
                    else -> 1
                }
                val endIndex =
                    if (index == formatList.size - 1) text.length
                    else formatList[index + 1].formatStart
                append(text.substring(startIndex, endIndex))
            }
        }
    }
}

private fun List<Format>.compactInnerFormats(): List<Format> {
    if (size <= 1) return this

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
            first { innerFormat.formatStart >= it.sentenceStart && innerFormat.formatStart < it.sentenceEnd }
        }.onSuccess { parentFormat ->
            return if (innerFormat.formatStart >= parentFormat.sentenceStart && innerFormat.formatEnd <= parentFormat.sentenceEnd) {
                compactFormats(this, innerFormat, parentFormat)
            } else {
                this.toMutableList().also { it.removeAt(index) }
            }
        }
    }

    return this
}

private fun compactFormats(
    initialList: List<Format>,
    innerFormat: Format,
    parentFormat: Format,
): List<Format> {
    if (innerFormat == parentFormat) return initialList

    val isOnlyLink = innerFormat.type.size == 1 && innerFormat.type.first() == FormatType.Link

    with(initialList.toMutableList()) {
        remove(innerFormat)
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
                formatStart = if (isOnlyLink) parentFormat.formatStart else innerFormat.formatStart,
                formatEnd = innerFormat.formatEnd,
                type = parentFormat.type.toMutableList().apply { addAll(innerFormat.type) },
                sentenceStart =
                if (isOnlyLink) parentFormat.formatStart + parentFormat.type.size
                else innerFormat.formatStart + innerFormat.type.size,
                sentenceEnd =
                if (isOnlyLink) parentFormat.formatEnd - (parentFormat.type.size - 1)
                else innerFormat.formatEnd - (innerFormat.type.size - 1),
            )
        )
        if (innerFormat.formatEnd < parentFormat.sentenceEnd) {
            add(
                Format(
                    formatStart = innerFormat.formatEnd,
                    formatEnd = parentFormat.formatEnd,
                    type = parentFormat.type,
                    sentenceStart = (if (isOnlyLink) parentFormat.formatEnd else innerFormat.formatEnd) + 1,
                    sentenceEnd = parentFormat.sentenceEnd
                ),
            )
        }

        return sortedBy { it.formatStart }
    }
}

private fun String.getFormat(formatList: List<Format>, index: Int, formatType: FormatType) =
    when (formatType) {
        FormatType.Bold -> FormatTag.Bold.tag.toString()
        FormatType.Italic -> FormatTag.Italic.tag.toString()
        FormatType.Strikethrough -> FormatTag.Strikethrough.tag.toString()
        FormatType.Quote -> FormatTag.Quote.tag.toString()
        FormatType.MultiQuote -> FormatTag.Quote.tag.toString().repeat(3)
        FormatType.Link -> null
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

private fun String.getFormatsFromLinks(links: List<String>): List<Format> = buildList {
    links.forEach {
        val link = it.getLinkWithoutFormatTags()
        val startIndex = this@getFormatsFromLinks.indexOf(link)
        val endIndex = startIndex + link.length
        add(
            Format(
                formatStart = startIndex,
                formatEnd = endIndex,
                type = listOf(FormatType.Link),
                sentenceStart = startIndex,
                sentenceEnd = endIndex,
            )
        )
    }
}

private fun String.getLinkWithoutFormatTags(): String {
    var first = 0
    var last = length - 1

    while (FormatTag.entries.any { it.tag == this[first] }) {
        first += 1
    }

    while (FormatTag.entries.any { it.tag == this[last] }) {
        last -= 1
    }

    return substring(first, last + 1)
}

/**
 * Tries to open a link using the URI handler.
 * If the link cannot be opened, it will show a warning.
 */
@Composable
fun OpenMessageLink(link: String) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current

    runCatching {
        uriHandler.openUri(link.completeURLProtocol())
    }.onFailure {
        coroutineScope.launch {
            snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.chat_click_link_in_message_intent_not_available))
        }
    }
}

/**
 * Adds URL protocol if required
 */
private fun String.completeURLProtocol() =
    if (this.toUri().scheme.isNullOrBlank()) "http://$this"
    else this

private fun String.applyFormat(
    format: Format,
) = buildAnnotatedString {
    with(format) {
        val isLink = type.contains(FormatType.Link)
        if (isLink) {
            pushStringAnnotation(
                tag = URL_TAG,
                annotation = substring(sentenceStart, sentenceEnd)
            )
        }
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold.takeIf { type.contains(FormatType.Bold) },
                fontStyle = FontStyle.Italic.takeIf { type.contains(FormatType.Italic) },
                textDecoration = TextDecoration.LineThrough.takeIf { type.contains(FormatType.Strikethrough) }
                    ?: TextDecoration.Underline.takeIf { isLink },
                fontFamily = robotoMono.takeIf {
                    type.contains(FormatType.Quote) || type.contains(FormatType.MultiQuote)
                }
            )
        ) {
            val sentence = substring(sentenceStart, sentenceEnd)
            append(sentence)
        }
        if (isLink) {
            pop()
        }
    }
}

private const val URL_TAG = "URL"