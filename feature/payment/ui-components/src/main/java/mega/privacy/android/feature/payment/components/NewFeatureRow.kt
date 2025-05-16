package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Composable UI for new ui revamped feature description
 */
@Composable
internal fun NewFeatureRow(
    painter: Painter,
    title: String,
    description: String,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            painter = painter,
            contentDescription = "",
            tint = IconColor.Brand,
            modifier = Modifier
                .testTag("$testTag:icon")
        )
        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MegaText(
                text = title,
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .testTag("$testTag:title")
            )
            MegaText(
                text = description,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .testTag("$testTag:description")
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FeatureRowPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewFeatureRow(
            painter = painterResource(id = IconPackR.drawable.ic_cloud),
            title = "Additional security when sharing",
            description = "Set passwords and expiry dates for file and folder links.",
            testTag = "testTag:feature-row-preview",
        )
    }
}