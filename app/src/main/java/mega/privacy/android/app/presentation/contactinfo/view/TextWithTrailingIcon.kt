package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 *Class helps to create a text view with icon at the end of text view
 */
@Composable
fun TextWithTrailingIcon(
    text: String,
    imageResource: Int,
    modifier: Modifier = Modifier,
    iconAlpha: Float = 1f,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    iconLine: Int = 0,
    overflow: TextOverflow = TextOverflow.Visible,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val painter = painterResource(id = imageResource)
    var lineTop = 0f
    var lineBottom = 0f
    var lineRight = 0f
    var lines: Int
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        style = textStyle,
        onTextLayout = { layoutResult ->
            lines = layoutResult.lineCount
            if (lines > iconLine) {
                val lineIndex = (lines - 1).coerceAtLeast(0)
                lineTop = layoutResult.getLineTop(lineIndex)
                lineBottom = layoutResult.getLineBottom(lineIndex)
                lineRight = layoutResult.getLineRight(lineIndex)
            }
        },
        modifier = modifier
            .testTag(imageResource.toString())
            .drawBehind {
                if (text.isEmpty()) return@drawBehind
                with(painter) {
                    translate(
                        left = lineRight + 8,
                        top = lineTop + (lineBottom - lineTop + 8) / 2 - painter.intrinsicSize.height / 2,
                    ) {
                        draw(painter.intrinsicSize, alpha = iconAlpha)
                    }
                }
            }
    )
}