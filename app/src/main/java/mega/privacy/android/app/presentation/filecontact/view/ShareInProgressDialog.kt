package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.MegaDialog
import mega.android.core.ui.components.dialogs.MegaDialogBackground
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R

@Composable
internal fun ShareInProgressDialog(
    modifier: Modifier = Modifier,
) {
    MegaDialog(
        dialogBackground = MegaDialogBackground.Surface1,
        onDismissRequest = { },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.Companion.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
            LargeInfiniteSpinnerIndicator()
            MegaText(
                text = stringResource(R.string.context_sharing_folder),
                textColor = TextColor.Primary,
                modifier = Modifier.Companion.padding(start = 16.dp),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ShareInProgressDialogPreview() {
    AndroidThemeForPreviews { ShareInProgressDialog() }
}