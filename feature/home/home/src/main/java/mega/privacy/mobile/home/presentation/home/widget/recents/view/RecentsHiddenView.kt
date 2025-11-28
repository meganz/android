package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as IconPackR

// TODO: Add all strings to resources/transifex once confirmed
@Composable
internal fun RecentsHiddenView(
    modifier: Modifier = Modifier,
    onShowActivityClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = modifier
                .weight(1f),
        ) {
            MegaText(
                text = "Recents activity hidden",
                style = AppTheme.typography.titleSmall,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(RECENTS_HIDDEN_TEXT_TEST_TAG)
            )
            TextButton(
                onClick = onShowActivityClicked,
                modifier = Modifier.testTag(RECENTS_HIDDEN_BUTTON_TEST_TAG),
                contentPadding = PaddingValues(
                    horizontal = 0.dp,
                    vertical = 12.dp
                )
            ) {
                MegaText(
                    text = "Show activity",
                    style = AppTheme.typography.labelLarge.copy(
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_recents),
            modifier = Modifier
                .size(60.dp)
        )
    }
}

internal const val RECENTS_HIDDEN_TEXT_TEST_TAG = "recents_widget:hidden_text"
internal const val RECENTS_HIDDEN_BUTTON_TEST_TAG = "recents_widget:hidden_button"

@CombinedThemePreviews
@Composable
private fun RecentsHiddenViewPreview() {
    AndroidThemeForPreviews {
        RecentsHiddenView(
            onShowActivityClicked = {}
        )
    }
}

