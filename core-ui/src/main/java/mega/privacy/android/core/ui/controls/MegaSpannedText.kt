package mega.privacy.android.core.ui.controls

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.core.ui.model.SpanIndicator

/**
 * High light text from mega format
 * [A]Billed monthly[/A] %s/month
 * [A]Google Pay[/A] (subscription)
 *
 * @param modifier
 * @param value
 * @param baseStyle the style apply for all text
 * @param styles the list of the tag and the custom style
 * first is open tag, second is close tag and third is custom style
 */
@Composable
fun MegaSpannedText(
    modifier: Modifier = Modifier,
    value: String,
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, SpanStyle>,
) {
    var temp = value
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            styles.toSortedMap(compareBy { value.indexOf(it.openTag) }).forEach { item ->
                val start = temp.indexOf(string = item.key.openTag)
                val end = temp.indexOf(string = item.key.closeTag, startIndex = start)
                if (start > 0) {
                    append(temp.substring(0, start))
                }
                if (start >= 0 && (start + item.key.openTag.length < end)) {
                    withStyle(item.value) {
                        append(temp.substring(start + item.key.openTag.length, end))
                    }
                    val index = end + item.key.closeTag.length
                    if (index < temp.length) {
                        temp = temp.substring(index)
                    }
                }
            }
            if (temp.isNotEmpty()) {
                append(temp)
            }
        },
        fontSize = baseStyle.fontSize,
        fontFamily = baseStyle.fontFamily,
        fontWeight = baseStyle.fontWeight,
        letterSpacing = baseStyle.letterSpacing,
        textAlign = baseStyle.textAlign,
        color = baseStyle.color,
        textDecoration = baseStyle.textDecoration,
    )
}

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