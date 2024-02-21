package mega.privacy.android.core.ui.controls.text

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * High light text from mega format
 * [A]Billed monthly[/A] %s/month
 * [A]Google Pay[/A] (subscription)
 *
 * @param value
 * @param baseStyle the style apply for all text
 * @param styles the list of the tag and the custom style
 * key is [SpanIndicator] for open and close tags, value is the [SpanStyle]
 * @param modifier
 * @param color the color apply for all text
 * @param maxLines Maximum number of lines for the text to span
 * @param overflow How visual overflow should be handled
 * @param textAlign to align text
 */
@Composable
fun MegaSpannedText(
    value: String,
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, MegaSpanStyle>,
    color: TextColor,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        text = spannedText(value, styles),
        color = MegaTheme.textColor(textColor = color),
        style = baseStyle,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

/**
 * High light text from mega format and receive clicks on spanned text
 * Check our [A]terms & conditions[/A]
 *
 * @param value
 * @param baseStyle the style apply for all text
 * @param styles the list of the tag and the custom style
 * key is [SpanIndicator] for open and close tags, value is the [SpanStyleWithAnnotation]
 * with the style and the annotation, we will receive clicks for not null annotations
 * @param onAnnotationClick will receive clicks on spanned text with not null annotation
 * @param modifier
 */
@Composable
fun MegaSpannedClickableText(
    value: String,
    styles: Map<SpanIndicator, MegaSpanStyleWithAnnotation>,
    color: TextColor,
    onAnnotationClick: (annotation: String) -> Unit,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = LocalTextStyle.current,
) {
    val annotatedLinkString = spannedTextWithAnnotation(value, styles)
    ClickableText(
        modifier = modifier,
        text = annotatedLinkString,
        style = megaTextStyle(baseStyle = baseStyle, color = color),
        onClick = { position ->
            annotatedLinkString.getStringAnnotations(ANNOTATION_TAG, position, position + 1)
                .firstOrNull()?.let { onAnnotationClick(it.item) }
        }
    )
}

@Composable
private fun spannedText(value: String, styles: Map<SpanIndicator, MegaSpanStyle>) =
    spannedTextWithAnnotation(value, styles.mapValues {
        MegaSpanStyleWithAnnotation(it.value, null)
    })

@OptIn(ExperimentalTextApi::class)
@Composable
private fun spannedTextWithAnnotation(
    value: String,
    styles: Map<SpanIndicator, MegaSpanStyleWithAnnotation>,
) =
    buildAnnotatedString {
        var temp = value
        while (temp.isNotEmpty()) {
            val nextTag = styles.keys.mapNotNull { tag ->
                val start = temp.indexOf(tag.openTag)
                if (start >= 0) start to tag else null
            }.minByOrNull { it.first }

            if (nextTag != null) {
                val (start, tag) = nextTag
                val end = temp.indexOf(tag.closeTag, startIndex = start)

                if (start > 0) {
                    append(temp.substring(0, start))
                }

                if (start in 0 until end) {
                    val contentStart = start + tag.openTag.length
                    if (contentStart < end) {
                        val spanStyleWithAnnotation = styles[tag]
                        val spanStyle = spanStyleWithAnnotation?.megaSpanStyle?.toSpanStyle()
                        spanStyleWithAnnotation?.annotation?.let { annotation ->
                            spanStyle?.let {
                                withAnnotation(ANNOTATION_TAG, annotation) {
                                    withStyle(spanStyle) {
                                        append(temp.substring(contentStart, end))
                                    }
                                }
                            }
                        } ?: run {
                            spanStyle?.let { style ->
                                withStyle(style) {
                                    append(temp.substring(contentStart, end))
                                }
                            }
                        }
                    }
                }

                val index = end + tag.closeTag.length
                temp = if (index <= temp.length) {
                    temp.substring(index)
                } else {
                    ""
                }
            } else {
                append(temp)
                temp = ""
            }
        }
    }

private const val ANNOTATION_TAG = "annotationTag"

/**
 * High light text preview one
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewOne() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaSpannedText(
            value = "[A]Google Pay[/A] (subscription)",
            baseStyle = MaterialTheme.typography.subtitle1,
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyle(color = TextColor.Error)
            ),
            color = TextColor.Placeholder
        )
    }
}

/**
 * High light text preview two
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewTwo() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaSpannedText(
            value = "Do you want to [A]Google Pay[/A] (subscription)",
            baseStyle = MaterialTheme.typography.subtitle1,
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyle(color = TextColor.Error)
            ),
            color = TextColor.Primary
        )
    }
}

/**
 * High light text preview three
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewThree() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaSpannedText(
            value = "Do you want to [A]Google Pay[/A] [B]Huawei[/B] (subscription)",
            baseStyle = MaterialTheme.typography.subtitle1,
            styles = hashMapOf(
                SpanIndicator('B') to MegaSpanStyle(color = TextColor.Accent),
                SpanIndicator('A') to MegaSpanStyle(color = TextColor.Error)
            ),
            color = TextColor.Secondary
        )
    }
}

/**
 * High light text with base colour preview
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewFour() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaSpannedText(
            value = "Choose [A]Google Pay[/A] (subscription)",
            baseStyle = MaterialTheme.typography.subtitle1,
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyle(color = TextColor.Error)
            ),
            color = TextColor.Placeholder
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MegaSpannedAlignedTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaSpannedText(
            value = "Do you want to [A]Google Pay[/A] [B]Huawei[/B]\n (subscription)",
            baseStyle = MaterialTheme.typography.subtitle1,
            styles = hashMapOf(
                SpanIndicator('B') to MegaSpanStyle(color = TextColor.Accent),
                SpanIndicator('A') to MegaSpanStyle(color = TextColor.Error)
            ),
            textAlign = TextAlign.Center,
            color = TextColor.Primary
        )
    }
}

/**
 * Clickable preview, you can test this preview in Interactive Mode
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedClickableTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var counter by remember { mutableStateOf(1) }
        MegaSpannedClickableText(
            value = "Click [A]here[/A] to increase the counter: [B]$counter[/B]\n and [R]here[/R] to reset",
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        color = TextColor.Accent,
                    ), "url or whatever you want to receive in onAnnotationClick"
                ),
                SpanIndicator('R') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        color = TextColor.Info,
                    ), "reset"
                ),
                SpanIndicator('B') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        color = TextColor.Error,
                    ), "d"
                )
            ),
            onAnnotationClick = { annotation ->
                if (annotation == "reset") {
                    counter = 1
                } else {
                    counter += 1
                }
            },
            baseStyle = MaterialTheme.typography.subtitle1,
            color = TextColor.Primary
        )
    }
}