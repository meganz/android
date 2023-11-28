package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

/**
 * Composable UI for feature description to reuse on Onboarding dialog for both Variants (A and B)
 */
@Composable
internal fun FeatureRow(
    drawableID: Painter,
    title: String,
    description: String,
    testTag: String,
    isLoading: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = drawableID,
            contentDescription = "",
            tint = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier
                .testTag("$testTag:icon")
                .placeholder(
                    color = MaterialTheme.colors.grey_020_grey_900,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                    visible = isLoading,
                )
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2medium,
                color = MaterialTheme.colors.textColorPrimary,
                modifier = Modifier
                    .testTag("$testTag:title")
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = isLoading,
                    )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.textColorPrimary,
                modifier = Modifier
                    .testTag("$testTag:description")
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = isLoading,
                    )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@CombinedThemePreviews
@Composable
fun FeatureRowPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_security_onboarding_dialog),
            title = "Additional security when sharing",
            description = "Set passwords and expiry dates for file and folder links.",
            testTag = STORAGE_DESCRIPTION_ROW,
        )
    }
}