package mega.privacy.android.legacy.core.ui.controls.text

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import java.text.BreakIterator

/**
 * A [Text] composable that shows the middle part of the text if the text is too long to fit in the
 * given constraints.
 *
 * Note: Middle ellipsis text is not yet supported in Compose. This is a temporary solution until
 * the official support is added. see this issue for more details: https://issuetracker.google.com/issues/185418980
 *
 **/
@Composable
@Deprecated(
    message = "This has been deprecated in favour of MiddleEllipsisText core component in core-ui module that uses TextColor to ensure usage of design system color tokens",
    replaceWith = ReplaceWith("mega.privacy.android.core.ui.controls.text.MiddleEllipsisText")
)
fun MiddleEllipsisText(
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
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    ellipsisChar: Char = '.',
    ellipsisCharCount: Int = 3,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    if (text.isEmpty()) {
        Text(
            text = text,
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
            onTextLayout = onTextLayout,
            style = style,
            overflow = overflow
        )
    } else {
        val layoutText = remember(text) { text }
        val textLayoutResultState = remember(layoutText) {
            mutableStateOf<TextLayoutResult?>(null)
        }
        val ellipsisText = ellipsisChar.toString().repeat(ellipsisCharCount)

        val breakIterator = BreakIterator.getCharacterInstance()
        breakIterator.setText(text)
        val charSplitIndexList = mutableListOf<Int>()
        while (breakIterator.next() != BreakIterator.DONE) {
            val index = breakIterator.current()
            charSplitIndexList.add(index)
        }
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
                    onTextLayout = { textLayoutResultState.value = it },
                    style = style
                )
            }[0].measure(Constraints())
            textLayoutResultState.value?.let { value ->
                val placeable = subcompose("MiddleEllipsisText_apply") {
                    val combinedText = remember(text, ellipsisText, textLayoutResultState) {
                        if (value.getBoundingBox(text.lastIndex).right <= constraints.maxWidth) {
                            text
                        } else {
                            val ellipsisCharWidth =
                                value.getBoundingBox(text.lastIndex + 1).width
                            val ellipsisTextWidth: Float = ellipsisCharWidth * ellipsisCharCount
                            val remainingWidth = constraints.maxWidth - ellipsisTextWidth
                            var leftPoint = 0
                            var rightPoint = text.lastIndex
                            var leftTextWidth = 0F
                            var rightTextWidth = 0F
                            var realLeftIndex = 0
                            var realRightIndex = charSplitIndexList.lastIndex

                            val textFromStart = mutableListOf<Char>()
                            val textFromEnd = mutableListOf<Char>()

                            run {
                                repeat(charSplitIndexList.size) {
                                    if (leftPoint >= rightPoint) {
                                        return@run
                                    }

                                    val leftTextBoundingBox =
                                        value.getBoundingBox(leftPoint)
                                    val rightTextBoundingBox =
                                        value.getBoundingBox(rightPoint)

                                    // For multibyte string handling
                                    if (leftTextWidth <= rightTextWidth &&
                                        leftTextWidth + leftTextBoundingBox.width + rightTextWidth <= remainingWidth
                                    ) {
                                        val remainingTargetCodePoints = if (realLeftIndex == 0) {
                                            charSplitIndexList[realLeftIndex]
                                        } else {
                                            charSplitIndexList[realLeftIndex] - charSplitIndexList[realLeftIndex - 1]
                                        }
                                        val targetText = mutableListOf<Char>()
                                        // multiple code points handling (e.g. flag emoji)
                                        repeat(remainingTargetCodePoints) {
                                            runCatching {
                                                targetText.add(text[leftPoint])
                                                val leftTextBoundingBoxWidth =
                                                    value.getBoundingBox(
                                                        leftPoint
                                                    ).width
                                                leftTextWidth += leftTextBoundingBoxWidth
                                                leftPoint += 1
                                            }.onFailure {
                                                return@run
                                            }
                                        }
                                        if (leftTextWidth + rightTextWidth <= remainingWidth) {
                                            textFromStart.addAll(targetText)
                                            realLeftIndex += 1
                                        }
                                    } else if (leftTextWidth >= rightTextWidth &&
                                        leftTextWidth + rightTextWidth + rightTextBoundingBox.width <= remainingWidth
                                    ) {
                                        val remainingTargetCodePoints =
                                            charSplitIndexList[realRightIndex] - charSplitIndexList[realRightIndex - 1]
                                        val targetText = mutableListOf<Char>()
                                        // multiple code points handling (e.g. flag emoji)
                                        repeat(remainingTargetCodePoints) {
                                            runCatching {
                                                targetText.add(0, text[rightPoint])
                                                val rightTextBoundingBoxWidth =
                                                    value.getBoundingBox(
                                                        rightPoint
                                                    ).width
                                                rightTextWidth += rightTextBoundingBoxWidth
                                                rightPoint -= 1
                                            }.onFailure {
                                                return@run
                                            }
                                        }
                                        if (leftTextWidth + rightTextWidth <= remainingWidth) {
                                            textFromEnd.addAll(0, targetText)
                                            realRightIndex -= 1
                                        }
                                    } else {
                                        return@run
                                    }
                                }
                            }

                            textFromStart.joinToString(separator = "") + ellipsisText + textFromEnd
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
                        maxLines = maxLines,
                        onTextLayout = onTextLayout,
                        style = style
                    )
                }[0].measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            } ?: return@SubcomposeLayout layout(0, 0) {}
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMiddleEllipsisText(
    @PreviewParameter(MiddleEllipsisTextProvider::class) text: String,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MiddleEllipsisText(
            text = text,
            color = Color.Green
        )
    }
}

/**
 * MiddleEllipsisText parameter provider for compose previews.
 */
private class MiddleEllipsisTextProvider : PreviewParameterProvider<String> {
    override val values = listOf(
        "SoooooooooooooooooooooooLoooooooooooooooooooongText",
        "\uD83D\uDE00".repeat(20),
        "Normal Text"
    ).asSequence()
}
