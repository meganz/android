package mega.privacy.android.core.ui.controls

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.model.SpanStyleWithAnnotation

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
 */
@Composable
fun MegaSpannedText(
    value: String,
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, SpanStyle>,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = spannedText(value, styles),
        style = baseStyle
    )
}

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
 * @param textAlign to align text
 */
@Composable
fun MegaSpannedAlignedText(
    value: String,
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, SpanStyle>,
    modifier: Modifier = Modifier,
    textAlign: TextAlign,
) {
    Text(
        modifier = modifier,
        text = spannedText(value, styles),
        style = baseStyle,
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
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, SpanStyleWithAnnotation>,
    onAnnotationClick: (annotation: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedLinkString = spannedTextWithAnnotation(value, styles)
    ClickableText(
        modifier = modifier,
        text = annotatedLinkString,
        style = baseStyle,
        onClick = { position ->
            annotatedLinkString.getStringAnnotations(ANNOTATION_TAG, position, position + 1)
                .firstOrNull()?.let { onAnnotationClick(it.item) }
        }
    )
}

private fun spannedText(value: String, styles: Map<SpanIndicator, SpanStyle>) =
    spannedTextWithAnnotation(value, styles.mapValues {
        SpanStyleWithAnnotation(it.value, null)
    })

@OptIn(ExperimentalTextApi::class)
private fun spannedTextWithAnnotation(
    value: String,
    styles: Map<SpanIndicator, SpanStyleWithAnnotation>,
) =
    buildAnnotatedString {
        var temp = value
        styles.toSortedMap(compareBy { value.indexOf(it.openTag) }).forEach { item ->
            val start = temp.indexOf(string = item.key.openTag)
            val end = temp.indexOf(string = item.key.closeTag, startIndex = start)
            if (start > 0) {
                append(temp.substring(0, start))
            }
            if (start >= 0 && (start + item.key.openTag.length < end)) {
                item.value.annotation?.let {
                    withAnnotation(ANNOTATION_TAG, it) {
                        withStyle(item.value.spanStyle) {
                            append(temp.substring(start + item.key.openTag.length, end))
                        }
                    }
                } ?: run {
                    withStyle(item.value.spanStyle) {
                        append(temp.substring(start + item.key.openTag.length, end))
                    }
                }
                val index = end + item.key.closeTag.length
                if (index < temp.length + 1) {
                    temp = temp.substring(index)
                }
            }
        }
        if (temp.isNotEmpty()) {
            append(temp)
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
    MegaSpannedText(
        value = "[A]Google Pay[/A] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = hashMapOf(
            SpanIndicator('A') to SpanStyle(color = Color.Red)
        )
    )
}

/**
 * High light text preview two
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewTwo() {
    MegaSpannedText(
        value = "Do you want to [A]Google Pay[/A] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = hashMapOf(
            SpanIndicator('A') to SpanStyle(color = Color.Red)
        )
    )
}

/**
 * High light text preview three
 *
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedTextPreviewThree() {
    MegaSpannedText(
        value = "Do you want to [A]Google Pay[/A] [B]Huawei[/B] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = hashMapOf(
            SpanIndicator('B') to SpanStyle(color = Color.Green),
            SpanIndicator('A') to SpanStyle(color = Color.Red)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MegaSpannedAlignedTextPreview() {
    MegaSpannedAlignedText(
        value = "Do you want to [A]Google Pay[/A] [B]Huawei[/B]\n (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = hashMapOf(
            SpanIndicator('B') to SpanStyle(color = Color.Green),
            SpanIndicator('A') to SpanStyle(color = Color.Red)
        ),
        textAlign = TextAlign.Center
    )
}

/**
 * Clickable preview, you can test this preview in Interactive Mode
 */
@Preview(showBackground = true)
@Composable
fun MegaSpannedClickableTextPreview() {
    var counter by remember { mutableStateOf(1) }
    MegaSpannedClickableText(
        value = "Click [A]here[/A] to increase the counter: [B]$counter[/B]\n and [R]here[/R] to reset",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = hashMapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline,
                ), "url or whatever you want to receive in onAnnotationClick"
            ),
            SpanIndicator('R') to SpanStyleWithAnnotation(
                SpanStyle(
                    color = Color.Green,
                    textDecoration = TextDecoration.Underline,
                ), "reset"
            ),
            SpanIndicator('B') to SpanStyleWithAnnotation(SpanStyle(color = Color.Red), "d")
        ),
        onAnnotationClick = { annotation ->
            if (annotation == "reset") {
                counter = 1
            } else {
                counter += 1
            }
        }
    )
}