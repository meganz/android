package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun ConflictDetailsDialog(
    conflictName: String,
    explanation: String,
) {
    Column {
        Text(
            conflictName,
            Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary)
        )
        Text(
            explanation,
            Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary)
        )
    }
}

@Composable
@CombinedThemePreviews
internal fun PreviewConflictDetailsDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ConflictDetailsDialog(
            conflictName = "Conflict A",
            explanation = "This folders contain multiple names on one side, that would all become the" +
                    " same single name on the other side. This may be due to syncing to case sensitive " +
                    "local filesystem, or the effects os escaped characters."
        )
    }
}
