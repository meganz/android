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
fun MegaHighLightText(
    modifier: Modifier = Modifier,
    value: String,
    baseStyle: TextStyle,
    styles: List<Triple<String, String, SpanStyle>>,
) {
    var temp = value
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            styles.forEach { item ->
                val start = temp.indexOf(string = item.first)
                val end = temp.indexOf(string = item.second, startIndex = start)
                if (start > 0) {
                    append(temp.substring(0, start))
                }
                if (start >= 0 && (start + item.first.length < end)) {
                    withStyle(item.third) {
                        append(temp.substring(start + item.first.length, end))
                    }
                    val index = end + item.second.length
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
fun HighLightTextPreviewOne() {
    MegaHighLightText(
        value = "[A]Google Pay[/A] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = listOf(
            Triple("[A]", "[/A]", SpanStyle(color = Color.Red))
        )
    )
}

/**
 * High light text preview two
 *
 */
@Preview(showBackground = true)
@Composable
fun HighLightTextPreviewTwo() {
    MegaHighLightText(
        value = "Do you want to [A]Google Pay[/A] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = listOf(
            Triple("[A]", "[/A]", SpanStyle(color = Color.Red))
        )
    )
}

/**
 * High light text preview three
 *
 */
@Preview(showBackground = true)
@Composable
fun HighLightTextPreviewThree() {
    MegaHighLightText(
        value = "Do you want to [A]Google Pay[/A] [B]Huawei[/B] (subscription)",
        baseStyle = MaterialTheme.typography.subtitle1,
        styles = listOf(
            Triple("[A]", "[/A]", SpanStyle(color = Color.Red)),
            Triple("[B]", "[/B]", SpanStyle(color = Color.Green)),
        )
    )
}