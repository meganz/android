package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun ConflictDetailsDialog(
    conflictName: String,
    explanation: String,
) {
    Column {
        MegaText(
            text = conflictName,
            textColor = TextColor.Primary,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            style = MaterialTheme.typography.subtitle1,
        )
        MegaText(
            text = explanation,
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            style = MaterialTheme.typography.subtitle2
        )
    }
}

@Composable
@CombinedThemePreviews
internal fun PreviewConflictDetailsDialog() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ConflictDetailsDialog(
            conflictName = "Conflict A",
            explanation = "This folders contain multiple names on one side, that would all become the" +
                    " same single name on the other side. This may be due to syncing to case sensitive " +
                    "local filesystem, or the effects os escaped characters."
        )
    }
}
