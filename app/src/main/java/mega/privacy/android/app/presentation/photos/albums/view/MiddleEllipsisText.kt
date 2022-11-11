package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit

/**
 * Jetpack compose hasn't officially support middle truncation for Text, so workaround is required.
 *
 * This widget API is copied from https://github.com/mataku/MiddleEllipsisText
 * and intended for internal use only for photos domain.
 */
@Composable
internal fun MiddleEllipsisText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    ellipsisChar: Char = '.',
    ellipsisCharCount: Int = 3,
) {

    if (text.isBlank()) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    } else {
        var textLayoutResult: TextLayoutResult? = null
        val ellipsisText = ellipsisChar.toString().repeat(ellipsisCharCount)

        SubcomposeLayout(modifier) { constraints ->
            subcompose("MiddleEllipsisText_calculate") {
                Text(
                    text = text + ellipsisChar,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    softWrap = softWrap,
                    onTextLayout = { textLayoutResult = it },
                    style = style
                )
            }[0].measure(Constraints())

            textLayoutResult ?: return@SubcomposeLayout layout(0, 0) {}

            val placeable = subcompose("MiddleEllipsisText_apply") {
                val combinedText = remember(text, ellipsisText, textLayoutResult) {
                    if (textLayoutResult!!.getBoundingBox(text.lastIndex).right <= constraints.maxWidth) {
                        text
                    } else {
                        val textWidth = textLayoutResult!!.getBoundingBox(text.lastIndex + 1).width
                        val ellipsisTextWidth: Float = textWidth * ellipsisCharCount
                        val remainingWidth = constraints.maxWidth - ellipsisTextWidth
                        val textFromStart = mutableListOf<Char>()
                        val textFromEnd = mutableListOf<Char>()
                        var leftPoint = 0
                        var rightPoint = 0
                        var leftTextWidth = 0F
                        var rightTextWidth = 0F

                        kotlin.run {
                            repeat(text.lastIndex) {
                                val leftPosition = leftPoint
                                val rightPosition = text.lastIndex - rightPoint
                                val leftTextBoundingBox =
                                    textLayoutResult!!.getBoundingBox(leftPosition)
                                val rightTextBoundingBox =
                                    textLayoutResult!!.getBoundingBox(rightPosition)

                                // For multibyte string handling
                                if (leftTextWidth <= rightTextWidth && leftTextWidth + leftTextBoundingBox.width + rightTextWidth <= remainingWidth) {
                                    val leftChar = text[leftPosition]
                                    textFromStart.add(leftChar)
                                    leftTextWidth += leftTextBoundingBox.width
                                    leftPoint += 1
                                } else if (leftTextWidth >= rightTextWidth && leftTextWidth + rightTextWidth + rightTextBoundingBox.width <= remainingWidth) {
                                    val rightChar = text[rightPosition]
                                    textFromEnd.add(rightChar)
                                    rightTextWidth += rightTextBoundingBox.width
                                    rightPoint += 1
                                } else {
                                    return@run
                                }
                            }
                        }
                        textFromStart.joinToString(separator = "") + ellipsisText + textFromEnd.reversed()
                            .joinToString(
                                separator = ""
                            )
                    }
                }
                Text(
                    text = combinedText,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    softWrap = softWrap,
                    maxLines = 1,
                    onTextLayout = onTextLayout,
                    style = style
                )
            }[0].measure(constraints)

            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}
