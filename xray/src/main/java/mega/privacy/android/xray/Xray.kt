package mega.privacy.android.xray

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private class Incrementer(
    var value: Long = 0L,
)

/**
 * Monitor recomposition counts for the target compose view.
 *
 * Usage of this API is to ease developer monitors recomposition event right from the UI.
 * The modifier will decorate a colorful layer above compose view with number of recompositions occurred during its lifecycle.
 *
 * Please note:
 * 1. If the compose is disposed from composition tree, the recorded recomposition count will be reset.
 * 2. Although 1st composition is not really considered as "recomposition", the layer will be still drawn to let developer aware that their compose view has been drawn for the 1st time.
 *
 * @param composableName the composable name label to be drawn so developer is easier to relate (optional).
 */
@OptIn(ExperimentalTextApi::class)
@Stable
fun Modifier.xray(
    composableName: String = "",
): Modifier = this.then(
    Modifier.composed(
        inspectorInfo = debugInspectorInfo {
            name = "xray"
            value = composableName
        },
        factory = {
            val textMeasurer = rememberTextMeasurer()

            val incrementer = remember { Incrementer() }
            incrementer.value++

            val numCompositions = incrementer.value

            var shouldHighlight by remember { mutableStateOf(false) }
            shouldHighlight = true

            LaunchedEffect(numCompositions) {
                delay(2000)
                shouldHighlight = false
            }

            Modifier.drawWithCache {
                onDrawWithContent {
                    drawContent()

                    if (shouldHighlight) {
                        drawHighlight(
                            composableName = composableName,
                            numCompositions = numCompositions,
                            textMeasurer = textMeasurer,
                        )
                    }
                }
            }
        },
    )
)

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawHighlight(
    composableName: String,
    numCompositions: Long,
    textMeasurer: TextMeasurer,
) {
    val factor = numCompositions / 20
    val (fromColor, toColor) = when (numCompositions) {
        in 0..19 -> Color.Magenta to Color.Blue
        in 20..39 -> Color.Blue to Color.Cyan
        in 40..59 -> Color.Cyan to Color.Green
        in 60..79 -> Color.Green to Color.Yellow
        in 80..99 -> Color.Yellow to Color.Red
        else -> Color.Red to Color.Red
    }
    val color = lerp(
        fromColor.copy(alpha = 0.5f),
        toColor.copy(alpha = 0.5f),
        minOf(1f, (numCompositions - factor * 20).toFloat() / 20f)
    )
    drawRect(
        color = color,
        style = Fill,
    )

    val measuredText = textMeasurer.measure(
        text = AnnotatedString(
            text = buildString {
                if (composableName.isNotBlank()) {
                    val normalizedComposableName = composableName.trim()
                    append("$normalizedComposableName: ")
                }
                append("+99".takeIf { numCompositions > 99 } ?: "$numCompositions")
            },
        ),
        style = TextStyle(
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    drawText(textLayoutResult = measuredText)
}
