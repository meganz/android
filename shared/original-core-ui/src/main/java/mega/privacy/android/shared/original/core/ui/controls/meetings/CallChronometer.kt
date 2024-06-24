package mega.privacy.android.shared.original.core.ui.controls.meetings

import android.graphics.Typeface
import android.os.SystemClock
import android.widget.Chronometer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Call chronometer
 *
 * @param duration      [Duration]
 * @param textStyle     [TextStyle]
 */

@Composable
fun CallChronometer(
    modifier: Modifier = Modifier,
    duration: Duration,
    textStyle: TextStyle,
) {
    val resolver: FontFamily.Resolver = LocalFontFamilyResolver.current
    val typeface: Typeface = remember(resolver, textStyle) {
        resolver.resolve(
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
            fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
            fontSynthesis = textStyle.fontSynthesis ?: FontSynthesis.All,
        )
    }.value as Typeface

    AndroidView(
        factory = { context ->
            Chronometer(context).apply {
                base = SystemClock.elapsedRealtime() - duration.inWholeMilliseconds
                start()
                format = " %s"
                this.typeface = typeface
                setTextColor(textStyle.color.toArgb())
            }
        },
        modifier = modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun CallChronometerPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CallChronometer(
            duration = Instant.now().epochSecond.toDuration(DurationUnit.SECONDS),
            textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary)
        )
    }
}