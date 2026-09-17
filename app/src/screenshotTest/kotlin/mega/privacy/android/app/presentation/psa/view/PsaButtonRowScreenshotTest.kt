package mega.privacy.android.app.presentation.psa.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton

/**
 * Baselines for the PSA button rows of both view variants, in the two states whose
 * arrangement differs (positive button plus dismiss, and dismiss only), at the default
 * font scale and at the largest system font scale on a 21:9 device.
 *
 * The large font goldens are the regression guard for the reported truncation: both
 * labels must stay fully legible instead of the dismiss label being squeezed away.
 */
class PsaButtonRowScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PsaButtonRowWithPositiveButton() {
        AndroidThemeForPreviews {
            ButtonRow(
                positiveButton = { PositiveTextMegaButton() },
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PsaButtonRowDismissOnly() {
        AndroidThemeForPreviews {
            ButtonRow(
                positiveButton = null,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @LargestFontTallDevicePreviews
    @Composable
    fun PsaButtonRowWithPositiveButtonLargestFont() {
        AndroidThemeForPreviews {
            ButtonRow(
                positiveButton = { PositiveTextMegaButton() },
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @LargestFontTallDevicePreviews
    @Composable
    fun PsaButtonRowDismissOnlyLargestFont() {
        AndroidThemeForPreviews {
            ButtonRow(
                positiveButton = null,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MegaPsaButtonRowWithPositiveButton() {
        AndroidThemeForPreviews {
            MegaButtonRow(
                positiveButton = { PositiveTextOnlyButton() },
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MegaPsaButtonRowDismissOnly() {
        AndroidThemeForPreviews {
            MegaButtonRow(
                positiveButton = null,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @LargestFontTallDevicePreviews
    @Composable
    fun MegaPsaButtonRowWithPositiveButtonLargestFont() {
        AndroidThemeForPreviews {
            MegaButtonRow(
                positiveButton = { PositiveTextOnlyButton() },
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @LargestFontTallDevicePreviews
    @Composable
    fun MegaPsaButtonRowDismissOnlyLargestFont() {
        AndroidThemeForPreviews {
            MegaButtonRow(
                positiveButton = null,
                onDismiss = {},
            )
        }
    }

    @Composable
    private fun PositiveTextMegaButton() {
        TextMegaButton(
            text = POSITIVE_BUTTON_TEXT,
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @Composable
    private fun PositiveTextOnlyButton() {
        TextOnlyButton(
            text = POSITIVE_BUTTON_TEXT,
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private const val POSITIVE_BUTTON_TEXT = "Positive Button Text"

@Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    apiLevel = 33,
    fontScale = 1.5f,
    device = "spec:width=360dp,height=840dp",
)
@Preview(
    showBackground = true,
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    apiLevel = 33,
    fontScale = 1.5f,
    device = "spec:width=360dp,height=840dp",
)
private annotation class LargestFontTallDevicePreviews
