package mega.privacy.android.app.upgradeAccount.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

/**
 * Composable UI for feature description to reuse on Onboarding dialog for both Variants (A and B)
 */
@Composable
fun FeatureRow(
    drawableID: Painter,
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
        Icon(
            painter = drawableID,
            contentDescription = "",
            tint = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier.testTag("$testTag:icon")
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2medium,
                color = MaterialTheme.colors.textColorPrimary,
                modifier = Modifier.testTag("$testTag:title")
            )
            Text(
                text = description,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.textColorPrimary,
                modifier = Modifier.testTag("$testTag:description")
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DescriptionRowPreviewDark")
@Composable
fun PreviewDescriptionRow() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_security_onboarding_dialog),
            title = "Additional security when sharing",
            description = "Set passwords and expiry dates for file and folder links.",
            testTag = STORAGE_DESCRIPTION_ROW,
        )
    }
}